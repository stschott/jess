package de.upb.sse.jess.visitors;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import de.upb.sse.jess.model.ResolutionInformation;
import de.upb.sse.jess.resolution.RobustResolver;

import java.util.Optional;


public class TypeUsageVisitor extends VoidVisitorAdapter<ResolutionInformation> {

    @Override
    public void visit(ClassOrInterfaceType cit, ResolutionInformation resInfo) {
        super.visit(cit, resInfo);
        ResolvedType rt = RobustResolver.tryResolve(cit);

        if (rt == null || rt.isTypeVariable()) {
            resInfo.addUnresolvableType(cit.getNameAsString());
            return;
        }

        resInfo.addResolvableType(rt);
    }

    @Override
    public void visit(MethodCallExpr mce, ResolutionInformation resInfo) {
        super.visit(mce, resInfo);

        ResolvedMethodDeclaration resolvedMethodDeclaration = RobustResolver.tryResolve(mce);
        if (resolvedMethodDeclaration != null && resolvedMethodDeclaration.isStatic()) {
            resInfo.addStaticMethod(mce.getNameAsString());
        } else {
            // an unresolvable method MAY be static as well
            resInfo.addStaticMethod(mce.getNameAsString());
        }

        Optional<Expression> scopeOpt = mce.getScope();
        if (scopeOpt.isEmpty()) return;
        Expression scope = scopeOpt.get();

        ResolvedType rt = RobustResolver.tryResolve(scopeOpt.get());

        if (rt == null || !rt.isReferenceType()) {
            resInfo.addUnresolvableType(scope.toString());
            return;
        }

        resInfo.addResolvableType(rt);
    }

    @Override
    public void visit(FieldAccessExpr fae, ResolutionInformation resInfo) {
        super.visit(fae, resInfo);

        ResolvedValueDeclaration resolvedValueDeclaration = RobustResolver.tryResolve(fae);
        if (resolvedValueDeclaration != null && resolvedValueDeclaration.isField() && resolvedValueDeclaration.asField().isStatic()) {
            resInfo.addStaticField(fae.getNameAsString());
        } else {
            // an unresolvable method MAY be static as well
            resInfo.addStaticField(fae.getNameAsString());
        }
    }

    @Override
    public void visit(NameExpr ne, ResolutionInformation resInfo) {
        super.visit(ne, resInfo);

        ResolvedValueDeclaration rvd = RobustResolver.tryResolve(ne);
        if (rvd instanceof JavaParserFieldDeclaration) {
            JavaParserFieldDeclaration jfd = (JavaParserFieldDeclaration) rvd;
            resInfo.addResolvableType(jfd.declaringType().getQualifiedName() + "." + ne.getNameAsString());
        } else {
            ResolvedType rt = RobustResolver.tryResolve((Expression) ne);
            if (rt != null && rt.isReferenceType()) resInfo.addResolvableType(rt);
            else resInfo.addUnresolvableType(ne.getNameAsString());
        }

    }

    @Override
    public void visit(SingleMemberAnnotationExpr anno, ResolutionInformation resInfo) {
        super.visit(anno, resInfo);
//        ResolvedAnnotationDeclaration rad = RobustResolver.tryResolve(anno);
//
//        if (rad == null) {
//            resInfo.addUnresolvableType(anno.getNameAsString());
//            return;
//        }
//
//        resInfo.addResolvableType(rad.getQualifiedName());
    }

    @Override
    public void visit(MarkerAnnotationExpr anno, ResolutionInformation resInfo) {
        super.visit(anno, resInfo);
//        ResolvedType rt = RobustResolver.tryResolve(anno);
//
//        if (rt == null) {
//            resInfo.addUnresolvableType(anno.getNameAsString());
//            return;
//        }
//
//        resInfo.addResolvableType(rt.toString());
    }

    @Override
    public void visit(NormalAnnotationExpr anno, ResolutionInformation resInfo) {
        super.visit(anno, resInfo);
//        ResolvedType rt = RobustResolver.tryResolve(anno);
//
//        if (rt == null) {
//            resInfo.addUnresolvableType(anno.getNameAsString());
//            return;
//        }
//
//        resInfo.addResolvableType(rt.toString());
    }

}
