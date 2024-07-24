package de.upb.sse.jess.visitors.pre;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import de.upb.sse.jess.annotation.Annotator;
import de.upb.sse.jess.resolution.RobustResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InternalKeptTypeResolutionVisitor extends VoidVisitorAdapter<Void> {
    private final Annotator annotator;

    public InternalKeptTypeResolutionVisitor(Annotator ann) {
        this.annotator = ann;
    }

    @Override
    public void visit(ClassOrInterfaceType cit, Void arg) {
        super.visit(cit, arg);
        if (!isInKeptDeclaration(cit)) return;

        ResolvedType rt = RobustResolver.tryResolve(cit);
        if (rt == null) return;
        annotator.keep(rt);

        if (!rt.isReferenceType()) return;
        ResolvedReferenceType rrt = rt.asReferenceType();
        Optional<ResolvedReferenceTypeDeclaration> rrtdOpt = rrt.getTypeDeclaration();

        if (rrtdOpt.isEmpty()) return;
        ResolvedReferenceTypeDeclaration rrtd = rrtdOpt.get();
        Optional<ClassOrInterfaceDeclaration> cidOpt = rrtd.toAst(ClassOrInterfaceDeclaration.class);

        if (cidOpt.isEmpty()) return;
        ClassOrInterfaceDeclaration cid = cidOpt.get();

        annotateImplementedAndExtendedTypes(cid);
    }

    @Override
    public void visit(TypeParameter tp, Void arg) {
        super.visit(tp, arg);

    }

    @Override
    public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
        super.visit(cid, arg);
        if (!cid.isAnnotationPresent(Annotator.KEEP_ANNOTATION) && !cid.isAnnotationPresent(Annotator.KEEP_ALL_ANNOTATION)) return;

        annotateImplementedAndExtendedTypeParameters(cid);
    }

    @Override
    public void visit(NameExpr ne, Void arg) {
        super.visit(ne, arg);
        if (!isWithinKeptField(ne)) return;

        ResolvedValueDeclaration rvd = RobustResolver.tryResolve(ne);
        if (rvd == null) return;

        annotator.keepAll(rvd);
    }

    private void annotateImplementedAndExtendedTypeParameters(ClassOrInterfaceDeclaration cid) {
        List<Type> types = new ArrayList<>();
        // collect type parameters of extended classes
        for (ClassOrInterfaceType cit : cid.getExtendedTypes()) {
            Optional<NodeList<Type>> typeParametersOpt = cit.getTypeArguments();
            if (typeParametersOpt.isEmpty()) continue;
            types.addAll(typeParametersOpt.get());
        }

        // collect type parameters of implemented classes
        for (ClassOrInterfaceType cit : cid.getImplementedTypes()) {
            Optional<NodeList<Type>> typeParametersOpt = cit.getTypeArguments();
            if (typeParametersOpt.isEmpty()) continue;
            types.addAll(typeParametersOpt.get());
        }

        for (Type type : types) {
            ResolvedType rt = RobustResolver.tryResolve(type);
            if (rt == null) continue;
            annotator.keep(rt);
        }
    }

    private void annotateImplementedAndExtendedTypes(ClassOrInterfaceDeclaration cid) {
        List<ClassOrInterfaceType> types = new ArrayList<>();
        // collect type parameters of extended classes
        types.addAll(cid.getExtendedTypes());
        types.addAll(cid.getImplementedTypes());

        for (ClassOrInterfaceType type : types) {
            ResolvedType rt = RobustResolver.tryResolve(type);
            if (rt == null) continue;
            annotator.keep(rt);
        }
    }

    private boolean isInKeptDeclaration(ClassOrInterfaceType cit) {
        Optional<FieldDeclaration> fieldDeclarationOpt = cit.findAncestor(FieldDeclaration.class);
        Optional<MethodDeclaration> methodDeclarationOpt = cit.findAncestor(MethodDeclaration.class);
        Optional<ConstructorDeclaration> constructorDeclarationOpt = cit.findAncestor(ConstructorDeclaration.class);
        Optional<ClassOrInterfaceDeclaration> ciDeclarationOpt = cit.findAncestor(ClassOrInterfaceDeclaration.class);

        NodeWithAnnotations<?> node = null;
        if (fieldDeclarationOpt.isPresent()) {
            node = fieldDeclarationOpt.get();
        } else if (methodDeclarationOpt.isPresent()) {
            node = methodDeclarationOpt.get();
        } else if (constructorDeclarationOpt.isPresent()) {
            node = constructorDeclarationOpt.get();
        } else if (ciDeclarationOpt.isPresent() && ciDeclarationOpt.get().isNestedType()) {
            node = ciDeclarationOpt.get();
        }

        if (node == null) return false;

        if (node.isAnnotationPresent(Annotator.KEEP_ALL_ANNOTATION)) return true;
        return node.isAnnotationPresent(Annotator.KEEP_ANNOTATION) && isInSignature(cit) ;
    }

    private boolean isInSignature(ClassOrInterfaceType cit) {
        Optional<BlockStmt> blockStmt = cit.findAncestor(BlockStmt.class);
        return blockStmt.isEmpty();
    }


    private boolean isWithinKeptField(Node node) {
        Optional<FieldDeclaration> fieldDeclarationOpt = node.findAncestor(FieldDeclaration.class);
        if (fieldDeclarationOpt.isPresent()) {
            FieldDeclaration fieldDeclaration = fieldDeclarationOpt.get();
            if (fieldDeclaration.isAnnotationPresent(Annotator.KEEP_ALL_ANNOTATION)) return true;
            if (fieldDeclaration.isAnnotationPresent(Annotator.KEEP_ANNOTATION)) return true;
        }
        return false;
    }
}
