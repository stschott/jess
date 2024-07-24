package de.upb.sse.jess.visitors;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.*;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionConstructorDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionEnumConstantDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFieldDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import de.upb.sse.jess.model.ResolutionInformation;
import de.upb.sse.jess.annotation.Annotator;
import de.upb.sse.jess.resolution.RobustResolver;

import java.util.*;

public class ResolutionVisitor extends VoidVisitorAdapter<Void> {
    private final Annotator annotator;

    public ResolutionVisitor(Annotator ann) {
        this.annotator = ann;
    }

    @Override
    public void visit(ClassOrInterfaceType cit, Void arg) {
        super.visit(cit, arg);
        ResolvedType rt = RobustResolver.tryResolve(cit);

        if (rt == null) return;
        if (rt.isTypeVariable()) return;

        annotator.keep(rt);
    }

    @Override
    public void visit(MethodCallExpr mce, Void arg) {
        super.visit(mce, arg);
        ResolvedMethodDeclaration rmd = RobustResolver.tryResolve(mce);

        if (rmd instanceof ReflectionMethodDeclaration) {
            // do nothing
        } else if (rmd instanceof JavaParserMethodDeclaration) {
            annotator.keep(rmd);
            if (rmd.isStatic() && mce.getScope().isPresent()) {
                Expression scope = mce.getScope().get();
                ResolvedType resolvedType = RobustResolver.tryResolve(scope);
                if (resolvedType == null) return;

                annotator.keep(resolvedType);
            }
        }
    }

    @Override
    public void visit(ObjectCreationExpr oce, Void arg) {
        super.visit(oce, arg);
        ResolvedConstructorDeclaration rcd = RobustResolver.tryResolve(oce);

        if (rcd instanceof ReflectionConstructorDeclaration) {
            // do nothing
        } else if (rcd instanceof JavaParserConstructorDeclaration) {
            annotator.keep(rcd);
        }
    }

    @Override
    public void visit(FieldAccessExpr fae, Void arg) {
        super.visit(fae, arg);
        if (fae.getScope().isMethodCallExpr()) return;
        ResolvedType scopeType = RobustResolver.tryResolve(fae.getScope());
        if (scopeType != null && scopeType.isArray()) return;

        ResolvedValueDeclaration rvd = RobustResolver.tryResolve(fae);

        if (rvd instanceof ReflectionFieldDeclaration || rvd instanceof ReflectionEnumConstantDeclaration) {
            // do nothing
        } else if (rvd instanceof JavaParserFieldDeclaration) {
            JavaParserFieldDeclaration jvd = (JavaParserFieldDeclaration) rvd;
            annotator.keep(rvd);

            if (jvd.isStatic()) {
                Expression scope = fae.getScope();
                ResolvedType resolvedType = RobustResolver.tryResolve(scope);
                if (resolvedType == null) return;

                annotator.keep(resolvedType);
            }
        } else if (rvd instanceof JavaParserEnumConstantDeclaration) {
            JavaParserEnumConstantDeclaration jed = (JavaParserEnumConstantDeclaration) rvd;
            annotator.keep(rvd);
        }
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt eci, Void arg) {
        super.visit(eci, arg);
        if (eci.isThis()) return;

        ResolvedConstructorDeclaration rcd = RobustResolver.tryResolve(eci);

        if (rcd instanceof ReflectionConstructorDeclaration) {
            // do nothing
        } else if (rcd instanceof JavaParserConstructorDeclaration) {
            annotator.keep(rcd);
        }
    }

