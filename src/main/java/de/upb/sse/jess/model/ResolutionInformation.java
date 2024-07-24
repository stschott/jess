package de.upb.sse.jess.model;

import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResolutionInformation {

    private final Set<String> resolvableTypes = new HashSet<>();
    private final Set<String> unresolvableTypes = new HashSet<>();
    private final Set<String> staticMethods = new HashSet<>();
    private final Set<String> staticFields = new HashSet<>();


    public Set<String> getResolvableTypes() {
        return resolvableTypes;
    }

    public Set<String> getUnresolvableTypes() {
        return unresolvableTypes;
    }

    public Set<String> getStaticMethods() {
        return staticMethods;
    }

    public Set<String> getStaticFields() {
        return staticFields;
    }

    public Set<String> getAllTypes() {
        return Stream.concat(resolvableTypes.stream(), unresolvableTypes.stream()).collect(Collectors.toSet());
    }

    public void addResolvableType(ResolvedType type) {
        ResolvedType rawType = type instanceof ReferenceTypeImpl ? ((ReferenceTypeImpl) type).toRawType() : type;
//        resolvableTypes.add(rawType.erasure().erasure().describe());
        resolvableTypes.add(rawType.describe().replaceAll("<.*?>", ""));
    }

    public void addResolvableType(String typeName) {
        resolvableTypes.add(typeName);
    }

    public void addUnresolvableType(String type) {
        unresolvableTypes.add(type);
    }

    public void addStaticMethod(String qualifiedName) {
        staticMethods.add(qualifiedName);
    }

    public void addStaticField(String qualifiedName) {
        staticFields.add(qualifiedName);
    }

    @Override
    public String toString() {
        return "ResolutionInformation{" +
                "resolvableTypes=" + resolvableTypes +
                ", unresolvableTypes=" + unresolvableTypes +
                ", staticMethods=" + staticMethods +
                ", staticFinalFields=" + staticFields +
                '}';
    }
}
