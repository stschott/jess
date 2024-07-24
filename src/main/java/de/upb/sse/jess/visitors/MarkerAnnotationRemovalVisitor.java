package de.upb.sse.jess.visitors;

import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import de.upb.sse.jess.annotation.Annotator;

public class MarkerAnnotationRemovalVisitor extends ModifierVisitor<Void> {

    @Override
    public MarkerAnnotationExpr visit(MarkerAnnotationExpr anno, Void arg) {
        super.visit(anno, arg);
        if (anno.getName().toString().equals(Annotator.TARGET_METHOD_ANNOTATION)) return anno;
        if (anno.getName().toString().equals(Annotator.KEEP_ALL_ANNOTATION)) return null;
        if (anno.getName().toString().equals(Annotator.KEEP_ANNOTATION)) return null;
//        if (RobustResolver.tryResolve(anno) != null) return null;
        // Keep annotations that cannot be resolved
        return null;
    }
}
