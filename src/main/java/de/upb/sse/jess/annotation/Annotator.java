package de.upb.sse.jess.annotation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.*;
import de.upb.sse.jess.resolution.RobustAncestorResolver;
import de.upb.sse.jess.resolution.RobustResolver;
import de.upb.sse.jess.util.MatchingUtil;

import java.util.*;

public class Annotator {
    public static final String KEEP_ANNOTATION = "Keep";
    public static final String KEEP_ALL_ANNOTATION = "KeepAll";
    public static final String TARGET_METHOD_ANNOTATION = "TargetMethod";
    private final Map<String, CompilationUnit> annotatedUnits = new HashMap<>();

    public Map<String, CompilationUnit> getAnnotatedUnits() {
        return annotatedUnits;
    }

    public void keep(CompilationUnit cu) {
        Optional<TypeDeclaration> typeDecOpt = cu.findFirst(TypeDeclaration.class);
        if (typeDecOpt.isEmpty()) return;
        TypeDeclaration typeDec = typeDecOpt.get();
        annotate(typeDec);
    }

    public void keep(ResolvedType rt) {
        if (!rt.isReferenceType()) return;
        keep(rt.asReferenceType().getTypeDeclaration().get());
    }

    public void keep(ResolvedTypeDeclaration rtd) {
        if (rtd.isReferenceType()) {
            keep(rtd.asReferenceType());
        }
    }

    public void keep(ResolvedReferenceTypeDeclaration rrtd) {
        TypeDeclaration<?> td = null;

        if (rrtd instanceof JavaParserInterfaceDeclaration) {
            JavaParserInterfaceDeclaration jpid = (JavaParserInterfaceDeclaration) rrtd;
            td = jpid.getWrappedNode();
        } else if (rrtd instanceof JavaParserClassDeclaration) {
            JavaParserClassDeclaration jpcd = (JavaParserClassDeclaration) rrtd;

            // handle functional interfaces
            annotateFunctionalInterfaces(jpcd);

            td = jpcd.getWrappedNode();
        } else if (rrtd instanceof JavaParserEnumDeclaration) {
            JavaParserEnumDeclaration jped = (JavaParserEnumDeclaration) rrtd;
            td = jped.getWrappedNode();
        } else if (rrtd instanceof JavaParserAnnotationDeclaration) {
            JavaParserAnnotationDeclaration jpad = (JavaParserAnnotationDeclaration) rrtd;
            Optional<AnnotationDeclaration> annoOpt = jpad.toAst(AnnotationDeclaration.class);
            if (annoOpt.isEmpty()) return;

            td = annoOpt.get();
        }
        keep(td);
    }

    public void keep(TypeDeclaration<?> td) {
        if (td == null) return;

        annotate(td);
        Optional<CompilationUnit> cuOpt = td.findCompilationUnit();
        if (cuOpt.isEmpty()) return;

        CompilationUnit cu = cuOpt.get();
        annotatedUnits.put((String) cu.findFirst(TypeDeclaration.class).get().getFullyQualifiedName().get(), cu);

        // handling inner classes
        ClassOrInterfaceDeclaration outerClass = getOuterClass(td);
        if (outerClass == null) return;

        keep(outerClass);
    }

    public void keep(ResolvedMethodDeclaration rmd) {
        if (!(rmd instanceof JavaParserMethodDeclaration)) return;
        JavaParserMethodDeclaration jmd = (JavaParserMethodDeclaration) rmd;
        annotate(jmd.getWrappedNode());
        ResolvedReferenceTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(jmd);
        if (typeDec == null) return;
        keep(typeDec);
    }

    public void keepAll(ResolvedMethodDeclaration rmd) {
        if (!(rmd instanceof JavaParserMethodDeclaration)) return;
        JavaParserMethodDeclaration jmd = (JavaParserMethodDeclaration) rmd;
        annotate(jmd.getWrappedNode(), true);
        ResolvedReferenceTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(jmd);
        if (typeDec == null) return;
        keep(typeDec);
    }

    public void keep(ResolvedConstructorDeclaration rcd) {
        if (!(rcd instanceof JavaParserConstructorDeclaration)) return;
        JavaParserConstructorDeclaration<?> jcd = (JavaParserConstructorDeclaration<?>) rcd;
        annotate(jcd.getWrappedNode());
        ResolvedReferenceTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(jcd);
        if (typeDec == null) return;
        keep(typeDec);
    }

    public void keepAll(ResolvedConstructorDeclaration rcd) {
        if (!(rcd instanceof JavaParserConstructorDeclaration)) return;
        JavaParserConstructorDeclaration<?> jcd = (JavaParserConstructorDeclaration<?>) rcd;
        annotate(jcd.getWrappedNode(), true);
        ResolvedReferenceTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(jcd);
        if (typeDec == null) return;
        keep(typeDec);
    }

    public void keep(ResolvedFieldDeclaration rfd) {
        if (rfd instanceof JavaParserFieldDeclaration) {
            JavaParserFieldDeclaration jfd = (JavaParserFieldDeclaration) rfd;
            annotate(jfd.getWrappedNode());
            ResolvedTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(jfd);
            if (typeDec == null) return;
            keep(typeDec);
        } else {
            // TODO: exclude reflective fields
            Optional<Node> fieldDecOpt = rfd.toAst();
            if (fieldDecOpt.isEmpty()) return;

            Node node = fieldDecOpt.get();
            if (!(node instanceof FieldDeclaration)) return;
            FieldDeclaration fieldDec = (FieldDeclaration) node;
            annotate(fieldDec);
            ResolvedTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(rfd);
            if (typeDec == null) return;
            keep(typeDec);
        }
    }

