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

import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * This class implementation has been based and refactored from <code>Parcelables</code> auto-value
 * extension implementation.
 *
 * https://github.com/rharter/auto-value-parcel/blob/master/auto-value-parcel/src/main/java/com/ryanharter/auto/value/parcel/Parcelables.java
 */
class Parcelables {
    private static final TypeName STRING = ClassName.get("java.lang", "String");
    private static final TypeName MAP = ClassName.get("java.util", "Map");
    private static final TypeName LIST = ClassName.get("java.util", "List");
    private static final TypeName BOOLEANARRAY = ArrayTypeName.of(boolean.class);
    private static final TypeName BYTEARRAY = ArrayTypeName.of(byte.class);
    private static final TypeName CHARARRAY = ArrayTypeName.of(char.class);
    private static final TypeName INTARRAY = ArrayTypeName.of(int.class);
    private static final TypeName LONGARRAY = ArrayTypeName.of(long.class);
    private static final TypeName STRINGARRAY = ArrayTypeName.of(String.class);
    private static final TypeName SPARSEARRAY = ClassName.get("android.util", "SparseArray");
    private static final TypeName SPARSEBOOLEANARRAY = ClassName.get("android.util", "SparseBooleanArray");
    private static final TypeName BUNDLE = ClassName.get("android.os", "Bundle");
    private static final TypeName PARCELABLE = ClassName.get("android.os", "Parcelable");
    private static final TypeName PARCELABLEARRAY = ArrayTypeName.of(PARCELABLE);
    private static final TypeName CHARSEQUENCE = ClassName.get("java.lang", "CharSequence");
    private static final TypeName IBINDER = ClassName.get("android.os", "IBinder");
    private static final TypeName OBJECTARRAY = ArrayTypeName.of(TypeName.OBJECT);
    private static final TypeName SERIALIZABLE = ClassName.get("java.io", "Serializable");
    private static final TypeName PERSISTABLEBUNDLE = ClassName.get("android.os", "PersistableBundle");
    private static final TypeName SIZE = ClassName.get("android.util", "Size");
    private static final TypeName SIZEF = ClassName.get("android.util", "SizeF");
    private static final TypeName TEXTUTILS = ClassName.get("android.text", "TextUtils");
    private static final TypeName ENUM = ClassName.get(Enum.class);

    private static final Set<TypeName> VALID_TYPES = ImmutableSet.of(STRING, MAP, LIST, BOOLEANARRAY,
            BYTEARRAY, CHARARRAY, INTARRAY, LONGARRAY, STRINGARRAY, SPARSEARRAY, SPARSEBOOLEANARRAY,
            BUNDLE, PARCELABLE, PARCELABLEARRAY, CHARSEQUENCE, IBINDER, OBJECTARRAY,
            SERIALIZABLE, PERSISTABLEBUNDLE, SIZE, SIZEF);

