package de.upb.sse.jess.model.stubs;

import de.upb.sse.jess.util.ImportUtil;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class ClassType {
    private final TypeContext type;
    private final String packageName;

    private boolean annotationType = false;
    private boolean interfaceType = false;
    private int typeParameters = 0;

    private final Set<String> imports = new HashSet<>();
    private final Set<MethodType> methodTypes = new HashSet<>();
    private final Set<FieldType> fieldTypes = new HashSet<>();
    private final Set<TypeContext> interfaceImplementations = new HashSet<>();
    private final Set<TypeContext> extendedTypes = new HashSet<>();

    public String getClassName() {
        return type.getType();
    }

    public String getFQN() {
        return packageName + "." + getClassName();
    }

    public void addImport(String i) {
        imports.add(i);
    }

    public void addFieldType(FieldType ft) {
        fieldTypes.add(ft);
    }

    public void addMethodType(MethodType mt) {
        methodTypes.add(mt);
    }

    public void addInterfaceImplementation(TypeContext interfaceType) {
        interfaceImplementations.add(interfaceType);
    }

    public void addExtendedType(TypeContext extendedType) {
        extendedTypes.add(extendedType);
    }

    public boolean containsImport(String type) {
        return imports.stream()
                .map(imp -> ImportUtil.cutoffAfterLastCharOccurrence(imp, '.'))
                .anyMatch(imp -> imp.equals(type));
    }

    @Override
    public String toString() {
        return "ClassType{" + '\n' +
                "  type='" + type + '\'' + '\n' +
                ", packageName='" + packageName + '\'' + '\n' +
                ", annotationType=" + annotationType + '\n' +
                ", interfaceType=" + interfaceType + '\n' +
                ", typeParameters=" + typeParameters + '\n' +
                ", imports=" + imports + '\n' +
                ", methodTypes=" + methodTypes + '\n' +
                ", fieldTypes=" + fieldTypes + '\n' +
                ", interfaceImplementations=" + interfaceImplementations + '\n' +
                ", extendedTypes='" + extendedTypes + '\'' + '\n' +
                '}';
    }
}

