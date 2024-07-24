package de.upb.sse.jess.resolution;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserEnumDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserInterfaceDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionEnumDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionInterfaceDeclaration;
import de.upb.sse.jess.Jess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RobustAncestorResolver {

    // resolve all interfaces of an enum
    public static List<ResolvedReferenceType> tryResolveAllInterfaces(ResolvedEnumDeclaration red) {
        return robustlyResolveAllInterfaces(red);
    }

    // resolve all interfaces of a class
    public static List<ResolvedReferenceType> tryResolveAllInterfaces(ResolvedClassDeclaration rcd) {
        try {
            return rcd.getAllInterfaces();
        } catch (Exception e) {
            return robustlyResolveAllInterfaces(rcd);
        }
    }

    // resolve all extended interfaces of an interface
    public static List<ResolvedReferenceType> tryResolveAllInterfaces(ResolvedInterfaceDeclaration rcd) {
        try {
            return rcd.getInterfacesExtended();
        } catch (Exception e) {
            return robustlyResolveAllInterfaces(rcd);
        }
    }

    // resolve direct interfaces of an enum
    public static List<ResolvedReferenceType> tryResolveInterfaces(ResolvedEnumDeclaration red) {
        return robustlyResolveDirectInterfaces(red);
    }

    // resolve direct interfaces of a class
    public static List<ResolvedReferenceType> tryResolveInterfaces(ResolvedClassDeclaration rcd) {
        try {
            return rcd.getInterfaces();
        } catch (Exception e) {
            return robustlyResolveDirectInterfaces(rcd);
        }
    }

    // resolve direct superclass of a class
    public static Optional<ResolvedReferenceType> tryResolveSuperclass(ResolvedClassDeclaration rcd) {
        try {
            return rcd.getSuperClass();
        } catch (Exception e) {
            return robustlyResolveDirectSuperclass(rcd);
        }
    }

    private static List<ResolvedReferenceType> robustlyResolveAllInterfaces(ResolvedReferenceTypeDeclaration typeDec) {
        List<ResolvedReferenceType> allInterfaces = new ArrayList<>();

        List<ResolvedReferenceTypeDeclaration> worklist = new ArrayList<>();
        worklist.add(typeDec);

        do {
            ResolvedReferenceTypeDeclaration currTypeDec = worklist.remove(0);
            List<ResolvedReferenceType> currInterfaces = robustlyResolveDirectInterfaces(currTypeDec);
            allInterfaces.addAll(currInterfaces);

            Optional<ResolvedReferenceType> directSuperclassOpt = Optional.empty();
            if (currTypeDec instanceof ReflectionClassDeclaration) {
                ResolvedClassDeclaration classTypeDec = (ResolvedClassDeclaration) currTypeDec;
                directSuperclassOpt = robustlyResolveDirectSuperclass(classTypeDec);
            }

            List<ResolvedReferenceType> mergedAncestors = new ArrayList<>(currInterfaces);
            directSuperclassOpt.ifPresent(mergedAncestors::add);

            List<ResolvedReferenceTypeDeclaration> nextLayerTypeDecs = mergedAncestors.stream()
                    .filter(ci -> ci.getTypeDeclaration().isPresent())
                    .map(ci -> ci.getTypeDeclaration().get())
                    .distinct()
                    .collect(Collectors.toList());

            worklist.addAll(nextLayerTypeDecs);

        } while (!worklist.isEmpty());

        return allInterfaces.stream().distinct().collect(Collectors.toList());
    }

    private static List<ResolvedReferenceType> robustlyResolveDirectInterfaces(ResolvedReferenceTypeDeclaration typeDec) {
        List<ResolvedReferenceType> resolvedInterfaces = new ArrayList<>();

        if (isReflectiveType(typeDec)) {
            resolvedInterfaces.addAll(getReflectiveInterfaces(typeDec));
        } else {
            TypeDeclaration<?> typeDecNode = getWrappedNode(typeDec);
            if (typeDecNode == null) return Collections.emptyList();
            Jess.inject(typeDecNode);

            List<ClassOrInterfaceType> implementedTypes = new ArrayList<>();
            if (typeDecNode instanceof ClassOrInterfaceDeclaration) {
                ClassOrInterfaceDeclaration classOrIntDec = (ClassOrInterfaceDeclaration) typeDecNode;
                if (classOrIntDec.isInterface()) {
                    implementedTypes = classOrIntDec.getExtendedTypes();
                } else {
                    implementedTypes = classOrIntDec.getImplementedTypes();
                }

            } else if (typeDecNode instanceof EnumDeclaration) {
                EnumDeclaration enumDecNode = (EnumDeclaration) typeDecNode;
                implementedTypes = enumDecNode.getImplementedTypes();
            }


            for (ClassOrInterfaceType implementedType : implementedTypes) {
                ResolvedType resolvedType = RobustResolver.tryResolve(implementedType);
                if (resolvedType == null) continue;
                if (!resolvedType.isReferenceType()) continue;

                resolvedInterfaces.add(resolvedType.asReferenceType());
            }
        }

        return resolvedInterfaces.stream().distinct().collect(Collectors.toList());
    }

    private static Optional<ResolvedReferenceType> robustlyResolveDirectSuperclass(ResolvedClassDeclaration typeDec) {
        if (isReflectiveType(typeDec)) return getReflectiveSuperclass(typeDec);

        TypeDeclaration<?> typeDecNode = getWrappedNode(typeDec);
        if (typeDecNode == null) return Optional.empty();
        Jess.inject(typeDecNode);

        if (!(typeDecNode instanceof ClassOrInterfaceDeclaration)) return Optional.empty();
        ClassOrInterfaceDeclaration classOrIntDec = (ClassOrInterfaceDeclaration) typeDecNode;
        if (classOrIntDec.isInterface()) return Optional.empty();
        Optional<ClassOrInterfaceType> superclassOpt = classOrIntDec.getExtendedTypes().stream().findFirst();
        if (superclassOpt.isEmpty()) return Optional.empty();
        ClassOrInterfaceType superclass = superclassOpt.get();
        ResolvedType resolvedType = RobustResolver.tryResolve(superclass);
        if (resolvedType == null) return Optional.empty();
        if (!resolvedType.isReferenceType()) return Optional.empty();

        return Optional.of(resolvedType.asReferenceType());
    }


    private static TypeDeclaration<?> getWrappedNode(ResolvedReferenceTypeDeclaration rrtd) {
        if (rrtd instanceof JavaParserClassDeclaration) return ((JavaParserClassDeclaration) rrtd).getWrappedNode();
        if (rrtd instanceof JavaParserEnumDeclaration) return ((JavaParserEnumDeclaration) rrtd).getWrappedNode();
        if (rrtd instanceof JavaParserInterfaceDeclaration) return ((JavaParserInterfaceDeclaration) rrtd).getWrappedNode();
        return null;
    }


    private static boolean isReflectiveType(ResolvedReferenceTypeDeclaration rrtd) {
        return rrtd instanceof ReflectionClassDeclaration ||
                rrtd instanceof ReflectionInterfaceDeclaration ||
                rrtd instanceof ReflectionEnumDeclaration;
    }

    private static List<ResolvedReferenceType> getReflectiveInterfaces(ResolvedReferenceTypeDeclaration rrtd) {
        if (rrtd instanceof ReflectionClassDeclaration) {
            return ((ReflectionClassDeclaration) rrtd).getInterfaces();
        } else if (rrtd instanceof ReflectionEnumDeclaration) {
            return ((ReflectionEnumDeclaration) rrtd).getAncestors();
        } else if (rrtd instanceof ReflectionInterfaceDeclaration) {
            return ((ReflectionInterfaceDeclaration) rrtd).getInterfacesExtended();
        }
        return Collections.emptyList();
    }

    private static Optional<ResolvedReferenceType> getReflectiveSuperclass(ResolvedClassDeclaration rcd) {
            return rcd.getSuperClass();
    }
}
