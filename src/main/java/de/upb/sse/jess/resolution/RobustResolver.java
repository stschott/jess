package de.upb.sse.jess.resolution;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.*;
import java.util.stream.Collectors;

public class RobustResolver {

    public static ResolvedType tryResolve(Type type) {
        try {
            return type.resolve();
        } catch (Exception e) {
            return null;
        }
    }


    public static ResolvedType tryResolve(Expression exp) {
        try {
            return exp.calculateResolvedType();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResolvedType tryResolve(ClassOrInterfaceType cit) {
        try {
            return cit.resolve();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResolvedValueDeclaration tryResolve(FieldAccessExpr fae) {
        try {
            return fae.resolve();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResolvedValueDeclaration tryResolve(FieldDeclaration fd) {
        try {
            return fd.resolve();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResolvedValueDeclaration tryResolve(NameExpr ne) {
        try {
            return ne.resolve();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResolvedMethodDeclaration tryResolve(MethodCallExpr mce) {
        // TODO: if not resolvable, try to match name and amount of arguments
//            System.out.println(mce);
//            System.out.println(mce.getArguments());
        try {
            return mce.resolve();
        } catch (Exception e) {
            return InternalTypeSolver.tryResolve(mce);
        }
    }

    public static ResolvedMethodDeclaration tryResolve(MethodReferenceExpr mre) {
        try {
            return mre.resolve();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResolvedMethodDeclaration tryResolve(MethodDeclaration md) {
        try {
            return md.resolve();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResolvedConstructorDeclaration tryResolve(ConstructorDeclaration cd) {
        try {
            return cd.resolve();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResolvedConstructorDeclaration tryResolve(ObjectCreationExpr oce) {
        try {
            return oce.resolve();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResolvedConstructorDeclaration tryResolve(ExplicitConstructorInvocationStmt ecis) {
        try {
            return ecis.resolve();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResolvedAnnotationDeclaration tryResolve(SingleMemberAnnotationExpr smae) {
        try {
            return smae.resolve();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResolvedEnumConstantDeclaration tryResolve(EnumConstantDeclaration ecd) {
        try {
            return ecd.resolve();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResolvedReferenceTypeDeclaration tryGetDeclaringType(ResolvedMethodDeclaration rmd) {
        try {
            return rmd.declaringType();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResolvedTypeDeclaration tryGetDeclaringType(ResolvedFieldDeclaration rfd) {
        try {
            return rfd.declaringType();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResolvedReferenceTypeDeclaration tryGetDeclaringType(ResolvedConstructorDeclaration rcd) {
        try {
            return rcd.declaringType();
        } catch (Exception e) {
            return null;
        }
    }



    public static String tryDescribe(ResolvedType rt) {
        try {
            return rt.describe();
        } catch (Exception e) {
            return null;
        }
    }

    public static List<ResolvedReferenceType> tryResolveAllInterfacesExtended(ResolvedInterfaceDeclaration rid) {
        try {
            return rid.getAllInterfacesExtended();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static Optional<ResolvedReferenceType> tryResolveSuperClass(ResolvedClassDeclaration rcd) {
        try {
            return rcd.getSuperClass();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static boolean tryIsFunctionalInterface(ResolvedReferenceTypeDeclaration rrtd) {
        try {
            return rrtd.isFunctionalInterface();
        } catch (Exception e) {
            return false;
        }
    }

    public static Set<MethodUsage> tryGetDeclaredMethods(ResolvedReferenceType type) {
        try {
            return type.getDeclaredMethods();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    public static Set<ResolvedMethodDeclaration> tryGetDeclaredMethods(ResolvedReferenceTypeDeclaration type) {
        try {
            return type.getDeclaredMethods();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    public static List<ResolvedMethodDeclaration> getAllResolvableSuperMethods(ResolvedReferenceType rrt) {
        List<ResolvedMethodDeclaration> allSuperMethods = new ArrayList<>();
        if (rrt.getQualifiedName().equals("java.lang.Object")) return allSuperMethods;

        ResolvedReferenceType superClass = rrt;
        do {
            Optional<ResolvedReferenceTypeDeclaration> superClassDecOpt = superClass.getTypeDeclaration();
            ResolvedReferenceTypeDeclaration superClassDec = superClassDecOpt.orElse(null);
            if (superClassDec == null) return allSuperMethods;

            allSuperMethods.addAll(superClassDec.getDeclaredMethods());

            if (!(superClassDec instanceof ResolvedClassDeclaration)) return allSuperMethods;
            ResolvedClassDeclaration superClassRcd = (ResolvedClassDeclaration) superClassDec;
            Optional<ResolvedReferenceType> superClassOpt = tryResolveSuperClass(superClassRcd);
            superClass = superClassOpt.orElse(null);

        } while (superClass != null);

        return allSuperMethods;
    }

    public static List<ResolvedMethodDeclaration> getAllResolvableMethodsVisibleToInheritors(ResolvedReferenceType rrt) {
        return getAllResolvableSuperMethods(rrt).stream().filter(m -> m.accessSpecifier() != AccessSpecifier.PRIVATE).collect(Collectors.toList());
    }

    public static ResolvedType tryResolveParamType(ResolvedParameterDeclaration param) {
        try {
            return param.getType();
        } catch (Exception e) {
            return null;
        }
    }

}