    public void keepAll(ResolvedFieldDeclaration rfd) {
        if (rfd instanceof JavaParserFieldDeclaration) {
            JavaParserFieldDeclaration jfd = (JavaParserFieldDeclaration) rfd;
            annotate(jfd.getWrappedNode(), true);
            ResolvedTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(jfd);
            if (typeDec == null) return;
            keep(typeDec);
        } else {
            // TODO: exclude reflective fields
            Optional<Node> fieldDecOpt = rfd.toAst();
            if (fieldDecOpt.isEmpty()) return;

            Node node = fieldDecOpt.get();
            if (!(node instanceof FieldDeclaration)) return;
            FieldDeclaration fieldDec = (FieldDeclaration) node;
            annotate(fieldDec, true);
            ResolvedTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(rfd);
            if (typeDec == null) return;
            keep(typeDec);
        }
    }

    public void keep(ResolvedValueDeclaration rvd) {
        if (rvd instanceof JavaParserFieldDeclaration) {
            JavaParserFieldDeclaration jfd = (JavaParserFieldDeclaration) rvd;
            annotate(jfd.getWrappedNode());
            ResolvedTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(jfd);
            if (typeDec == null) return;
            keep(typeDec);
        } else if (rvd instanceof JavaParserEnumConstantDeclaration) {
            JavaParserEnumConstantDeclaration jecd = (JavaParserEnumConstantDeclaration) rvd;
            annotate(jecd.getWrappedNode());
            keep(jecd.getType());
        }
    }

    public void keepAll(ResolvedValueDeclaration rvd) {
        if (rvd instanceof JavaParserFieldDeclaration) {
            JavaParserFieldDeclaration jfd = (JavaParserFieldDeclaration) rvd;
            annotate(jfd.getWrappedNode(), true);
            ResolvedTypeDeclaration typeDec = RobustResolver.tryGetDeclaringType(jfd);
            if (typeDec == null) return;
            keep(typeDec);
        } else if (rvd instanceof JavaParserEnumConstantDeclaration) {
            JavaParserEnumConstantDeclaration jecd = (JavaParserEnumConstantDeclaration) rvd;
            annotate(jecd.getWrappedNode(), true);
            keep(jecd.getType());
        }
    }

    public void keep(InitializerDeclaration id) {
        annotate(id);
        Optional<TypeDeclaration> typeDecOpt = id.findAncestor(TypeDeclaration.class);
        if (typeDecOpt.isEmpty()) return;
        keep(typeDecOpt.get());
    }

    public void keepAll(InitializerDeclaration id) {
        annotate(id, true);
        Optional<TypeDeclaration> typeDecOpt = id.findAncestor(TypeDeclaration.class);
        if (typeDecOpt.isEmpty()) return;
        keep(typeDecOpt.get());
    }

    public void setTargetMethod(NodeWithAnnotations<?> n) {
        n.addMarkerAnnotation(TARGET_METHOD_ANNOTATION);
    }

    private ClassOrInterfaceDeclaration getOuterClass(TypeDeclaration<?> td) {
        Optional<Node> parentNodeOpt = td.getParentNode();
        if (parentNodeOpt.isEmpty()) return null;

        Node parentNode = parentNodeOpt.get();
        if (!(parentNode instanceof ClassOrInterfaceDeclaration)) return null;

        return (ClassOrInterfaceDeclaration) parentNode;
    }

    private void annotate(NodeWithAnnotations<?> node) {
        annotate(node, false);
    }

    private void annotate(NodeWithAnnotations<?> node, boolean keepAll) {
        if(node.isAnnotationPresent(KEEP_ALL_ANNOTATION)) return;

        if (keepAll) {
            Optional<AnnotationExpr> keepAnnotationOpt = node.getAnnotationByName(KEEP_ANNOTATION);
            if (keepAnnotationOpt.isEmpty()) {
                node.addMarkerAnnotation(KEEP_ALL_ANNOTATION);
            } else {
                AnnotationExpr keepAnnotation = keepAnnotationOpt.get();
                keepAnnotation.setName(KEEP_ALL_ANNOTATION);
            }
        } else {
            if (node.isAnnotationPresent(KEEP_ANNOTATION)) return;

            node.addMarkerAnnotation(KEEP_ANNOTATION);
        }
    }

    private void annotateFunctionalInterfaces(JavaParserClassDeclaration jpcd) {
        // Annotate functional interface methods
        List<ResolvedReferenceType> interfaces = RobustAncestorResolver.tryResolveInterfaces(jpcd);
        for (ResolvedReferenceType inter : interfaces) {
            Optional<ResolvedReferenceTypeDeclaration> interDeclarationOpt = inter.getTypeDeclaration();
            if (interDeclarationOpt.isEmpty()) continue;

            ResolvedReferenceTypeDeclaration interDeclaration = interDeclarationOpt.get();
            if (!RobustResolver.tryIsFunctionalInterface(interDeclaration)) continue;

            interDeclaration.getDeclaredMethods().forEach(fm -> {
                if (!fm.isAbstract()) return;
                if (!(fm instanceof JavaParserMethodDeclaration)) return;
                JavaParserMethodDeclaration jpmd = (JavaParserMethodDeclaration) fm;
                annotate(jpmd.getWrappedNode());

                Set<ResolvedMethodDeclaration> declaredMethods = jpcd.getDeclaredMethods();
                for (ResolvedMethodDeclaration dm : declaredMethods) {
                    if (!MatchingUtil.trySignatureMatching(dm, fm)) continue;
                    if (!(dm instanceof JavaParserMethodDeclaration)) return;
                    JavaParserMethodDeclaration jpmd2 = (JavaParserMethodDeclaration) dm;
                    annotate(jpmd2.getWrappedNode());
                }
            });
        }
    }
}