    static void readValue(CodeBlock.Builder block, AutoParcelProcessor.Property property, final TypeName parcelableType) {

        if (property.isNullable()) {
            block.add("in.readInt() == 0 ? ");
        }

        if (parcelableType.equals(STRING)) {
            block.add("in.readString()");
        } else if (parcelableType.equals(TypeName.BYTE) || parcelableType.equals(TypeName.BYTE.box())) {
            block.add("in.readByte()");
        } else if (parcelableType.equals(TypeName.INT) || parcelableType.equals(TypeName.INT.box())) {
            block.add("in.readInt()");
        } else if (parcelableType.equals(TypeName.SHORT) || parcelableType.equals(TypeName.SHORT.box())) {
            block.add("(short) in.readInt()");
        } else if (parcelableType.equals(TypeName.CHAR) || parcelableType.equals(TypeName.CHAR.box())) {
            block.add("(char) in.readInt()");
        } else if (parcelableType.equals(TypeName.LONG) || parcelableType.equals(TypeName.LONG.box())) {
            block.add("in.readLong()");
        } else if (parcelableType.equals(TypeName.FLOAT) || parcelableType.equals(TypeName.FLOAT.box())) {
            block.add("in.readFloat()");
        } else if (parcelableType.equals(TypeName.DOUBLE) || parcelableType.equals(TypeName.DOUBLE.box())) {
            block.add("in.readDouble()");
        } else if (parcelableType.equals(TypeName.BOOLEAN) || parcelableType.equals(TypeName.BOOLEAN.box())) {
            block.add("in.readInt() == 1");
        } else if (parcelableType.equals(PARCELABLE)) {
            if (property.typeName.equals(PARCELABLE)) {
                block.add("in.readParcelable($T.class.getClassLoader())", parcelableType);
            } else {
                block.add("($T) in.readParcelable($T.class.getClassLoader())", property.typeName, property.typeName);
            }
        } else if (parcelableType.equals(CHARSEQUENCE)) {
            block.add("$T.CHAR_SEQUENCE_CREATOR.createFromParcel(in)", TEXTUTILS);
//        } else if (parcelableType.equals(MAP)) {
//            block.add("($T) in.readHashMap($T.class.getClassLoader())", property.typeName, parcelableType);
        } else if (parcelableType.equals(LIST)) {
            block.add("($T) in.readArrayList($T.class.getClassLoader())", property.typeName, parcelableType);
        } else if (parcelableType.equals(BOOLEANARRAY)) {
            block.add("in.createBooleanArray()");
        } else if (parcelableType.equals(BYTEARRAY)) {
            block.add("in.createByteArray()");
        } else if (parcelableType.equals(CHARARRAY)) {
            block.add("in.createCharArray()");
        } else if (parcelableType.equals(STRINGARRAY)) {
            block.add("in.readStringArray()");
        } else if (parcelableType.equals(IBINDER)) {
            if (property.typeName.equals(IBINDER)) {
                block.add("in.readStrongBinder()");
            } else {
                block.add("($T) in.readStrongBinder()", property.typeName);
            }
        } else if (parcelableType.equals(OBJECTARRAY)) {
            block.add("in.readArray($T.class.getClassLoader())", parcelableType);
        } else if (parcelableType.equals(INTARRAY)) {
            block.add("in.createIntArray()");
        } else if (parcelableType.equals(LONGARRAY)) {
            block.add("in.createLongArray()");
        } else if (parcelableType.equals(SERIALIZABLE)) {
            if (property.typeName.equals(SERIALIZABLE)) {
                block.add("in.readSerializable()");
            } else {
                block.add("($T) in.readSerializable()", property.typeName);
            }
        } else if (parcelableType.equals(PARCELABLEARRAY)) {
            ArrayTypeName atype = (ArrayTypeName) property.typeName;
            if (atype.componentType.equals(PARCELABLE)) {
                block.add("in.readParcelableArray($T.class.getClassLoader())", parcelableType);
            } else {
                // FIXME: 31/07/16 not sure if this works
                block.add("($T) in.readParcelableArray($T.class.getClassLoader())", atype, parcelableType);
            }
        } else if (parcelableType.equals(SPARSEARRAY)) {
            block.add("in.readSparseArray($T.class.getClassLoader())", parcelableType);
        } else if (parcelableType.equals(SPARSEBOOLEANARRAY)) {
            block.add("in.readSparseBooleanArray()");
        } else if (parcelableType.equals(BUNDLE)) {
            block.add("in.readBundle($T.class.getClassLoader())", property.typeName);
        } else if (parcelableType.equals(PERSISTABLEBUNDLE)) {
            block.add("in.readPersistableBundle($T.class.getClassLoader())", property.typeName);
        } else if (parcelableType.equals(SIZE)) {
            block.add("in.readSize()");
        } else if (parcelableType.equals(SIZEF)) {
            block.add("in.readSizeF()");
        } else if (parcelableType.equals(ENUM)) {
            block.add("$T.valueOf(in.readString())", property.typeName);
        } else {
            block.add("($T) in.readValue($T.class.getClassLoader())", property.typeName, parcelableType);
        }

        if (property.isNullable()) {
            block.add(" : null");
        }
    }

