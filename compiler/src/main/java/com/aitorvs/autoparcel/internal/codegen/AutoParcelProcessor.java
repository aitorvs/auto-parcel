package com.aitorvs.autoparcel.internal.codegen;

import com.aitorvs.autoparcel.AutoParcel;
import com.aitorvs.autoparcel.internal.common.MoreElements;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
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
        if (ancestorIsAutoParcel(type) ) {
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
        mErrorReporter.reportNote(source, type);

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
        List<VariableElement> nonPrivateFields = getNonPrivateLocalFields(type);
        if (nonPrivateFields.isEmpty()) {
            mErrorReporter.abortWithError("generateClass was invoked with no non-private fields", type);
        }

        // Generate the AutoParcel_??? class
        String pkg = TypeUtil.packageNameOf(type);
        TypeName classTypeName = ClassName.get(pkg, className);
        TypeSpec.Builder subClass = TypeSpec.classBuilder(className)
                // Class must be always final
                .addModifiers(FINAL)
                // extends from original abstract class
                .superclass(ClassName.get(pkg, classToExtend))
                // implements Parcelable
                .addSuperinterface(ClassName.get("android.os", "Parcelable"))
                // Add the constructor
                .addMethod(generateConstructor(nonPrivateFields))
                // overrides describeContents()
                .addMethod(generateDescribeContents())
                // static final CREATOR
                .addField(generateCreator(processingEnv, nonPrivateFields, classTypeName))
                // overrides writeToParcel()
                .addMethod(generateWriteToParcel(processingEnv, nonPrivateFields)); // generate writeToParcel()


        JavaFile javaFile = JavaFile.builder(pkg, subClass.build()).build();
        return javaFile.toString();
    }

    private List<VariableElement> getNonPrivateLocalFields(TypeElement type) {
        List<VariableElement> allFields = ElementFilter.fieldsIn(type.getEnclosedElements());
        List<VariableElement> nonPrivateFields = new ArrayList<>();

        for (VariableElement field : allFields) {
            if (!field.getModifiers().contains(PRIVATE)) {
                nonPrivateFields.add(field);
            }
        }

        return nonPrivateFields;
    }

    MethodSpec generateConstructor(List<VariableElement> fields) {

        List<ParameterSpec> params = Lists.newArrayListWithCapacity(fields.size());
        for (VariableElement field : fields) {
            params.add(ParameterSpec.builder(TypeName.get(field.asType()), field.getSimpleName().toString()).build());
        }

        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addParameters(params);

        for (ParameterSpec param : params) {
            builder.addStatement("this.$N = $N;", param.name, param.name);
        }

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

    private MethodSpec generateWriteToParcel(ProcessingEnvironment env, List<VariableElement> fields) {
        ParameterSpec dest = ParameterSpec
                .builder(ClassName.get("android.os", "Parcel"), "dest")
                .build();
        ParameterSpec flags = ParameterSpec.builder(int.class, "flags").build();
        MethodSpec.Builder builder = MethodSpec.methodBuilder("writeToParcel")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(dest)
                .addParameter(flags);

        for (VariableElement field : fields) {
            builder.addCode(Parcelables.writeValue(field, dest, flags));
        }

        return builder.build();
    }

    MethodSpec generateDescribeContents() {
        return MethodSpec.methodBuilder("describeContents")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(int.class)
                .addStatement("return 0")
                .build();
    }

    FieldSpec generateCreator(ProcessingEnvironment env, List<VariableElement> fields, TypeName type) {
        ClassName creator = ClassName.bestGuess("android.os.Parcelable.Creator");
        TypeName creatorOfClass = ParameterizedTypeName.get(creator, type);

        Types typeUtils = env.getTypeUtils();
        CodeBlock.Builder ctorCall = CodeBlock.builder();
        ctorCall.add("return new $T(\n", type);
        ctorCall.indent().indent();
        boolean requiresSuppressWarnings = false;
        for (int i = 0, n = fields.size(); i < fields.size(); i++) {
            VariableElement field = fields.get(i);
            final TypeName typeName = TypeName.get(field.asType());
            requiresSuppressWarnings |= Parcelables.isTypeRequiresSuppressWarnings(typeName);
            Parcelables.readValue(ctorCall, field, typeName);

            if (i < n - 1) ctorCall.add(",");
            ctorCall.add("\n");
        }
        ctorCall.unindent().unindent();
        ctorCall.add(");\n");

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
        while(true) {
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

    private static AnnotationSpec createSuppressUncheckedWarningAnnotation() {
        return AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "\"unchecked\"")
                .build();
    }
}
