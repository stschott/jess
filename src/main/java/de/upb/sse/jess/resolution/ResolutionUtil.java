package de.upb.sse.jess.resolution;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import de.upb.sse.jess.annotation.Annotator;
import de.upb.sse.jess.util.MatchingUtil;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ResolutionUtil {

    public static boolean isReflectiveType(ResolvedType type) {
        String typeDescription = RobustResolver.tryDescribe(type);
        if (typeDescription == null) return false;
        return isReflectiveType(getErasure(type.describe()));
    }

    public static boolean isReflectiveType(String type) {
        try {
            new ReflectionTypeSolver().solveType(type);
            return true;
        } catch (UnsolvedSymbolException e) {
            return false;
        }
    }

    public static boolean isInDefaultPackage(String type) {
        if (type.startsWith("java.lang")) return isReflectiveType(type);

        return isReflectiveType("java.lang." + type);
    }

    public static String getErasure(String type) {
        int genericCharLocation = type.indexOf('<');
        return genericCharLocation > -1 ? type.substring(0, genericCharLocation) : type;
    }

    public static boolean isTargetClass(ResolvedReferenceType type, String targetClass) {
        return type.getQualifiedName().equals(targetClass);
    }

    public static boolean isKeptMethod(ResolvedMethodDeclaration rmd) {
        if (!(rmd instanceof JavaParserMethodDeclaration)) return false;

        JavaParserMethodDeclaration jmd = (JavaParserMethodDeclaration) rmd;
        MethodDeclaration md = jmd.getWrappedNode();
        return md.isAnnotationPresent(Annotator.KEEP_ANNOTATION) || md.isAnnotationPresent(Annotator.KEEP_ALL_ANNOTATION);
    }

    public static boolean isOverridingAbstractSuperMethod(MethodDeclaration md, String targetClass) {
        ResolvedMethodDeclaration rmd = md.resolve();
        ResolvedReferenceTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(rmd);
        if (typeDec == null) return false;

        if (typeDec.isClass()) {

            ResolvedClassDeclaration rClass = typeDec.asClass();
            Optional<ResolvedReferenceType> superClassOpt = RobustAncestorResolver.tryResolveSuperclass(rClass);

            if (superClassOpt.isPresent()) {
                ResolvedReferenceType superClass = superClassOpt.get();

                for (ResolvedMethodDeclaration resolvedSuperMethod : RobustResolver.getAllResolvableSuperMethods(superClass)) {
                    if (!resolvedSuperMethod.isAbstract()) continue;
                    if (!(resolvedSuperMethod instanceof ReflectionMethodDeclaration) && !isTargetClass(superClass, targetClass) && !isKeptMethod(resolvedSuperMethod)) continue;
//                    if (!isTransitivelyOverridingReflectionMethodDeclaration(resolvedSuperMethod)) continue;
                    if (!MatchingUtil.areSignaturesMatching(rmd, resolvedSuperMethod)) continue;
                    return true;
                }
            }
        } else if (typeDec.isInterface()) {
            // default interface method check
            ResolvedInterfaceDeclaration rid = typeDec.asInterface();
            List<ResolvedReferenceType> extendedInterfaces = RobustAncestorResolver.tryResolveAllInterfaces(rid);

            for (ResolvedReferenceType ref : extendedInterfaces) {
                for (MethodUsage interfaceMethod : RobustResolver.tryGetDeclaredMethods(ref)) {
                    ResolvedMethodDeclaration resolvedInterfaceMethod = interfaceMethod.getDeclaration();
                    if (!resolvedInterfaceMethod.isAbstract()) continue;
                    if (!md.isDefault()) continue;
                    if (!MatchingUtil.areSignaturesMatching(rmd, resolvedInterfaceMethod)) continue;
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isOverridingAbstractInterfaceMethod(MethodDeclaration md, String targetClass) {
        ResolvedMethodDeclaration rmd = md.resolve();
        ResolvedReferenceTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(rmd);
        if (typeDec == null) return false;

        List<ResolvedReferenceType> interfaces;

        if (typeDec.isEnum()) {
            ResolvedEnumDeclaration rEnum = typeDec.asEnum();
            interfaces = RobustAncestorResolver.tryResolveInterfaces(rEnum);
        } else if (typeDec.isClass()) {
            ResolvedClassDeclaration rClass = typeDec.asClass();
            interfaces = RobustAncestorResolver.tryResolveAllInterfaces(rClass);
        } else {
            return false;
        }


        for (ResolvedReferenceType inter : interfaces) {
            Optional<ResolvedReferenceTypeDeclaration> typeDecOpt = inter.getTypeDeclaration();
            if (!isReflectiveType(inter) &&
                    !isTargetClass(inter, targetClass) &&
                    typeDecOpt.isPresent() &&
                    !RobustResolver.tryIsFunctionalInterface(typeDecOpt.get())
            ) continue;

            for (ResolvedMethodDeclaration interfaceMethod : inter.getAllMethods()) {
                if (!MatchingUtil.areSignaturesMatching(rmd, interfaceMethod)) continue;
                return true;
            }
        }
        return false;
    }

    public static boolean isAbstractFunctionalInterfaceMethod(MethodDeclaration md) {
        ResolvedMethodDeclaration rmd = md.resolve();
        ResolvedReferenceTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(rmd);
        if (typeDec == null) return false;

        if (!rmd.isAbstract()) return false;
        if (!typeDec.isInterface()) return false;
        if (!RobustResolver.tryIsFunctionalInterface(typeDec)) return false;

        return true;
    }

    public static boolean isOverridingKeptInterfaceMethod(MethodDeclaration md) {
        ResolvedMethodDeclaration rmd = md.resolve();
        ResolvedReferenceTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(rmd);
        if (typeDec == null) return false;

        if (!typeDec.isClass()) return false;
        ResolvedClassDeclaration rClass = typeDec.asClass();

        List<ResolvedReferenceType> interfaces = RobustAncestorResolver.tryResolveAllInterfaces(rClass);
        for (ResolvedReferenceType inter : interfaces) {

            for (ResolvedMethodDeclaration interfaceMethod : RobustResolver.getAllResolvableMethodsVisibleToInheritors(inter)) {
                if (!interfaceMethod.isAbstract()) continue;
                if (!MatchingUtil.areSignaturesMatching(rmd, interfaceMethod)) continue;
                Optional<MethodDeclaration> interfaceMethodDeclarationOpt = interfaceMethod.toAst(MethodDeclaration.class);
                if (interfaceMethodDeclarationOpt.isEmpty()) continue;
                MethodDeclaration interfaceMethodDeclaration = interfaceMethodDeclarationOpt.get();
                if (!interfaceMethodDeclaration.isAnnotationPresent(Annotator.KEEP_ANNOTATION)) continue;
                return true;
            }
        }
        return false;
    }

    public static boolean isOverridingAbstractEnumMethod(MethodDeclaration md) {
        ResolvedMethodDeclaration rmd = md.resolve();
        ResolvedReferenceTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(rmd);
        if (typeDec == null) return false;

        if (!typeDec.isEnum()) return false;

        Set<ResolvedMethodDeclaration> enumMethods = RobustResolver.tryGetDeclaredMethods(typeDec);
        for (ResolvedMethodDeclaration enumMethod : enumMethods) {
            if (!enumMethod.isAbstract()) continue;
            if (!MatchingUtil.areSignaturesMatching(rmd, enumMethod)) continue;
            Optional<MethodDeclaration> interfaceMethodDeclarationOpt = enumMethod.toAst(MethodDeclaration.class);
            if (interfaceMethodDeclarationOpt.isEmpty()) continue;
            MethodDeclaration interfaceMethodDeclaration = interfaceMethodDeclarationOpt.get();
            if (!interfaceMethodDeclaration.isAnnotationPresent(Annotator.KEEP_ANNOTATION)) continue;
            return true;
        }
        return false;
    }

}
