package de.upb.sse.jess.visitors.slicing;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import de.upb.sse.jess.annotation.Annotator;
import de.upb.sse.jess.resolution.RobustResolver;
import de.upb.sse.jess.util.SlicingUtil;
import org.checkerframework.checker.units.qual.N;

import java.util.*;
import java.util.stream.Collectors;

public class SlicingVisitor extends ModifierVisitor<Void> {
    private final String targetClass;
    private final boolean isPreSlicing;

    public SlicingVisitor(String targetClass) {
        this(targetClass, false);
    }

    public SlicingVisitor(String targetClass, boolean isPreSlicing) {
        this.targetClass = targetClass;
        this.isPreSlicing = isPreSlicing;
    }

    @Override
    public ClassOrInterfaceDeclaration visit(ClassOrInterfaceDeclaration cid, Void arg) {
        super.visit(cid, arg);

        // aggressive slicing
//        if (!isPreSlicing && cid.isAnnotationPresent(Annotator.KEEP_ANNOTATION)) {
//            NodeList<ClassOrInterfaceType> implementedTypes = new NodeList<>(cid.getImplementedTypes());
//            for (ClassOrInterfaceType implementedType : implementedTypes) {
//                if (res.tryResolve(implementedType) != null) continue;
//                cid.getImplementedTypes().remove(implementedType);
//            }
//
//            NodeList<ClassOrInterfaceType> extendedTypes = new NodeList<>(cid.getExtendedTypes());
//            for (ClassOrInterfaceType extendedType : extendedTypes) {
//                if (res.tryResolve(extendedType) != null) continue;
//                cid.getExtendedTypes().remove(extendedType);
//            }
//        }

        if (cid.isAnnotationPresent(Annotator.KEEP_ANNOTATION)) {
            if (cid.isInterface()) return cid;

            List<ConstructorDeclaration> constructors = cid.getConstructors();
            Optional<ConstructorDeclaration> defaultConstructorOpt = constructors.stream().filter(cd -> cd.getParameters().size() == 0).findAny();

            ConstructorDeclaration defaultConstructor = defaultConstructorOpt.isPresent() ? defaultConstructorOpt.get() : cid.addConstructor(Modifier.Keyword.PUBLIC);
            if (defaultConstructor.isAnnotationPresent(Annotator.KEEP_ALL_ANNOTATION)) {
                // TODO: create another constructor if the default constructor is designated as target
//                defaultConstructor = cid.addConstructor(Modifier.Keyword.PUBLIC);
//                defaultConstructor.addParameter("int", "arg1");
                return cid;
            }

            defaultConstructor.setBody(new BlockStmt());
            addSuperCall(defaultConstructor);
            addFieldInitializations(defaultConstructor, cid);
//            NodeList<Statement> defaultStatements = defaultConstructor.getBody().getStatements();
//            defaultStatements.addAll(getFieldInitializingStatements(cid));
//            defaultConstructor.getBody().setStatements(defaultStatements);

            return cid;
        }

        return null;
    }

    @Override
    public EnumDeclaration visit(EnumDeclaration ed, Void arg) {
        super.visit(ed, arg);

        if (ed.isAnnotationPresent(Annotator.KEEP_ANNOTATION)) {
            List<ConstructorDeclaration> constructors = ed.getConstructors();
            Optional<ConstructorDeclaration> defaultConstructorOpt = constructors.stream().filter(cd -> cd.getParameters().size() == 0).findAny();

            ConstructorDeclaration defaultConstructor = defaultConstructorOpt.isPresent() ? defaultConstructorOpt.get() : ed.addConstructor();
            if (defaultConstructor.isAnnotationPresent(Annotator.KEEP_ALL_ANNOTATION)) return ed;

            addSuperCall(defaultConstructor);
            NodeList<Statement> defaultStatements = defaultConstructor.getBody().getStatements();
            defaultStatements.addAll(getFieldInitializingStatements(ed));
            defaultConstructor.getBody().setStatements(defaultStatements);

            return ed;
        }

        return null;
    }

