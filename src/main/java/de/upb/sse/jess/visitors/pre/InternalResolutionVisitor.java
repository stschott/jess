package de.upb.sse.jess.visitors.pre;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserConstructorDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;;
import de.upb.sse.jess.annotation.Annotator;
import de.upb.sse.jess.resolution.RobustResolver;
import de.upb.sse.jess.util.SignatureUtil;

import java.util.*;
import java.util.stream.Collectors;

public class InternalResolutionVisitor extends VoidVisitorAdapter<Void> {
    private final List<String> methodsToKeep;
    private final List<String> keepClinit;
    private final List<String> keepInit;
    private final Annotator annotator;
    private final boolean looseMatching;

    public InternalResolutionVisitor(Annotator ann, List<String> methodsToKeep) {
        this(ann, methodsToKeep, Collections.emptyList(), Collections.emptyList(), true);
    }

    public InternalResolutionVisitor(Annotator ann, List<String> methodsToKeep, List<String> keepClinit, List<String> keepInit) {
        this(ann, methodsToKeep, keepClinit, keepInit, true);
    }

    public InternalResolutionVisitor(Annotator ann, List<String> methodsToKeep, List<String> keepClinit, List<String> keepInit, boolean looseMatching) {
        this.annotator = ann;
        this.methodsToKeep = methodsToKeep;
        this.keepClinit = keepClinit;
        this.keepInit = keepInit;
        this.looseMatching = looseMatching;
    }

    @Override
    public void visit(ClassOrInterfaceType cit, Void arg) {
        super.visit(cit, arg);
        if (!isWithinKeptMethod(cit)) return;

        ResolvedType rt = RobustResolver.tryResolve(cit);

        if (rt == null) return;
        if (rt.isTypeVariable()) return;

        annotator.keep(rt);
    }

    @Override
    public void visit(MethodCallExpr mce, Void arg) {
        super.visit(mce, arg);
        if (!isWithinKeptMethod(mce)) return;

        ResolvedMethodDeclaration rmd = RobustResolver.tryResolve(mce);
        if (!(rmd instanceof JavaParserMethodDeclaration)) return;

        annotator.keep(rmd);
    }

    @Override
    public void visit(FieldAccessExpr fae, Void arg) {
        super.visit(fae, arg);
        if (!isWithinKeptMethod(fae)) return;

        ResolvedValueDeclaration rvd = RobustResolver.tryResolve(fae);
        if (!(rvd instanceof JavaParserFieldDeclaration)) return;

        annotator.keep(rvd);
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt ecis, Void arg) {
        super.visit(ecis, arg);
        if (!isWithinKeptMethod(ecis)) return;
        if (!ecis.isThis()) return;

        ResolvedConstructorDeclaration rcd = RobustResolver.tryResolve(ecis);
        if (!(rcd instanceof JavaParserConstructorDeclaration)) return;

        annotator.keep(rcd);
    }