    public static void readValueWithTypeAdapter(CodeBlock.Builder block, AutoParcelProcessor.Property property, final FieldSpec adapter) {
        if (property.isNullable()) {
            block.add("in.readInt() == 0 ? ");
        }
        block.add("$N.fromParcel(in)", adapter);
        if (property.isNullable()) {
            block.add(" : null");
        }
    }

    public static CodeBlock writeVersion(int version, ParameterSpec out) {
        CodeBlock.Builder block = CodeBlock.builder();

        block.add("$N.writeInt( /* version */ " + version + ")", out);
        block.add(";\n");

        return block.build();
    }

    public static CodeBlock writeValue(AutoParcelProcessor.Property property, ParameterSpec out, ParameterSpec flags, Types typeUtils) {
        CodeBlock.Builder block = CodeBlock.builder();

        if (property.isNullable()) {
            block.beginControlFlow("if ($N == null)", property.fieldName);
            block.addStatement("$N.writeInt(1)", out);
            block.nextControlFlow("else");
            block.addStatement("$N.writeInt(0)", out);
        }

        TypeName type = getTypeNameFromProperty(property, typeUtils);

        if (type.equals(STRING))
            block.add("$N.writeString($N)", out, property.fieldName);
        else if (type.equals(TypeName.BYTE) || type.equals(TypeName.BYTE.box()))
            block.add("$N.writeInt($N)", out, property.fieldName);
        else if (type.equals(TypeName.INT) || type.equals(TypeName.INT.box()))
            block.add("$N.writeInt($N)", out, property.fieldName);
        else if (type.equals(TypeName.SHORT))
            block.add("$N.writeInt(((Short) $N).intValue())", out, property.fieldName);
        else if (type.equals(TypeName.SHORT.box()))
            block.add("$N.writeInt($N.intValue())", out, property.fieldName);
        else if (type.equals(TypeName.CHAR) || type.equals(TypeName.CHAR.box()))
            block.add("$N.writeInt($N)", out, property.fieldName);
        else if (type.equals(TypeName.LONG) || type.equals(TypeName.LONG.box()))
            block.add("$N.writeLong($N)", out, property.fieldName);
        else if (type.equals(TypeName.FLOAT) || type.equals(TypeName.FLOAT.box()))
            block.add("$N.writeFloat($N)", out, property.fieldName);
        else if (type.equals(TypeName.DOUBLE) || type.equals(TypeName.DOUBLE.box()))
            block.add("$N.writeDouble($N)", out, property.fieldName);
        else if (type.equals(TypeName.BOOLEAN) || type.equals(TypeName.BOOLEAN.box()))
            block.add("$N.writeInt($N ? 1 : 0)", out, property.fieldName);
        else if (type.equals(PARCELABLE))
            block.add("$N.writeParcelable($N, $N)", out, property.fieldName, flags);
        else if (type.equals(CHARSEQUENCE))
            block.add("$T.writeToParcel($N, $N, $N)", TEXTUTILS, property.fieldName, out, flags);
//        else if (type.equals(MAP))
//            block.add("$N.writeMap($N)", out, property.fieldName);
        else if (type.equals(LIST))
            block.add("$N.writeList($N)", out, property.fieldName);
        else if (type.equals(BOOLEANARRAY))
            block.add("$N.writeBooleanArray($N)", out, property.fieldName);
        else if (type.equals(BYTEARRAY))
            block.add("$N.writeByteArray($N)", out, property.fieldName);
        else if (type.equals(CHARARRAY))
            block.add("$N.writeCharArray($N)", out, property.fieldName);
        else if (type.equals(STRINGARRAY))
            block.add("$N.writeStringArray($N)", out, property.fieldName);
        else if (type.equals(IBINDER))
            block.add("$N.writeStrongBinder($N)", out, property.fieldName);
        else if (type.equals(OBJECTARRAY))
            block.add("$N.writeArray($N)", out, property.fieldName);
        else if (type.equals(INTARRAY))
            block.add("$N.writeIntArray($N)", out, property.fieldName);
        else if (type.equals(LONGARRAY))
            block.add("$N.writeLongArray($N)", out, property.fieldName);
        else if (type.equals(SERIALIZABLE))
            block.add("$N.writeSerializable($N)", out, property.fieldName);
        else if (type.equals(PARCELABLEARRAY))
            block.add("$N.writeParcelableArray($N)", out, property.fieldName);
        else if (type.equals(SPARSEARRAY))
            block.add("$N.writeSparseArray($N)", out, property.fieldName);
        else if (type.equals(SPARSEBOOLEANARRAY))
            block.add("$N.writeSparseBooleanArray($N)", out, property.fieldName);
        else if (type.equals(BUNDLE))
            block.add("$N.writeBundle($N)", out, property.fieldName);
        else if (type.equals(PERSISTABLEBUNDLE))
            block.add("$N.writePersistableBundle($N)", out, property.fieldName);
        else if (type.equals(SIZE))
            block.add("$N.writeSize($N)", out, property.fieldName);
        else if (type.equals(SIZEF))
            block.add("$N.writeSizeF($N)", out, property.fieldName);
        else if (type.equals(ENUM))
            block.add("$N.writeString($N.name())", out, property.fieldName);
        else
            block.add("$N.writeValue($N)", out, property.fieldName);

        block.add(";\n");

        if (property.isNullable()) {
            block.endControlFlow();
        }
        return block.build();
    }