    @Override
    public MethodDeclaration visit(MethodDeclaration md, Void arg) {
        super.visit(md, arg);

        if (md.isAnnotationPresent(Annotator.KEEP_ALL_ANNOTATION)) return md;

//        if (ResolutionUtil.isAbstractFunctionalInterfaceMethod(md)) return SlicingUtil.emptyMethod(md);
//        if (ResolutionUtil.isOverridingAbstractSuperMethod(md, targetClass)) return SlicingUtil.emptyMethod(md);
//        if (ResolutionUtil.isOverridingAbstractInterfaceMethod(md, targetClass)) return SlicingUtil.emptyMethod(md);
//        if (ResolutionUtil.isOverridingKeptInterfaceMethod(md)) return SlicingUtil.emptyMethod(md);
//        if (ResolutionUtil.isOverridingAbstractEnumMethod(md)) return SlicingUtil.emptyMethod(md);

        if (md.isAnnotationPresent(Annotator.KEEP_ANNOTATION)) return SlicingUtil.emptyMethod(md);

        return null;
    }

    @Override
    public ConstructorDeclaration visit(ConstructorDeclaration cd, Void arg) {
        super.visit(cd, arg);

        if (cd.isAnnotationPresent(Annotator.KEEP_ALL_ANNOTATION)) {
            fixInitializedFinalFields(cd);
            return cd;
        }
        if (cd.getParameters().size() == 0) return cd;

        if (cd.isAnnotationPresent(Annotator.KEEP_ANNOTATION)) {
//            sliceConstructor(cd);
            ExplicitConstructorInvocationStmt ecis = new ExplicitConstructorInvocationStmt(true, null, new NodeList<>());
            NodeList<Statement> statements = new NodeList<>();
            statements.add(ecis);
            cd.getBody().setStatements(statements);
            return cd;
        }

        return null;
    }

    @Override
    public FieldDeclaration visit(FieldDeclaration fd, Void arg) {
        super.visit(fd, arg);
//        if (fd.isFinal()) fd.setFinal(false);
        if (fd.isAnnotationPresent(Annotator.KEEP_ALL_ANNOTATION)) return fd;
        if (!fd.isAnnotationPresent(Annotator.KEEP_ANNOTATION)) return null;

//        NodeList<VariableDeclarator> variables = fd.getVariables();
//        variables.forEach(v -> {
//            // if the initializer is a literal, keep it
//            // otherwise we may have duplicate case literals, if we reset two fields to the same value
//            Optional<Expression> initializerOpt = v.getInitializer();
//            if (initializerOpt.isPresent()) {
//                Expression initializer = initializerOpt.get();
//                if (allOperatorsLiterals(initializer)) return;
//            } else {
//                if (fd.isFinal()) {
//                    // field is final and needs some kind of initialization
//
//                    return;
//                } else {
//                    // field not final and not initialized
//                    return;
//                }
//            }
//
//            v.setInitializer(SlicingUtil.getGenericReturnExpression(v.getTypeAsString()));
//        });
//        if (variables.size() < 1) return null;
//        fd.setVariables(variables);

        return fd;
    }

    @Override
    public EnumConstantDeclaration visit(EnumConstantDeclaration ecd, Void arg) {
        super.visit(ecd, arg);
        ecd.setArguments(new NodeList<>());
        return ecd;
    }

    @Override
    public InitializerDeclaration visit(InitializerDeclaration id, Void arg) {
        super.visit(id, arg);
        if (id.isAnnotationPresent(Annotator.KEEP_ANNOTATION)) return id;
        if (id.isAnnotationPresent(Annotator.KEEP_ALL_ANNOTATION)) return id;

        return null;
    }

    @Override
    public SingleMemberAnnotationExpr visit(SingleMemberAnnotationExpr smae, Void arg) {
        super.visit(smae, arg);
        return null;
    }

    @Override
    public SingleMemberAnnotationExpr visit(NormalAnnotationExpr nae, Void arg) {
        super.visit(nae, arg);
        return null;
    }

    @Override
    public BlockComment visit(BlockComment comm, Void arg) {
        super.visit(comm, arg);
        return null;
    }

    @Override
    public JavadocComment visit(JavadocComment comm, Void arg) {
        super.visit(comm, arg);
        return null;
    }

    @Override
    public LineComment visit(LineComment comm, Void arg) {
        super.visit(comm, arg);
        return null;
    }

