package de.upb.sse.jess.visitors.pre;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserEnumDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserInterfaceDeclaration;
import de.upb.sse.jess.annotation.Annotator;
import de.upb.sse.jess.resolution.RobustResolver;
import de.upb.sse.jess.resolution.ResolutionUtil;

import java.util.Optional;

public class PreSlicingVisitor extends VoidVisitorAdapter<Void> {
    private final String targetClass;
    private final Annotator ann;

    public PreSlicingVisitor(String targetClass, Annotator ann) {
        this.targetClass = targetClass;
        this.ann = ann;
    }

    @Override
    public void visit(MethodDeclaration md, Void arg) {
        super.visit(md, arg);
        Optional<TypeDeclaration> typeDecOpt = md.findAncestor(TypeDeclaration.class);
        if (typeDecOpt.isEmpty()) return;

        TypeDeclaration typeDec = typeDecOpt.get();
        if (!typeDec.isAnnotationPresent(Annotator.KEEP_ANNOTATION) && !typeDec.isAnnotationPresent(Annotator.KEEP_ALL_ANNOTATION)) return;

        if (
                ResolutionUtil.isAbstractFunctionalInterfaceMethod(md) ||
                ResolutionUtil.isOverridingAbstractSuperMethod(md, targetClass) ||
                ResolutionUtil.isOverridingAbstractInterfaceMethod(md, targetClass) ||
                ResolutionUtil.isOverridingKeptInterfaceMethod(md) ||
                ResolutionUtil.isOverridingAbstractEnumMethod(md)
        ) {

            ResolvedMethodDeclaration rmd = RobustResolver.tryResolve(md);
            if (rmd == null) return;
            ann.keep(rmd);

            checkType(md.getType());
            md.getParameters().forEach(param -> checkType(param.getType()));
        }

    }

    private void checkType(Type type) {
        if (!type.isReferenceType()) return;

        ResolvedType resolvedType = RobustResolver.tryResolve(type);
        if (resolvedType == null) return;
        if (!resolvedType.isReferenceType()) return;

        ResolvedReferenceType rrt = resolvedType.asReferenceType();
        Optional<ResolvedReferenceTypeDeclaration> declarationOpt = rrt.getTypeDeclaration();
        if (declarationOpt.isEmpty()) return;

        ResolvedReferenceTypeDeclaration declaration = declarationOpt.get();

        TypeDeclaration<?> td = null;
        if (declaration instanceof JavaParserClassDeclaration) {
            JavaParserClassDeclaration jpcd = (JavaParserClassDeclaration) declaration;
            td = jpcd.getWrappedNode();
        } else if (declaration instanceof JavaParserInterfaceDeclaration) {
            JavaParserInterfaceDeclaration jpid = (JavaParserInterfaceDeclaration) declaration;
            td = jpid.getWrappedNode();
        } else if (declaration instanceof JavaParserEnumDeclaration) {
            JavaParserEnumDeclaration jped = (JavaParserEnumDeclaration) declaration;
            td = jped.getWrappedNode();
        }

        if (td == null) return;

        ann.keep(td);
    }
}