    @Override
    public void visit(MethodReferenceExpr mre, Void arg) {
        super.visit(mre, arg);

        if (mre.getIdentifier().equals("new")) {
            // constructor invocation during MethodReferenceExpr
            Expression scope = mre.getScope();

            ResolvedType rt = RobustResolver.tryResolve(scope);
            if (rt == null) return;
            if (!rt.isReferenceType()) return;

            Optional<ResolvedReferenceTypeDeclaration> typeDeclarationOpt = rt.asReferenceType().getTypeDeclaration();
            if (typeDeclarationOpt.isEmpty()) return;

            ResolvedReferenceTypeDeclaration typeDeclaration = typeDeclarationOpt.get();
            if (!(typeDeclaration instanceof JavaParserClassDeclaration)) return;

            List<ResolvedConstructorDeclaration> constructors = typeDeclaration.getConstructors();
            for (ResolvedConstructorDeclaration constructor : constructors) {
                annotator.keep(constructor);
            }
        } else {
            // method invocation during MethodReferenceExpr
            Expression scope = mre.getScope();
            ResolvedType rt = RobustResolver.tryResolve(scope);
            if (rt == null) return;

            List<ResolvedType> relevantTypes = new ArrayList<>();

            if (rt.isTypeVariable()) {
                // class A<S extends B> { S delegate; delegate::onClose }
                ResolvedTypeVariable rtv = rt.asTypeVariable();
                ResolvedTypeParameterDeclaration rtpd = rtv.asTypeParameter();

                List<ResolvedTypeParameterDeclaration.Bound> bounds = rtpd.getBounds();
                for (ResolvedTypeParameterDeclaration.Bound bound : bounds) {
                    ResolvedType boundType = bound.getType();
                    if (!boundType.isReferenceType()) continue;
                    relevantTypes.add(boundType);
                }
            } else if (rt.isReferenceType()) {
                relevantTypes.add(rt);
            }

            for (ResolvedType relevantType : relevantTypes) {
                Optional<ResolvedReferenceTypeDeclaration> typeDeclarationOpt = relevantType.asReferenceType().getTypeDeclaration();
                if (typeDeclarationOpt.isEmpty()) return;

                ResolvedReferenceTypeDeclaration typeDeclaration = typeDeclarationOpt.get();
                if (!(typeDeclaration instanceof JavaParserClassDeclaration) && !(typeDeclaration instanceof JavaParserInterfaceDeclaration)) return;

                Set<ResolvedMethodDeclaration> methods = typeDeclaration.getDeclaredMethods();
                for (ResolvedMethodDeclaration method : methods) {
                    if (!method.getName().equals(mre.getIdentifier())) continue;
                    annotator.keep(method);
                }
            }
        }
     }

    @Override
    public void visit(NameExpr ne, Void arg) {
        super.visit(ne, arg);

        ResolvedValueDeclaration rvd = RobustResolver.tryResolve(ne);
        if (rvd == null) return;

        if (rvd instanceof JavaParserFieldDeclaration) {
            JavaParserFieldDeclaration jfd = (JavaParserFieldDeclaration) rvd;
            annotator.keep(jfd);
        } else if (rvd.isField()) {
            ResolvedFieldDeclaration rfd = rvd.asField();
            annotator.keep(rfd);
        }
    }

    @Override
    public void visit(TryStmt ts, Void arg) {
        super.visit(ts, arg);
        // If a method is used in a try-with-resources statement, a constructor needs to be present in the class, even
        // if no object is created explicitly
        NodeList<Expression> resources = ts.getResources();
        for (Expression exp : resources) {
            if (!exp.isVariableDeclarationExpr()) continue;
            VariableDeclarationExpr vde = exp.asVariableDeclarationExpr();
            NodeList<VariableDeclarator> variables = vde.getVariables();

            for (VariableDeclarator variable : variables) {
                Optional<Expression> initializerOpt = variable.getInitializer();
                if (initializerOpt.isEmpty()) continue;
                Expression initializer = initializerOpt.get();
                if (!initializer.isMethodCallExpr()) continue;
                MethodCallExpr mce = initializer.asMethodCallExpr();

                ResolvedMethodDeclaration rmd = RobustResolver.tryResolve(mce);
                if (rmd == null) continue;
                if (!rmd.isStatic()) continue;

                ResolvedReferenceTypeDeclaration declaringType = rmd.declaringType();
                Optional<ResolvedConstructorDeclaration> conOpt = declaringType.getConstructors().stream().findFirst();
                conOpt.ifPresent(annotator::keep);

            }
        }
    }

    @Override
    public void visit(ClassExpr ce, Void arg) {
        super.visit(ce, arg);

        ResolvedType rt = RobustResolver.tryResolve(ce.getType());
        if (rt == null) return;

        annotator.keep(rt);
    }

}