    private void sliceConstructor(ConstructorDeclaration cd) {
        NodeList<Statement> statements = cd.getBody().getStatements();
        List<Statement> filteredStatementList = statements.stream().filter(st -> {
            if (!st.isExplicitConstructorInvocationStmt()) return false;
            return !st.asExplicitConstructorInvocationStmt().isThis();
        }).collect(Collectors.toList());

        if (filteredStatementList.size() < 1) {
            cd.setBody(cd.getBody().setStatements(new NodeList<>()));
            return;
        }

        ResolvedConstructorDeclaration shortestConstructor = getShortestSuperConstructor(cd, this.isPreSlicing);
        if (shortestConstructor == null) {
            cd.setBody(cd.getBody().setStatements(new NodeList<>()));
            return;
        }

        filteredStatementList.forEach(st -> {
            ExplicitConstructorInvocationStmt cist = (ExplicitConstructorInvocationStmt) st;
            NodeList<Expression> arguments = new NodeList<>();
            for (int i = 0; i < shortestConstructor.getNumberOfParams(); i++) {
                arguments.add(SlicingUtil.getGenericReturnExpression(shortestConstructor.getParam(i).describeType()));
            }
            cist.setArguments(arguments);
        });

        cd.setBody(cd.getBody().setStatements(new NodeList<>(filteredStatementList)));
    }

    private void addSuperCall(ConstructorDeclaration cd) {
        ResolvedConstructorDeclaration shortestConstructor = getShortestSuperConstructor(cd, this.isPreSlicing);

        if (shortestConstructor == null) {
            if (cd.isAnnotationPresent(Annotator.KEEP_ALL_ANNOTATION)) return;

            cd.setBody(cd.getBody().setStatements(new NodeList<>()));
            return;
        }

        // Add exceptions to constructor
        NodeList<ReferenceType> exceptions = new NodeList<>();
        List<ResolvedType> superConstructorExceptions = shortestConstructor.getSpecifiedExceptions();
        for (ResolvedType ex : superConstructorExceptions) {
            if (!ex.isReferenceType()) continue;
            ResolvedReferenceType rrt = ex.asReferenceType();
            Optional<ResolvedReferenceTypeDeclaration> rrtdOpt = rrt.getTypeDeclaration();
            if (rrtdOpt.isEmpty()) continue;
            ResolvedReferenceTypeDeclaration rrtd = rrtdOpt.get();

            exceptions.add(StaticJavaParser.parseClassOrInterfaceType(rrtd.getQualifiedName()));
        }

        exceptions.forEach(cd::addThrownException);

        if (shortestConstructor.getNumberOfParams() == 0) {
            cd.setBody(cd.getBody().setStatements(new NodeList<>()));
            return;
        }

        NodeList<Expression> arguments = new NodeList<>();
        for (int i = 0; i < shortestConstructor.getNumberOfParams(); i++) {
            arguments.add(SlicingUtil.getGenericReturnExpression(shortestConstructor.getParam(i).describeType()));
        }

        ExplicitConstructorInvocationStmt cist = new ExplicitConstructorInvocationStmt();
        cist.setThis(false);
        cist.setArguments(arguments);
        cd.setBody(cd.getBody().setStatements(new NodeList<>(cist)));
    }

