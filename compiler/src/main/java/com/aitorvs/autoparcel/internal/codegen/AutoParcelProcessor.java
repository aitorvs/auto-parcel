package com.aitorvs.autoparcel.internal.codegen;

/*
 * Copyright (C) 13/07/16 aitorvs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.aitorvs.autoparcel.AutoParcel;
import com.aitorvs.autoparcel.ParcelAdapter;
import com.aitorvs.autoparcel.ParcelVersion;
import com.aitorvs.autoparcel.internal.common.MoreElements;
import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

@SupportedAnnotationTypes("com.aitorvs.autoparcel.AutoParcel")
public final class AutoParcelProcessor extends AbstractProcessor {
    private ErrorReporter mErrorReporter;
    private Types mTypeUtils;


    static final class Property {
        final String fieldName;
        final VariableElement element;
        final TypeName typeName;
        final ImmutableSet<String> annotations;
        final int version;
        TypeMirror typeAdapter;

        Property(String fieldName, VariableElement element) {
            this.fieldName = fieldName;
            this.element = element;
            this.typeName = TypeName.get(element.asType());
            this.annotations = getAnnotations(element);

            // get the parcel adapter if any
            ParcelAdapter parcelAdapter = element.getAnnotation(ParcelAdapter.class);
            if (parcelAdapter != null) {
                try {
                    parcelAdapter.value();
                } catch (MirroredTypeException e) {
                    this.typeAdapter = e.getTypeMirror();
                }
            }

            // get the element version, default 0
            ParcelVersion parcelVersion = element.getAnnotation(ParcelVersion.class);
            this.version = parcelVersion == null ? 0 : parcelVersion.from();
        }

        public boolean isNullable() {
            return this.annotations.contains("Nullable");
        }

        public int version() {
            return this.version;
        }

        private ImmutableSet<String> getAnnotations(VariableElement element) {
            ImmutableSet.Builder<String> builder = ImmutableSet.builder();
            for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
                builder.add(annotation.getAnnotationType().asElement().getSimpleName().toString());
            }

            return builder.build();
        }
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mErrorReporter = new ErrorReporter(processingEnv);
        mTypeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Collection<? extends Element> annotatedElements =
                env.getElementsAnnotatedWith(AutoParcel.class);
        List<TypeElement> types = new ImmutableList.Builder<TypeElement>()
                .addAll(ElementFilter.typesIn(annotatedElements))
                .build();

        for (TypeElement type : types) {
            processType(type);
        }

        // We are the only ones handling AutoParcel annotations
        return true;
    }

    private void processType(TypeElement type) {
        AutoParcel autoParcel = type.getAnnotation(AutoParcel.class);
        if (autoParcel == null) {
            mErrorReporter.abortWithError("annotation processor for @AutoParcel was invoked with a" +
                    "type annotated differently; compiler bug? O_o", type);
        }
        if (type.getKind() != ElementKind.CLASS) {
            mErrorReporter.abortWithError("@" + AutoParcel.class.getName() + " only applies to classes", type);
        }
        if (ancestorIsAutoParcel(type)) {
            mErrorReporter.abortWithError("One @AutoParcel class shall not extend another", type);
        }

        checkModifiersIfNested(type);

        // get the fully-qualified class name
        String fqClassName = generatedSubclassName(type, 0);
        // class name
        String className = TypeUtil.simpleNameOf(fqClassName);
        String source = generateClass(type, className, type.getSimpleName().toString(), false);
        source = Reformatter.fixup(source);
        writeSourceFile(fqClassName, source, type);

    }

    private void writeSourceFile(String className, String text, TypeElement originatingType) {
        try {
            JavaFileObject sourceFile =
                    processingEnv.getFiler().createSourceFile(className, originatingType);
            Writer writer = sourceFile.openWriter();
            try {
                writer.write(text);
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            // This should really be an error, but we make it a warning in the hope of resisting Eclipse
            // bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=367599. If that bug manifests, we may get
            // invoked more than once for the same file, so ignoring the ability to overwrite it is the
            // right thing to do. If we are unable to write for some other reason, we should get a compile
            // error later because user code will have a reference to the code we were supposed to
            // generate (new AutoValue_Foo() or whatever) and that reference will be undefined.
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "Could not write generated class " + className + ": " + e);
        }
    }

    private String generateClass(TypeElement type, String className, String classToExtend, boolean isFinal) {
        if (type == null) {
            mErrorReporter.abortWithError("generateClass was invoked with null type", type);
        }
        if (className == null) {
            mErrorReporter.abortWithError("generateClass was invoked with null class name", type);
        }
        if (classToExtend == null) {
            mErrorReporter.abortWithError("generateClass was invoked with null parent class", type);
        }
        List<VariableElement> nonPrivateFields = getParcelableFieldsOrError(type);
        if (nonPrivateFields.isEmpty()) {
            mErrorReporter.abortWithError("generateClass error, all fields are declared PRIVATE", type);
        }

        // get the properties
        ImmutableList<Property> properties = buildProperties(nonPrivateFields);

        // get the type adapters
        ImmutableMap<TypeMirror, FieldSpec> typeAdapters = getTypeAdapters(properties);

        // get the parcel version
        //noinspection ConstantConditions
        int version = type.getAnnotation(AutoParcel.class).version();

        // Generate the AutoParcel_??? class
        String pkg = TypeUtil.packageNameOf(type);
        TypeName classTypeName = ClassName.get(pkg, className);
        TypeSpec.Builder subClass = TypeSpec.classBuilder(className)
                // Add the version
                .addField(TypeName.INT, "version", PRIVATE)
                // Class must be always final
                .addModifiers(FINAL)
                // extends from original abstract class
                .superclass(ClassName.get(pkg, classToExtend))
                // Add the DEFAULT constructor
                .addMethod(generateConstructor(properties))
                // Add the private constructor
                .addMethod(generateConstructorFromParcel(processingEnv, properties, typeAdapters))
                // overrides describeContents()
                .addMethod(generateDescribeContents())
                // static final CREATOR
                .addField(generateCreator(processingEnv, properties, classTypeName, typeAdapters))
                // overrides writeToParcel()
                .addMethod(generateWriteToParcel(version, processingEnv, properties, typeAdapters)); // generate writeToParcel()

        if (!ancestoIsParcelable(processingEnv, type)) {
            // Implement android.os.Parcelable if the ancestor does not do it.
            subClass.addSuperinterface(ClassName.get("android.os", "Parcelable"));
        }

        if (!typeAdapters.isEmpty()) {
            typeAdapters.values().forEach(subClass::addField);
        }


        JavaFile javaFile = JavaFile.builder(pkg, subClass.build()).build();
        return javaFile.toString();
    }

    private ImmutableMap<TypeMirror, FieldSpec> getTypeAdapters(ImmutableList<Property> properties) {
        Map<TypeMirror, FieldSpec> typeAdapters = new LinkedHashMap<>();
        NameAllocator nameAllocator = new NameAllocator();
        nameAllocator.newName("CREATOR");
        for (Property property : properties) {
            if (property.typeAdapter != null && !typeAdapters.containsKey(property.typeAdapter)) {
                ClassName typeName = (ClassName) TypeName.get(property.typeAdapter);
                String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, typeName.simpleName());
                name = nameAllocator.newName(name, typeName);

                typeAdapters.put(property.typeAdapter, FieldSpec.builder(
                        typeName, NameAllocator.toJavaIdentifier(name), PRIVATE, STATIC, FINAL)
                        .initializer("new $T()", typeName).build());
            }
        }
        return ImmutableMap.copyOf(typeAdapters);
    }

    private ImmutableList<Property> buildProperties(List<VariableElement> elements) {
        ImmutableList.Builder<Property> builder = ImmutableList.builder();
        for (VariableElement element : elements) {
            builder.add(new Property(element.getSimpleName().toString(), element));
        }

        return builder.build();
    }

    /**
     * This method returns a list of all non private fields. If any <code>private</code> fields is
     * found, the method errors out
     *
     * @param type element
     * @return list of all non-<code>private</code> fields
     */
    private List<VariableElement> getParcelableFieldsOrError(TypeElement type) {
        List<VariableElement> allFields = ElementFilter.fieldsIn(type.getEnclosedElements());
        List<VariableElement> nonPrivateFields = new ArrayList<>();

        for (VariableElement field : allFields) {
            if (!field.getModifiers().contains(PRIVATE)) {
                nonPrivateFields.add(field);
            } else {
                // return error, PRIVATE fields are not allowed
                mErrorReporter.abortWithError("getFieldsError error, PRIVATE fields not allowed", type);
            }
        }

        return nonPrivateFields;
    }

    private MethodSpec generateConstructor(ImmutableList<Property> properties) {

        List<ParameterSpec> params = Lists.newArrayListWithCapacity(properties.size());
        for (Property property : properties) {
            params.add(ParameterSpec.builder(property.typeName, property.fieldName).build());
        }

        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addParameters(params);

        for (ParameterSpec param : params) {
            builder.addStatement("this.$N = $N", param.name, param.name);
        }

        return builder.build();
    }

    private MethodSpec generateConstructorFromParcel(
            ProcessingEnvironment env,
            ImmutableList<Property> properties,
            ImmutableMap<TypeMirror, FieldSpec> typeAdapters) {

        // Create the PRIVATE constructor from Parcel
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(PRIVATE)      // private
                .addParameter(ClassName.bestGuess("android.os.Parcel"), "in"); // input param

        // get a code block builder
        CodeBlock.Builder block = CodeBlock.builder();

        // First thing is reading the Parcelable object version
        block.add("this.version = in.readInt();\n");

        // FIXME: 31/07/16 remove if not used
        boolean requiresSuppressWarnings = false;

        // Now, iterate all properties, check the version initialize them
        for (Property p : properties) {

            // get the property version
            int pVersion = p.version();
            if (pVersion > 0) {
                block.beginControlFlow("if (this.version >= $L)", pVersion);
            }

            block.add("this.$N = ", p.fieldName);

            if (p.typeAdapter != null && typeAdapters.containsKey(p.typeAdapter)) {
                Parcelables.readValueWithTypeAdapter(block, p, typeAdapters.get(p.typeAdapter));
            } else {
                requiresSuppressWarnings |= Parcelables.isTypeRequiresSuppressWarnings(p.typeName);
                TypeName parcelableType = Parcelables.getTypeNameFromProperty(p, env.getTypeUtils());
                Parcelables.readValue(block, p, parcelableType);
            }

            block.add(";\n");

            if (pVersion > 0) {
                block.endControlFlow();
            }
        }

        builder.addCode(block.build());

        return builder.build();
    }

    private String generatedSubclassName(TypeElement type, int depth) {
        return generatedClassName(type, Strings.repeat("$", depth) + "AutoParcel_");
    }

    private String generatedClassName(TypeElement type, String prefix) {
        String name = type.getSimpleName().toString();
        while (type.getEnclosingElement() instanceof TypeElement) {
            type = (TypeElement) type.getEnclosingElement();
            name = type.getSimpleName() + "_" + name;
        }
        String pkg = TypeUtil.packageNameOf(type);
        String dot = pkg.isEmpty() ? "" : ".";
        return pkg + dot + prefix + name;
    }

    private MethodSpec generateWriteToParcel(
            int version,
            ProcessingEnvironment env,
            ImmutableList<Property> properties,
            ImmutableMap<TypeMirror, FieldSpec> typeAdapters) {
        ParameterSpec dest = ParameterSpec
                .builder(ClassName.get("android.os", "Parcel"), "dest")
                .build();
        ParameterSpec flags = ParameterSpec.builder(int.class, "flags").build();
        MethodSpec.Builder builder = MethodSpec.methodBuilder("writeToParcel")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(dest)
                .addParameter(flags);

        // write first the parcelable object version...
        builder.addCode(Parcelables.writeVersion(version, dest));

        // ...then write all the properties
        for (Property p : properties) {
            if (p.typeAdapter != null && typeAdapters.containsKey(p.typeAdapter)) {
                FieldSpec typeAdapter = typeAdapters.get(p.typeAdapter);
                builder.addCode(Parcelables.writeValueWithTypeAdapter(typeAdapter, p, dest));
            } else {
                builder.addCode(Parcelables.writeValue(p, dest, flags, env.getTypeUtils()));
            }
        }

        return builder.build();
    }

    private MethodSpec generateDescribeContents() {
        return MethodSpec.methodBuilder("describeContents")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(int.class)
                .addStatement("return 0")
                .build();
    }

    private FieldSpec generateCreator(
            ProcessingEnvironment env,
            ImmutableList<Property> properties,
            TypeName type,
            ImmutableMap<TypeMirror, FieldSpec> typeAdapters) {
        ClassName creator = ClassName.bestGuess("android.os.Parcelable.Creator");
        TypeName creatorOfClass = ParameterizedTypeName.get(creator, type);

        Types typeUtils = env.getTypeUtils();
        CodeBlock.Builder ctorCall = CodeBlock.builder();
        boolean requiresSuppressWarnings = false;
        ctorCall.add("return new $T(in);\n", type);

        // Method createFromParcel()
        MethodSpec.Builder createFromParcel = MethodSpec.methodBuilder("createFromParcel")
                .addAnnotation(Override.class);
        if (requiresSuppressWarnings) {
            createFromParcel.addAnnotation(createSuppressUncheckedWarningAnnotation());
        }
        createFromParcel
                .addModifiers(PUBLIC)
                .returns(type)
                .addParameter(ClassName.bestGuess("android.os.Parcel"), "in");
        createFromParcel.addCode(ctorCall.build());

        TypeSpec creatorImpl = TypeSpec.anonymousClassBuilder("")
                .superclass(creatorOfClass)
                .addMethod(createFromParcel
                        .build())
                .addMethod(MethodSpec.methodBuilder("newArray")
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .returns(ArrayTypeName.of(type))
                        .addParameter(int.class, "size")
                        .addStatement("return new $T[size]", type)
                        .build())
                .build();

        return FieldSpec
                .builder(creatorOfClass, "CREATOR", PUBLIC, FINAL, STATIC)
                .initializer("$L", creatorImpl)
                .build();
    }

    private void checkModifiersIfNested(TypeElement type) {
        ElementKind enclosingKind = type.getEnclosingElement().getKind();
        if (enclosingKind.isClass() || enclosingKind.isInterface()) {
            if (type.getModifiers().contains(PRIVATE)) {
                mErrorReporter.abortWithError("@AutoParcel class must not be private", type);
            }
            if (!type.getModifiers().contains(STATIC)) {
                mErrorReporter.abortWithError("Nested @AutoParcel class must be static", type);
            }
        }
        // In principle type.getEnclosingElement() could be an ExecutableElement (for a class
        // declared inside a method), but since RoundEnvironment.getElementsAnnotatedWith doesn't
        // return such classes we won't see them here.
    }

    private boolean ancestorIsAutoParcel(TypeElement type) {
        while (true) {
            TypeMirror parentMirror = type.getSuperclass();
            if (parentMirror.getKind() == TypeKind.NONE) {
                return false;
            }
            TypeElement parentElement = (TypeElement) mTypeUtils.asElement(parentMirror);
            if (MoreElements.isAnnotationPresent(parentElement, AutoParcel.class)) {
                return true;
            }
            type = parentElement;
        }
    }

    private boolean ancestoIsParcelable(ProcessingEnvironment env, TypeElement type) {
        // TODO: 15/07/16 check recursively
        TypeMirror classType = type.asType();
        TypeMirror parcelable = env.getElementUtils().getTypeElement("android.os.Parcelable").asType();
        return TypeUtil.isClassOfType(env.getTypeUtils(), parcelable, classType);
    }

    private static AnnotationSpec createSuppressUncheckedWarningAnnotation() {
        return AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "\"unchecked\"")
                .build();
    }
}