    public static CodeBlock writeValueWithTypeAdapter(FieldSpec adapter, AutoParcelProcessor.Property p, ParameterSpec out) {
        CodeBlock.Builder block = CodeBlock.builder();

        if (p.isNullable()) {
            block.beginControlFlow("if ($N == null)", p.fieldName);
            block.addStatement("$N.writeInt(1)", out);
            block.nextControlFlow("else");
            block.addStatement("$N.writeInt(0)", out);
        }

        block.addStatement("$N.toParcel($N, $N)", adapter, p.fieldName, out);

        if (p.isNullable()) {
            block.endControlFlow();
        }

        return block.build();
    }

    static boolean isTypeRequiresSuppressWarnings(TypeName type) {
        return type.equals(LIST) ||
                type.equals(MAP);
    }

    static TypeName getTypeNameFromProperty(AutoParcelProcessor.Property property, Types types) {
        TypeMirror returnType = property.element.asType();
        TypeElement element = (TypeElement) types.asElement(returnType);
        if (element != null) {
            TypeName parcelableType = getParcelableType(types, element);
            if (!PARCELABLE.equals(parcelableType) && element.getKind() == ElementKind.ENUM) {
                return ENUM;
            }
            return parcelableType;
        }
        return property.typeName;
    }

    public static TypeName getParcelableType(Types types, TypeElement type) {
        TypeMirror typeMirror = type.asType();
        while (typeMirror.getKind() != TypeKind.NONE) {

            // first, check if the class is valid.
            TypeName typeName = TypeName.get(typeMirror);
            if (typeName instanceof ParameterizedTypeName) {
                typeName = ((ParameterizedTypeName) typeName).rawType;
            }
            if (isValidType(typeName)) {
                return typeName;
            }

            // then check if it implements valid interfaces
            for (TypeMirror iface : type.getInterfaces()) {
                TypeName inherited = getParcelableType(types, (TypeElement) types.asElement(iface));
                if (inherited != null) {
                    return inherited;
                }
            }
            // then move on
            type = (TypeElement) types.asElement(typeMirror);
            typeMirror = type.getSuperclass();
        }
        return null;
    }

    public static boolean isValidType(TypeName typeName) {
        return typeName.isPrimitive() || typeName.isBoxedPrimitive() || VALID_TYPES.contains(typeName);
    }
}