    private ResolvedConstructorDeclaration getShortestSuperConstructor(ConstructorDeclaration cd, boolean onlyKeptConstructor) {
        ResolvedConstructorDeclaration rcd = RobustResolver.tryResolve(cd);
        if (rcd == null) return null;

        ResolvedReferenceTypeDeclaration declaringType = rcd.declaringType();
        if (!declaringType.isClass()) return null;

        ResolvedClassDeclaration declaringClass = declaringType.asClass();
        Optional<ResolvedReferenceType> superClassOpt = RobustResolver.tryResolveSuperClass(declaringClass);
        if (superClassOpt.isEmpty()) return null;

        ResolvedReferenceType superClass = superClassOpt.get();
        Optional<ResolvedReferenceTypeDeclaration> superClassDeclarationOpt = superClass.getTypeDeclaration();
        if (superClassDeclarationOpt.isEmpty()) return null;

        ResolvedReferenceTypeDeclaration superClassDeclaration = superClassDeclarationOpt.get();

        // if super class is not a ReflectionClassDeclaration, a default constructor will be inserted
        if (this.isPreSlicing && !(superClassDeclaration instanceof ReflectionClassDeclaration)) return null;
        if (!(superClassDeclaration instanceof ReflectionClassDeclaration) && !this.targetClass.equals(superClassDeclaration.getQualifiedName())) return null;

        List<ResolvedConstructorDeclaration> constructors = superClassDeclaration.getConstructors();

        // filter only kept constructors
        if (onlyKeptConstructor && this.targetClass.equals(superClassDeclaration.getQualifiedName())) {
            constructors = constructors.stream().filter(rCon -> {
                Optional<ConstructorDeclaration> conOpt = rCon.toAst(ConstructorDeclaration.class);
                if (conOpt.isEmpty()) return false;
                ConstructorDeclaration con = conOpt.get();
                return con.isAnnotationPresent(Annotator.KEEP_ANNOTATION) || con.isAnnotationPresent(Annotator.KEEP_ALL_ANNOTATION);
            })
                    .collect(Collectors.toList());
        }

        if (constructors.size() < 1) return null;

//        if (constructors.stream().anyMatch(con -> con.getNumberOfParams() == 0)) return null;
        // conversion to ArrayList is needed, as the returned list is immutable and cannot be sorted
        List<ResolvedConstructorDeclaration> superConstructors = new ArrayList<>(constructors);
        superConstructors.sort(Comparator.comparingInt(ResolvedMethodLikeDeclaration::getNumberOfParams));

        return superConstructors.get(0);
    }

    private void addFieldInitializations(ConstructorDeclaration cd, ClassOrInterfaceDeclaration cid) {
        List<FieldDeclaration> fieldDeclarations = cid.findAll(FieldDeclaration.class);
        for (FieldDeclaration fd : fieldDeclarations) {
            Optional<ClassOrInterfaceDeclaration> ancestorDeclarationOpt = fd.findAncestor(ClassOrInterfaceDeclaration.class);
            if (ancestorDeclarationOpt.isEmpty()) continue;
            ClassOrInterfaceDeclaration ancestorDeclaration = ancestorDeclarationOpt.get();
            if (!ancestorDeclaration.equals(cid)) continue;
            Optional<EnumDeclaration> fieldEnumDeclarationOpt = fd.findAncestor(EnumDeclaration.class);

            if (fieldEnumDeclarationOpt.isPresent()) {
                EnumDeclaration fieldEnumDeclaration = fieldEnumDeclarationOpt.get();
                Optional<EnumDeclaration> cidEnumDeclarationOpt = cid.findAncestor(EnumDeclaration.class);
                if (cidEnumDeclarationOpt.isEmpty()) continue;
                EnumDeclaration cidEnumDeclaration = cidEnumDeclarationOpt.get();
                if (!fieldEnumDeclaration.equals(cidEnumDeclaration)) continue;
            }


            NodeList<VariableDeclarator> variables = fd.getVariables();
            variables.forEach(v -> {
                // if the initializer is a literal, keep it
                // otherwise we may have duplicate case literals, if we reset two fields to the same value

                Optional<Expression> initializerOpt = v.getInitializer();
                if (initializerOpt.isPresent()) {
                    Expression initializer = initializerOpt.get();
                    if (allOperatorsLiterals(initializer)) return;
                    if (fd.isFinal() && fd.isStatic()) {
                        // cannot assign a value to a static final field in constructor
                        // so just replace it with a literal
                        v.setInitializer(SlicingUtil.getGenericReturnExpression(v.getTypeAsString()));
                        return;
                    }

                    v.removeInitializer();
                } else {
                    if (fd.isFinal() && fd.isStatic()) {
                        v.setInitializer(SlicingUtil.getGenericReturnExpression(v.getTypeAsString()));
                        return;
                    }
                }

                // cannot assign final static fields in a constructor
                if (fd.isFinal()) {
                    // only need a dummy assignment for final fields
                    Expression dummyLiteral =  SlicingUtil.getGenericReturnExpression(v.getTypeAsString());
                    Expression fieldToAssign = new FieldAccessExpr(new ThisExpr(), v.getNameAsString());
                    AssignExpr assignExp = new AssignExpr(fieldToAssign, dummyLiteral, AssignExpr.Operator.ASSIGN);
                    ExpressionStmt assignExpStmt = new ExpressionStmt(assignExp);
                    cd.getBody().getStatements().add(assignExpStmt);
                }
            });
        }
    }

