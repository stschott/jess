package de.upb.sse.jess.inference;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedLambdaConstraintType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import de.upb.sse.jess.exceptions.AmbiguityException;
import de.upb.sse.jess.generation.unknown.UnknownType;
import de.upb.sse.jess.model.stubs.TypeContext;
import de.upb.sse.jess.resolution.RobustResolver;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public class InferenceEngine {
    private final boolean failOnAmbiguity;


    public TypeContext inferType(Expression exp) {
        if (exp.isLiteralExpr()) return inferType(exp.asLiteralExpr());

        String resolvedType = tryInternalResolve(exp);
        if (resolvedType != null) return new TypeContext(resolvedType);

        if (exp.isSuperExpr()) {
            NodeList<ClassOrInterfaceType> extendedClasses = getExtendedClasses(exp);
            if (extendedClasses.size() < 1) return null;
            String type = getExtendedClasses(exp).get(0).getNameAsString();
            if (type == null) return null;

            return new TypeContext(type);
        }
        if (exp.isObjectCreationExpr()) {
            String type = exp.asObjectCreationExpr().getTypeAsString();
            if (type == null) return null;

            return new TypeContext(type);
        }
        if (exp.isCastExpr()) {
           String type = exp.asCastExpr().getTypeAsString();
           if (type == null) return null;

           return new TypeContext(type);
        }
        if (exp.isClassExpr()) return new TypeContext("Class<?>");

        if (exp.isEnclosedExpr()) return inferType(exp.asEnclosedExpr().getInner());
        if (exp.isNameExpr()) return inferType(exp.asNameExpr().getName());
        if (exp.isArrayAccessExpr()) return inferType(exp.asArrayAccessExpr().getName());
        if (exp.isAssignExpr()) return inferType(exp.asAssignExpr().getTarget());
        if (exp.isUnaryExpr()) return inferType(exp.asUnaryExpr());
        if (exp.isBinaryExpr()) return inferType(exp.asBinaryExpr());

        if (exp.isFieldAccessExpr()) {
            FieldAccessExpr fExp = exp.asFieldAccessExpr();
            String type = tryInternalResolve(fExp);
            if (type != null) return new TypeContext(type);

            if (fExp.getScope().isThisExpr() || fExp.getScope().isSuperExpr()) {
                return inferType(fExp.getName());
            }

            return inferTypeFromParent(exp.asFieldAccessExpr());
        }
        if (exp.isMethodCallExpr()) {
            TypeContext typeContext = inferFromMapping(exp.asMethodCallExpr());
            if (typeContext != null) return typeContext;

            return inferTypeFromParent(exp.asMethodCallExpr());
        }

        return null;
    }

    public TypeContext inferTypeFromParent(Expression exp) {
        StringBuilder arrayAccessSuffix = new StringBuilder();
        Node parentNode = getParent(exp);

        while (parentNode instanceof ArrayAccessExpr) {
            ArrayAccessExpr parentArrayAccessExpr = (ArrayAccessExpr) parentNode;
            // check if the expression is used as the index within the ArrayAccessExpr
            if (parentArrayAccessExpr.getIndex().equals(exp)) return new TypeContext("int");

            parentNode = getParent(parentNode);
            arrayAccessSuffix.append("[]");
        }

        if (parentNode instanceof Expression) {
            Expression parentExp = (Expression) parentNode;
            TypeContext inferredTypeContext = inferType(parentExp);
            if (inferredTypeContext == null) return null;

            String type = inferredTypeContext.getType();
            return new TypeContext(type + arrayAccessSuffix);
        } else if (parentNode instanceof Statement) {
            Statement parentStmt = (Statement) parentNode;
            TypeContext inferredTypeContext = inferType(parentStmt);
            if (inferredTypeContext == null) return null;

            String type = inferredTypeContext.getType();
            return new TypeContext(type + arrayAccessSuffix);
        } else if (parentNode instanceof VariableDeclarator) {
            VariableDeclarator vd = (VariableDeclarator) parentNode;
            String type = vd.getTypeAsString();
            if (type == null) return null;

            return new TypeContext(type + arrayAccessSuffix);
        } else {
            return null;
//            throw new RuntimeException("Parent node is not an Expression nor a Statement: " + parentNode + " Class: " + parentNode.getClass());
        }
    }

    public TypeContext inferType(Statement stmt) {
        if (stmt.isIfStmt() || stmt.isWhileStmt() || stmt.isDoStmt()) return new TypeContext("boolean");
        if (stmt.isThrowStmt()) {
            if (failOnAmbiguity) throw new AmbiguityException("Method that returns an undecidable exception: " + stmt);
            return new TypeContext(UnknownType.CLASS);
        }
        if (stmt.isExpressionStmt()) {
            if (failOnAmbiguity) throw new AmbiguityException("Empty field access for the following expression: " + stmt);
            return new TypeContext(UnknownType.CLASS);    // some random placeholder to generate something
        }
        if (stmt.isReturnStmt()) {
            MethodDeclaration declaringMethod = getDeclaringMethod(stmt);
            return new TypeContext(declaringMethod.getTypeAsString());
        }
        if (stmt.isForEachStmt()) {
            return new TypeContext(stmt.asForEachStmt().getVariableDeclarator().getTypeAsString());
        }
        return null;
    }

    public TypeContext inferType(LiteralExpr exp) {
        if (exp.isBooleanLiteralExpr()) return new TypeContext("boolean");
        else if (exp.isCharLiteralExpr()) return new TypeContext("char");
        else if (exp.isDoubleLiteralExpr()) return new TypeContext("double");
        else if (exp.isIntegerLiteralExpr()) return new TypeContext("int");
        else if (exp.isLongLiteralExpr()) return new TypeContext("long");
        else if (exp.isStringLiteralExpr()) return new TypeContext("String");
        else {
            if (failOnAmbiguity) throw new AmbiguityException("Method has been called with null literal and cannot be inferred");
            return new TypeContext(UnknownType.CLASS);
        }
    }

    public TypeContext inferType(SimpleName sn) {
        String name = sn.getIdentifier();

        TypeContext type = inferTypeFromParameter(sn, name);
        if (type != null) return type;

        type = inferTypeFromField(sn, name);
        if (type != null) return type;

        type = inferTypeFromBody(sn, name);
        if (type != null) return type;

        type = inferTypeFromSuperClass(sn, name);
        return type;
    }

    public TypeContext inferType(BinaryExpr exp) {
        if (exp.getOperator().equals(BinaryExpr.Operator.PLUS)) return inferType(exp.getLeft());

        return null;
    }

    public TypeContext inferType(UnaryExpr exp) {
        switch (exp.getOperator()) {
            case LOGICAL_COMPLEMENT:
                return new TypeContext("boolean");
            case BITWISE_COMPLEMENT:
            case POSTFIX_DECREMENT:
            case POSTFIX_INCREMENT:
            case PREFIX_DECREMENT:
            case PREFIX_INCREMENT:
            case MINUS:
            case PLUS:
                return new TypeContext("int");
            default:
                return null;
        }

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private TypeContext inferTypeFromParameter(Node node, String name) {
        Optional<CallableDeclaration> callDecOpt = node.findAncestor(CallableDeclaration.class);
        if (callDecOpt.isEmpty()) return null;

        CallableDeclaration<?> callDec = callDecOpt.get();
        NodeList<Parameter> parameters = callDec.getParameters();

        for (Parameter parameter : parameters) {
            if (!parameter.getNameAsString().equals(name)) continue;
            String type = parameter.getTypeAsString();
            if (type == null) continue;

            return new TypeContext(type);
        }

        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private TypeContext inferTypeFromField(Node node, String name) {
        Optional<TypeDeclaration> typeDecOpt = node.findAncestor(TypeDeclaration.class);

        while (typeDecOpt.isPresent()) {
            TypeDeclaration<?> typeDec = typeDecOpt.get();
            List<FieldDeclaration> fields = typeDec.getFields();
            for (FieldDeclaration field : fields) {
                for (VariableDeclarator varDec : field.getVariables()) {
                    if (!varDec.getNameAsString().equals(name)) continue;

                    String type = varDec.getTypeAsString();
                    if (type == null) continue;

                    return new TypeContext(type);
                }
            }
            typeDecOpt = typeDec.findAncestor(TypeDeclaration.class);
        }

        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private TypeContext inferTypeFromBody(Node node, String name) {
        Optional<BlockStmt> blockStmtOpt = node.findAncestor(BlockStmt.class);
        
        while (blockStmtOpt.isPresent()) {
            BlockStmt blockStmt = blockStmtOpt.get();
//            if (!getDeclaringClass(blockStmt).equals(getDeclaringClass(node))) break;
            
            NodeList<Statement> statements = blockStmt.getStatements();

            for (Statement statement : statements) {

                VariableDeclarationExpr vdExp = null;
                if (statement.isExpressionStmt()) {
                    ExpressionStmt expressionStmt = statement.asExpressionStmt();
                    Expression exp = expressionStmt.getExpression();
                    if (!exp.isVariableDeclarationExpr()) continue;
                    vdExp = exp.asVariableDeclarationExpr();
                } else if (statement.isForEachStmt()) {
                    vdExp = statement.asForEachStmt().getVariable();
                }

                if (vdExp == null) continue;

                NodeList<VariableDeclarator> variables = vdExp.getVariables();
                for (VariableDeclarator vd : variables) {
                    if (!vd.getNameAsString().equals(name)) continue;

                    String type = vd.getTypeAsString();
                    if (type == null) continue;

                    return new TypeContext(type);
                }


            }

            blockStmtOpt = blockStmt.findAncestor(BlockStmt.class);
        }
        
        return null;
    }

    private TypeContext inferTypeFromSuperClass(Node node, String name) {
        Optional<ClassOrInterfaceDeclaration> typeDecOpt = node.findAncestor(ClassOrInterfaceDeclaration.class);
        if (typeDecOpt.isEmpty()) return null;

        ClassOrInterfaceDeclaration typeDec = typeDecOpt.get();
        NodeList<ClassOrInterfaceType> extendedTypes = typeDec.getExtendedTypes();
        List<ClassOrInterfaceType> workList = new ArrayList<>(extendedTypes);

        while (!workList.isEmpty()) {
            ClassOrInterfaceType currClassType = workList.remove(0);
            ResolvedType resolvedType = RobustResolver.tryResolve(currClassType);
            if (resolvedType == null) continue;
            if (!resolvedType.isReferenceType()) continue;

            ResolvedReferenceType referenceType = resolvedType.asReferenceType();
            Set<ResolvedFieldDeclaration> declaredFields = referenceType.getDeclaredFields();
            for (ResolvedFieldDeclaration resolvedField : declaredFields) {
                Optional<FieldDeclaration> fieldOpt = resolvedField.toAst(FieldDeclaration.class);
                if (fieldOpt.isEmpty()) continue;
                FieldDeclaration field = fieldOpt.get();
                if (field.isPrivate()) continue;

                for (VariableDeclarator varDec : field.getVariables()) {
                    if (!varDec.getNameAsString().equals(name)) continue;

                    String type = varDec.getTypeAsString();
                    if (type == null) continue;
                    TypeContext tc = new TypeContext(type);

                    // need to get the compilation unit to find import from a different context than initial type usage
                    Optional<ResolvedReferenceTypeDeclaration> refTypeDecOpt = referenceType.getTypeDeclaration();
                    if (refTypeDecOpt.isEmpty()) return tc;
                    Optional<Node> astNodeOpt = refTypeDecOpt.get().toAst();
                    if (astNodeOpt.isEmpty()) return tc;
                    Optional<CompilationUnit> compilationUnitOpt = astNodeOpt.get().findCompilationUnit();
                    if (compilationUnitOpt.isEmpty()) return tc;
                    tc.setContext(compilationUnitOpt.get());

                    return tc;
                }
            }


            Optional<ResolvedReferenceTypeDeclaration> refTypeDecOpt = referenceType.getTypeDeclaration();
            if (refTypeDecOpt.isEmpty()) continue;
            Optional<ClassOrInterfaceDeclaration> astNodeOpt = refTypeDecOpt.get().toAst(ClassOrInterfaceDeclaration.class);
            if (astNodeOpt.isEmpty()) continue;
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration = astNodeOpt.get();
            NodeList<ClassOrInterfaceType> extendedTypes2 = classOrInterfaceDeclaration.getExtendedTypes();
            workList.addAll(extendedTypes2);
        }


        return null;
    }

    private TypeContext inferFromMapping(MethodCallExpr exp) {
        switch (exp.getNameAsString()) {
            case "toString":
                return new TypeContext("String");
            case "hashcode":
                return new TypeContext("int");
            default:
                return null;
        }
    }

    private Node getParent(Node n) {
        Optional<Node> parentNodeOpt = n.getParentNode();
        if (parentNodeOpt.isEmpty()) throw new RuntimeException("No parent node for the node: " + n);

        return parentNodeOpt.get();
    }

    private String tryInternalResolve(Expression exp) {
        if (exp.isMethodCallExpr()) {
            ResolvedMethodDeclaration rmd = RobustResolver.tryResolve(exp.asMethodCallExpr());
            if (rmd instanceof JavaParserMethodDeclaration) return ((JavaParserMethodDeclaration) rmd).getWrappedNode().getTypeAsString();
            if (rmd instanceof ReflectionMethodDeclaration) return rmd.getReturnType().describe();
        }

        String type;
        ResolvedType resolvedType = RobustResolver.tryResolve(exp);
        if (resolvedType == null) return null;
        if (resolvedType instanceof ResolvedLambdaConstraintType) {
            ResolvedLambdaConstraintType lambdaType = (ResolvedLambdaConstraintType) resolvedType;
            return lambdaType.getBound().describe();
        }
        return resolvedType.describe();
    }

    public static ClassOrInterfaceDeclaration getDeclaringClass(Node n) {
        Optional<ClassOrInterfaceDeclaration> typeDecOpt = n.findAncestor(ClassOrInterfaceDeclaration.class);
        if (typeDecOpt.isEmpty()) throw new RuntimeException("No declaring class for the node: " + n);

        return typeDecOpt.get();
    }

    public static MethodDeclaration getDeclaringMethod(Node n) {
        Optional<MethodDeclaration> methodDecOpt = n.findAncestor(MethodDeclaration.class);
        if (methodDecOpt.isEmpty()) throw new RuntimeException("No declaring method for the node: " + n);

        return methodDecOpt.get();
    }

    public static NodeList<ClassOrInterfaceType> getExtendedClasses(Node n) {
        ClassOrInterfaceDeclaration declaringClass = getDeclaringClass(n);
        return declaringClass.getExtendedTypes();
    }
}
