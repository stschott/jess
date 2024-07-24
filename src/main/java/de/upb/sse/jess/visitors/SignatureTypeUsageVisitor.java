package de.upb.sse.jess.visitors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserEnumDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserInterfaceDeclaration;
import de.upb.sse.jess.annotation.Annotator;
import de.upb.sse.jess.resolution.RobustResolver;

import java.util.Optional;

public class SignatureTypeUsageVisitor extends VoidVisitorAdapter<Void> {
    private final Annotator ann;
    private CompilationUnit root = null;

    public SignatureTypeUsageVisitor(Annotator ann) {
        this.ann = ann;
    }

    @Override
    public void visit(CompilationUnit cu, Void arg) {
        this.root = cu;
        super.visit(cu, arg);
    }

    @Override
    public void visit(MethodDeclaration md, Void arg) {
        super.visit(md, arg);
        if (!md.isAnnotationPresent(Annotator.KEEP_ANNOTATION)) return;

        checkType(md.getType());
        md.getParameters().forEach(param -> checkType(param.getType()));
    }

    @Override
    public void visit(ConstructorDeclaration cd, Void arg) {
        super.visit(cd, arg);
        if (!cd.isAnnotationPresent(Annotator.KEEP_ANNOTATION)) return;

        cd.getParameters().forEach(param -> checkType(param.getType()));
    }

    @Override
    public void visit(FieldDeclaration fd, Void arg) {
        super.visit(fd, arg);
        if (!fd.isAnnotationPresent(Annotator.KEEP_ANNOTATION)) return;

        fd.getVariables().forEach(v -> checkType(v.getType()));
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
        Optional<CompilationUnit> cuOpt = td.findCompilationUnit();
        if (cuOpt.isEmpty()) return;
        CompilationUnit cu = cuOpt.get();

        if (!cu.equals(this.root)) return;
        ann.keep(td);
    }

}