    private void fixInitializedFinalFields(ConstructorDeclaration cd) {
        BlockStmt body = cd.getBody();
        List<AssignExpr> assignExpressions = body.findAll(AssignExpr.class);
        for (AssignExpr assignExpr : assignExpressions) {
            Expression assignTarget = assignExpr.getTarget();

            ResolvedValueDeclaration rvd;
            if (assignTarget.isFieldAccessExpr()) {
                FieldAccessExpr fieldAccessExpr = assignTarget.asFieldAccessExpr();
                rvd = RobustResolver.tryResolve(fieldAccessExpr);
            } else if (assignTarget.isNameExpr()) {
                NameExpr nameExpr = assignTarget.asNameExpr();
                rvd = RobustResolver.tryResolve(nameExpr);
            } else {
                continue;
            }

            if (rvd == null) continue;
            if (!rvd.isField()) continue;
            ResolvedFieldDeclaration rfd = rvd.asField();

            if (!(rfd instanceof JavaParserFieldDeclaration)) continue;
            JavaParserFieldDeclaration jpfd = (JavaParserFieldDeclaration) rfd;
            FieldDeclaration fieldDeclaration = jpfd.getWrappedNode();

            if (!fieldDeclaration.isFinal()) continue;
            fieldDeclaration.setFinal(false);
        }
    }

    private NodeList<Statement> getFieldInitializingStatements(TypeDeclaration<?> td) {
        NodeList<Statement> statements = new NodeList<>();
        List<FieldDeclaration> fields = td.getFields();
        for (FieldDeclaration fd : fields) {
            if (!fd.isAnnotationPresent(Annotator.KEEP_ANNOTATION) && !fd.isAnnotationPresent(Annotator.KEEP_ALL_ANNOTATION)) continue;
            if (!fd.isFinal()) continue;
            if (fd.isStatic()) continue;

            NodeList<VariableDeclarator> variables = fd.getVariables();
            for (VariableDeclarator variable : variables) {
                if (variable.getInitializer().isPresent()) continue;

                AssignExpr initializationExpr = new AssignExpr(variable.getNameAsExpression(), SlicingUtil.getGenericReturnExpression(variable.getTypeAsString()), AssignExpr.Operator.ASSIGN);
                ExpressionStmt initializationStmt = new ExpressionStmt(initializationExpr);
                statements.add(initializationStmt);
            }
        }
        return statements;
    }

    private Statement getSuperCall(ConstructorDeclaration cd) {
        NodeList<Statement> statements = cd.getBody().getStatements();
        for (Statement statement : statements) {
            if (!statement.isExplicitConstructorInvocationStmt()) continue;
            ExplicitConstructorInvocationStmt eci = statement.asExplicitConstructorInvocationStmt();
            if (eci.isThis()) continue;
            return statement;
        }
        return null;
    }

    private Statement wrapInExceptionCatch(Statement st) {
        Parameter para = new Parameter(StaticJavaParser.parseClassOrInterfaceType("Exception"), "e");
        CatchClause cc = new CatchClause(para, new BlockStmt());
        TryStmt ts = new TryStmt(new BlockStmt(new NodeList<>(st)), new NodeList<>(cc), new BlockStmt());
        return ts;
    }

    private boolean allOperatorsLiterals(Expression exp) {
        if (exp.isLiteralExpr()) return true;
        if (exp.isUnaryExpr()) return allOperatorsLiterals(exp.asUnaryExpr().getExpression());
        if (exp.isCastExpr() && exp.asCastExpr().getType().isPrimitiveType()) return allOperatorsLiterals(exp.asCastExpr().getExpression());
        if (exp.isBinaryExpr()) return allOperatorsLiterals(exp.asBinaryExpr().getLeft()) && allOperatorsLiterals(exp.asBinaryExpr().getRight());
        return false;
    }

}