    @Override
    public void visit(NameExpr ne, Void arg) {
        super.visit(ne, arg);
        if (!isWithinKeptMethod(ne)) return;

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
    public void visit(ObjectCreationExpr oce, Void arg) {
        super.visit(oce, arg);
        if (!isWithinKeptMethod(oce)) return;

        ResolvedConstructorDeclaration rcd = RobustResolver.tryResolve(oce);

         if (rcd instanceof JavaParserConstructorDeclaration) {
            annotator.keep(rcd);
        }
    }


    @Override
    public void visit(MethodReferenceExpr mre, Void arg) {
        super.visit(mre, arg);
        if (!isWithinKeptMethod(mre)) return;

        Expression scope = mre.getScope();

        // check if scope references a field
        List<FieldDeclaration> fieldDeclarations = mre.findRootNode().findAll(FieldDeclaration.class);
        for (FieldDeclaration fd : fieldDeclarations) {
            NodeList<VariableDeclarator> variables = fd.getVariables();

            for (VariableDeclarator vd : variables) {
                if (!vd.getNameAsString().equals(scope.toString())) continue;
                ResolvedValueDeclaration rvd = RobustResolver.tryResolve(fd);
                if (rvd == null) continue;

                annotator.keep(rvd);
            }
        }

        ResolvedMethodDeclaration rmd = RobustResolver.tryResolve(mre);
        if (rmd instanceof JavaParserMethodDeclaration) {
            annotator.keep(rmd);
        } else if (rmd == null) {
            if (!isWithinSameCompilationUnit(mre, scope)) return;

            ResolvedType rt = RobustResolver.tryResolve(scope);

            if (rt == null) return;
            if (!rt.isReferenceType()) return;

            ResolvedReferenceType refType = rt.asReferenceType();
            Optional<ResolvedReferenceTypeDeclaration> refTypeDecOpt = refType.getTypeDeclaration();
            if (refTypeDecOpt.isEmpty()) return;

            ResolvedReferenceTypeDeclaration refTypeDec = refTypeDecOpt.get();
            Set<ResolvedMethodDeclaration> declaredMethods = refTypeDec.getDeclaredMethods();
            String mreIdentifier = mre.getIdentifier();

            for (ResolvedMethodDeclaration declaredMethod : declaredMethods) {
                if (!declaredMethod.getName().equals(mreIdentifier)) continue;

                annotator.keep(declaredMethod);
            }
        }
    }


    @Override
    public void visit(MethodDeclaration md, Void arg) {
        super.visit(md, arg);
        if (!isWithinKeptMethod(md)) return;

        ResolvedMethodDeclaration rmd = RobustResolver.tryResolve(md);
        if (rmd == null) return;

        annotator.keepAll(rmd);
    }

    @Override
    public void visit(ConstructorDeclaration cd, Void arg) {
        super.visit(cd, arg);
        if (!isWithinKeptMethod(cd)) return;

        ResolvedConstructorDeclaration rcd = RobustResolver.tryResolve(cd);
        if (rcd == null) return;
        annotator.keepAll(rcd);

        // keep non static fields and initializers
        Optional<ClassOrInterfaceDeclaration> classDecOpt = cd.findAncestor(ClassOrInterfaceDeclaration.class);
        if (classDecOpt.isEmpty()) return;
        ClassOrInterfaceDeclaration classDec = classDecOpt.get();

        keepNonStaticFields(classDec);
        keepNonStaticInitializers(classDec);

        // nothing more to do if we keep the default constructor
        if (rcd.getNumberOfParams() == 0) {
            // TODO: need to check for this calls in the constructors and annotate them with "KeepAll"


            return;
        }

        // if a constructor needs to be compiled, the code of the default constructor is inlined in the bytecode
        // therefore the default constructor's code needs to be kept as well
        List<ConstructorDeclaration> constructorDeclarations = classDec.getConstructors();

        Optional<ConstructorDeclaration> cDecOpt = constructorDeclarations.stream()
                .filter(cDec -> cDec.getParameters().isEmpty())
                .findAny();
        if (cDecOpt.isEmpty()) return;
        ConstructorDeclaration defaultConstructor = cDecOpt.get();
        ResolvedConstructorDeclaration rDefaultConstructor = RobustResolver.tryResolve(defaultConstructor);
        if (rDefaultConstructor == null) return;
        annotator.keepAll(rDefaultConstructor);
    }

    @Override
    public void visit(FieldDeclaration fd, Void arg) {
        super.visit(fd, arg);
        if (fd.isStatic() && !isKeptClinit(fd)) return;
        if (!fd.isStatic() && !isKeptInit(fd)) return;

        // if there are changes to the <clinit> method, keep all static fields
        ResolvedValueDeclaration rfd = RobustResolver.tryResolve(fd);
        if (rfd == null) return;

        annotator.keepAll(rfd);

    }

    @Override
    public void visit(InitializerDeclaration id, Void arg) {
        super.visit(id, arg);
        if (id.isStatic() && !isKeptClinit(id)) return;
        if (!id.isStatic() && !isKeptInit(id)) return;

        // if there are changes to the <clinit> method, keep all static initializers
        annotator.keepAll(id);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean isWithinKeptMethod(Node node) {
        if (isKeptClinit(node)) return isWithinClinit(node);
        if (isKeptInit(node)) return isWithinInit(node);
        if (isKeptMethod(node)) return true;
        if (!looseMatching) return isWithinKeptMethodBasedOnPartialSignature(node);

        Optional<CallableDeclaration> declaredMethodOpt = node.findAncestor(CallableDeclaration.class);

        while(declaredMethodOpt.isPresent()) {
            CallableDeclaration declaredMethod = declaredMethodOpt.get();
            if (methodsToKeep.contains(declaredMethod.getSignature().asString())) return true;
            declaredMethodOpt = declaredMethod.findAncestor(CallableDeclaration.class);
        }

        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean isWithinKeptMethodBasedOnPartialSignature(Node node) {
        Optional<CallableDeclaration> declaredMethodOpt = node.findAncestor(CallableDeclaration.class);

        while(declaredMethodOpt.isPresent()) {
            CallableDeclaration declaredMethod = declaredMethodOpt.get();
            if (methodsToKeep.contains(SignatureUtil.getPartialCallableSignature(declaredMethod))) return true;
            declaredMethodOpt = declaredMethod.findAncestor(CallableDeclaration.class);
        }

        return false;
    }

    private boolean isKeptMethod(Node node) {
        if (!(node instanceof CallableDeclaration)) return false;
        CallableDeclaration<?> cd = (CallableDeclaration<?>) node;
        if (!looseMatching) return methodsToKeep.contains(SignatureUtil.getPartialCallableSignature(cd));
        if (!methodsToKeep.contains(cd.getSignature().asString())) return false;

        annotator.setTargetMethod(cd);
        return true;
    }

    private boolean isKeptClinit(Node node) {
        String classSignature = SignatureUtil.getClassSignature(node);
        return keepClinit.contains(classSignature);
    }

    private boolean isKeptInit(Node node) {
        String classSignature = SignatureUtil.getClassSignature(node);
        return keepInit.contains(classSignature);
    }

    private boolean isWithinSameCompilationUnit(Node node1, Node node2) {
        Optional<CompilationUnit> cu1Opt = node1.findCompilationUnit();
        Optional<CompilationUnit> cu2Opt = node2.findCompilationUnit();

        if (cu1Opt.isEmpty() || cu2Opt.isEmpty()) return false;
        CompilationUnit cu1 = cu1Opt.get();
        CompilationUnit cu2 = cu2Opt.get();

        return cu1.equals(cu2);
    }

    private boolean isWithinClinit(Node node) {
        Optional<FieldDeclaration> fieldDeclarationOpt = node.findAncestor(FieldDeclaration.class);
        if (fieldDeclarationOpt.isPresent()) {
            FieldDeclaration fieldDeclaration = fieldDeclarationOpt.get();
            if (fieldDeclaration.isStatic()) return true;
        }

        Optional<InitializerDeclaration> initializerDeclarationOpt = node.findAncestor(InitializerDeclaration.class);
        if (initializerDeclarationOpt.isPresent()) {
            InitializerDeclaration initializerDeclaration = initializerDeclarationOpt.get();
            if (initializerDeclaration.isStatic()) return true;
        }

        return false;
    }

    private boolean isWithinInit(Node node) {
        Optional<FieldDeclaration> fieldDeclarationOpt = node.findAncestor(FieldDeclaration.class);
        if (fieldDeclarationOpt.isPresent()) {
            FieldDeclaration fieldDeclaration = fieldDeclarationOpt.get();
            if (!fieldDeclaration.isStatic()) return true;
        }

        Optional<InitializerDeclaration> initializerDeclarationOpt = node.findAncestor(InitializerDeclaration.class);
        if (initializerDeclarationOpt.isPresent()) {
            InitializerDeclaration initializerDeclaration = initializerDeclarationOpt.get();
            if (!initializerDeclaration.isStatic()) return true;
        }

        return false;
    }

    private void keepStaticFields(ClassOrInterfaceDeclaration cid) {
        List<FieldDeclaration> fieldDeclarations = cid.getFields();
        List<FieldDeclaration> staticFieldDeclarations = fieldDeclarations.stream()
                .filter(NodeWithStaticModifier::isStatic)
                .collect(Collectors.toList());

        keepFields(staticFieldDeclarations);
    }

    private void keepNonStaticFields(ClassOrInterfaceDeclaration cid) {
        List<FieldDeclaration> fieldDeclarations = cid.getFields();
        List<FieldDeclaration> nonStaticFieldDeclarations = fieldDeclarations.stream()
                .filter(fd -> !fd.isStatic())
                .collect(Collectors.toList());

        keepFields(nonStaticFieldDeclarations);
    }

    private void keepFields(List<FieldDeclaration> fields) {
        for (FieldDeclaration fd : fields) {
            ResolvedValueDeclaration rvd = RobustResolver.tryResolve(fd);
            if (rvd == null) continue;
            annotator.keepAll(rvd);
        }
    }

    private void keepStaticInitializers(ClassOrInterfaceDeclaration cid) {
        List<Node> childNodes = cid.getChildNodes();
        List<InitializerDeclaration> initializerDeclarations = childNodes.stream()
                .filter(n -> n instanceof InitializerDeclaration)
                .map(n -> (InitializerDeclaration) n)
                .collect(Collectors.toList());

        List<InitializerDeclaration> staticInitializerDeclarations = initializerDeclarations.stream()
                .filter(InitializerDeclaration::isStatic)
                .collect(Collectors.toList());

        keepInitializers(staticInitializerDeclarations);
    }

    private void keepNonStaticInitializers(ClassOrInterfaceDeclaration cid) {
        List<Node> childNodes = cid.getChildNodes();
        List<InitializerDeclaration> initializerDeclarations = childNodes.stream()
                .filter(n -> n instanceof InitializerDeclaration)
                .map(n -> (InitializerDeclaration) n)
                .collect(Collectors.toList());

        List<InitializerDeclaration> nonStaticInitializerDeclarations = initializerDeclarations.stream()
                .filter(id -> !id.isStatic())
                .collect(Collectors.toList());

        keepInitializers(nonStaticInitializerDeclarations);
    }

    private void keepInitializers(List<InitializerDeclaration> initializers) {
        for (InitializerDeclaration id : initializers) {
            annotator.keep(id);
        }
    }

}
