package de.upb.sse.jess.visitors;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import de.upb.sse.jess.annotation.Annotator;
import de.upb.sse.jess.exceptions.AmbiguityException;
import de.upb.sse.jess.generation.unknown.UnknownType;
import de.upb.sse.jess.inference.InferenceEngine;
import de.upb.sse.jess.model.stubs.*;
import de.upb.sse.jess.resolution.ResolutionUtil;
import de.upb.sse.jess.resolution.RobustResolver;
import de.upb.sse.jess.util.FileUtil;
import de.upb.sse.jess.util.ImportUtil;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class UnresolvableTypeVisitor extends VoidVisitorAdapter<Map<String, ClassType>> {
    private final CombinedTypeSolver combinedTypeSolver;
    private final InferenceEngine inferenceEngine;
    private final List<String> packageRoots;
    private final boolean failOnAmbiguity;
    private final List<ImportDeclaration> imports = new ArrayList<>();
    private String cuPackage = "";
    private String cuClass = "";

    @Override
    public void visit(CompilationUnit cu, Map<String, ClassType> stubClasses) {
        // always executed first to find all imports
        imports.addAll(ImportUtil.getRelevantImportsFromCU(cu));

        Optional<PackageDeclaration> cuPackageOpt = cu.getPackageDeclaration();
        cuPackage = cuPackageOpt.isPresent() ? cuPackageOpt.get().getNameAsString() : "";
        cuClass = cu.getPrimaryTypeName().isPresent() ? cu.getPrimaryTypeName().get() : "";
        super.visit(cu, stubClasses);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration dec, Map<String, ClassType> stubClasses) {
        super.visit(dec, stubClasses);

        // implemented interfaces
        NodeList<ClassOrInterfaceType> implementedTypes = dec.getImplementedTypes();
        for (ClassOrInterfaceType implementedType : implementedTypes) {
            if (RobustResolver.tryResolve(implementedType) != null) continue;
            String type = implementedType.getNameAsString();
            if (type == null) continue;

            int amountOfTypeArguments = implementedType.getTypeArguments().map(NodeList::size).orElse(0);
            insertInterface(stubClasses, new TypeContext(type), amountOfTypeArguments);
        }

        // extended types (maybe more than one for interfaces)
        NodeList<ClassOrInterfaceType> extendedTypes = dec.getExtendedTypes();
        for (ClassOrInterfaceType extendedType : extendedTypes) {
            if (RobustResolver.tryResolve(extendedType) != null) continue;
            String type = extendedType.getNameAsString();
            if (type == null) continue;

            int amountOfTypeArguments = extendedType.getTypeArguments().map(NodeList::size).orElse(0);
            if (dec.isInterface()) {
                insertInterface(stubClasses, new TypeContext(type), amountOfTypeArguments);
            } else {
                insertClass(stubClasses, new TypeContext(type), amountOfTypeArguments);
            }
        }
    }

    @Override
    public void visit(MethodDeclaration dec, Map<String, ClassType> stubClasses) {
        super.visit(dec, stubClasses);

        NodeList<ReferenceType> thrownExceptions = dec.getThrownExceptions();
        for (ReferenceType thrownException : thrownExceptions) {
            ResolvedType resolvedType = RobustResolver.tryResolve(thrownException);
            if (resolvedType != null) continue;

            String exceptionName = thrownException.toString();

            ClassType exceptionType = getOrCreateClassType(stubClasses, new TypeContext(exceptionName));
            exceptionType.addExtendedType(new TypeContext("Exception"));
        }
    }

    @Override
    public void visit(ConstructorDeclaration dec, Map<String, ClassType> stubClasses) {
        super.visit(dec, stubClasses);

        NodeList<ReferenceType> thrownExceptions = dec.getThrownExceptions();
        for (ReferenceType thrownException : thrownExceptions) {
            ResolvedType resolvedType = RobustResolver.tryResolve(thrownException);
            if (resolvedType != null) continue;

            String exceptionName = thrownException.toString();

            ClassType exceptionType = getOrCreateClassType(stubClasses, new TypeContext(exceptionName));
            exceptionType.addExtendedType(new TypeContext("Exception"));
        }
    }

    @Override
    public void visit(ClassOrInterfaceType type, Map<String, ClassType> stubClasses) {
        super.visit(type, stubClasses);
        if (RobustResolver.tryResolve(type) != null) return;


        String typeName = type.getNameAsString();
        if (typeName == null) return;

        // check if type arguments have been used for object creation
        int amountOfTypeArguments = type.getTypeArguments().map(NodeList::size).orElse(0);

        // TODO: handle inner classes
        insertClass(stubClasses, new TypeContext(typeName), amountOfTypeArguments);
    }

    @Override
    public void visit(FieldAccessExpr exp, Map<String, ClassType> stubClasses) {
        super.visit(exp, stubClasses);
        if (RobustResolver.tryResolve(exp) != null) return;

        Expression scope = exp.getScope();

        TypeContext scopeType = inferenceEngine.inferType(scope);
        TypeContext scopeClass = scopeType != null ? scopeType : new TypeContext(scope.toString());

        String fieldName = exp.getNameAsString();
        TypeContext fieldType = inferenceEngine.inferType(exp);

        boolean isStatic = scopeType == null || (scope.isNameExpr() && inferenceEngine.inferType(scope.asNameExpr().getName()) == null);

        FieldType ft = new FieldType(fieldName, fieldType, isStatic, Visibility.PUBLIC);
        insertField(stubClasses, scopeClass, ft);
    }

    @Override
    public void visit(MethodCallExpr exp, Map<String, ClassType> stubClasses) {
        super.visit(exp, stubClasses);
        if (RobustResolver.tryResolve(exp) != null) return;

        // called method is not resolvable
        Optional<Expression> scopeOpt = exp.getScope();
        if (scopeOpt.isPresent()) {
            Expression scope = scopeOpt.get();
            TypeContext scopeType = inferenceEngine.inferType(scope);
            TypeContext scopeClass = scopeType != null ? scopeType : new TypeContext(scope.toString());

            boolean isStatic = scopeType == null || (scope.isNameExpr() && inferenceEngine.inferType(scope.asNameExpr().getName()) == null);

            String methodName = exp.getNameAsString();
            TypeContext methodType = inferenceEngine.inferType(exp);

            // if there is no assignment or other usage of the method, make it void
            Optional<Node> parentNodeOpt = exp.getParentNode();
            if (methodType != null && methodType.getType().equals(UnknownType.CLASS) && parentNodeOpt.isPresent() && parentNodeOpt.get() instanceof ExpressionStmt) {
                methodType = new TypeContext("void");
            }

            // infer parameter types
            List<TypeContext> parameterTypes = getParameterTypes(exp);

            MethodType mt = new MethodType(methodName, parameterTypes, methodType, false, isStatic);
            if (scope.isSuperExpr()) {
                MethodDeclaration overridingMethod = getOverridingMethodDec(exp, methodName, parameterTypes);
                if (overridingMethod != null) {
                    ClassType ct = getOrCreateClassType(stubClasses, scopeClass);
                    Visibility visibility = overridingMethod.isPublic() ? Visibility.PUBLIC : Visibility.PROTECTED;
                    NodeList<ReferenceType> thrownExceptions = overridingMethod.getThrownExceptions();
                    thrownExceptions.forEach(ex -> addImport(ct, new TypeContext(ex.toString())));
                    mt = new MethodType(methodName, parameterTypes, methodType, false, isStatic, visibility, thrownExceptions);
                }
            }

            insertMethod(stubClasses, scopeClass, mt);
        } else {
            String methodName = exp.getNameAsString();
            TypeContext methodType = inferenceEngine.inferType(exp);

            // if there is no assignment or other usage of the method, make it void
            Optional<Node> parentNodeOpt = exp.getParentNode();
            if (methodType != null && methodType.getType().equals(UnknownType.CLASS) && parentNodeOpt.isPresent() && parentNodeOpt.get() instanceof ExpressionStmt) {
                methodType = new TypeContext("void");
            }

            // infer parameter types
            List<TypeContext> parameterTypes = getParameterTypes(exp);
            MethodType mt = new MethodType(methodName, parameterTypes, methodType, false, true);

            ClassOrInterfaceDeclaration declaringClass = InferenceEngine.getDeclaringClass(exp);
            insertMethod(stubClasses, new TypeContext(declaringClass.getNameAsString()), mt);
        }
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt stmt, Map<String, ClassType> stubClasses) {
        super.visit(stmt, stubClasses);
        if (stmt.isThis()) return;
        if (RobustResolver.tryResolve(stmt) != null) return;

        Optional<ClassOrInterfaceDeclaration> classDecOpt = stmt.findAncestor(ClassOrInterfaceDeclaration.class);
        if (classDecOpt.isEmpty()) return;

        ClassOrInterfaceDeclaration classDec = classDecOpt.get();
        if (classDec.isInterface()) return;

        NodeList<ClassOrInterfaceType> extendedTypes = classDec.getExtendedTypes();
        if (extendedTypes.size() < 1) return;

        ClassOrInterfaceType extendedType = extendedTypes.get(0);
        String type = extendedType.getNameAsString();

        // infer parameter types
        List<TypeContext> parameterTypes = getParameterTypes(stmt);

        MethodType mt = new MethodType(type, parameterTypes, null, true, false);
        insertMethod(stubClasses, new TypeContext(type), mt);
    }

    @Override
    public void visit(ObjectCreationExpr exp, Map<String, ClassType> stubClasses) {
        super.visit(exp, stubClasses);
        if (RobustResolver.tryResolve(exp) != null) return;

        String className = exp.getTypeAsString();

        // infer parameter types
        List<TypeContext> parameterTypes = getParameterTypes(exp);
        MethodType mt = new MethodType(className, parameterTypes, null, true, false);
        insertMethod(stubClasses, new TypeContext(className), mt);
    }

    @Override
    public void visit(ThrowStmt stmt, Map<String, ClassType> stubClasses) {
        super.visit(stmt, stubClasses);
        Expression exp = stmt.getExpression();
        if (RobustResolver.tryResolve(exp) != null) return;

        TypeContext typeContext = inferenceEngine.inferType(exp);
        if (typeContext == null) return;

        ClassType ct = getOrCreateClassType(stubClasses, typeContext);
        ct.addExtendedType(new TypeContext("RuntimeException"));
    }

    @Override
    public void visit(CatchClause cc, Map<String, ClassType> stubClasses) {
        super.visit(cc, stubClasses);
        Parameter param = cc.getParameter();
        Type type = param.getType();

        if (type.isUnionType()) {
            UnionType uType = type.asUnionType();
            for (ReferenceType rt : uType.getElements()) {
                if (RobustResolver.tryResolve(rt) != null) continue;

                String rtType = rt.toString();
                if (rtType == null) continue;

                ClassType ct = getOrCreateClassType(stubClasses, new TypeContext(rtType));
                ct.addExtendedType(new TypeContext("RuntimeException"));
            }
        } else {
            ReferenceType rt = type.asReferenceType();
            if (RobustResolver.tryResolve(rt) != null) return;

            String rtType = rt.toString();
            if (rtType == null) return;

            ClassType ct = getOrCreateClassType(stubClasses, new TypeContext(rtType));
            ct.addExtendedType(new TypeContext("RuntimeException"));
        }
    }

    @Override
    public void visit(ForEachStmt stmt, Map<String, ClassType> stubClasses) {
        super.visit(stmt, stubClasses);
        Expression iterable = stmt.getIterable();
        if (RobustResolver.tryResolve(iterable) != null) return;

        TypeContext iterableType = inferenceEngine.inferType(iterable);
        if (iterableType == null) return;

        ClassType ct = getOrCreateClassType(stubClasses, iterableType);
        TypeContext variableType = inferenceEngine.inferType(stmt.getVariable());
        if (variableType == null) return;

        ct.addInterfaceImplementation(new TypeContext("Iterable<" + variableType.getType() + ">"));
        ct.addImport("java.util.Iterator");

        MethodType mt = new MethodType("iterator", Collections.emptyList(), new TypeContext("Iterator<" + variableType.getType() + ">"), false, false);
        // the following statement leads the compiler to crashing when running variousTest4
//        MethodInformation mi = new MethodInformation("iterator", Collections.emptyList(), variableType, false, false);
        insertMethod(stubClasses, iterableType, mt);
    }

    @Override
    public void visit(MethodReferenceExpr exp, Map<String, ClassType> stubClasses) {
        super.visit(exp, stubClasses);
        if (RobustResolver.tryResolve(exp) != null) return;


    }

    @Override
    public void visit(MarkerAnnotationExpr exp, Map<String, ClassType> stubClasses) {
        super.visit(exp, stubClasses);
        if (exp.getNameAsString().equals(Annotator.KEEP_ANNOTATION)) return;
        if (exp.getNameAsString().equals(Annotator.KEEP_ALL_ANNOTATION)) return;
        if (exp.getNameAsString().equals(Annotator.TARGET_METHOD_ANNOTATION)) return;
        if (RobustResolver.tryResolve(exp) != null) return;

        insertAnnotationDeclaration(stubClasses, new TypeContext(exp.getNameAsString()));
    }

    @Override
    public void visit(SingleMemberAnnotationExpr exp, Map<String, ClassType> stubClasses) {
        super.visit(exp, stubClasses);
        if (RobustResolver.tryResolve(exp) != null) return;

        String annotationName = exp.getNameAsString();
        TypeContext annoParameterType = inferenceEngine.inferType(exp.getMemberValue());
        if (annoParameterType == null) return;

        insertAnnotationDeclaration(stubClasses, new TypeContext(annotationName));
        FieldType ft = new FieldType("value", annoParameterType, false, Visibility.PUBLIC);
        insertField(stubClasses, new TypeContext(annotationName), ft);
    }

    @Override
    public void visit(NormalAnnotationExpr exp, Map<String, ClassType> stubClasses) {
        super.visit(exp, stubClasses);
        if (RobustResolver.tryResolve(exp) != null) return;

        String annotationName = exp.getNameAsString();
        insertAnnotationDeclaration(stubClasses, new TypeContext(annotationName));

        for (MemberValuePair pair : exp.getPairs()) {
            TypeContext annoParameterType = inferenceEngine.inferType(pair.getValue());
            if (annoParameterType == null) continue;

            FieldType ft = new FieldType(pair.getNameAsString(), annoParameterType , false, Visibility.PUBLIC);
            insertField(stubClasses, new TypeContext(annotationName), ft);
        }
    }

    @Override
    public void visit(CastExpr ce, Map<String, ClassType> stubClasses) {
        super.visit(ce, stubClasses);
        if (RobustResolver.tryResolve(ce) != null) return;

        TypeContext type = inferenceEngine.inferType(ce.getExpression());
        if (type == null) return;

        if (type.getType().equals(ce.getTypeAsString())) return;

        ClassType ct = getOrCreateClassType(stubClasses, new TypeContext(ce.getTypeAsString()));
        ct.addExtendedType(type);
        addImport(ct, type);
    }

    private MethodDeclaration getOverridingMethodDec(Node n, String methodName, List<TypeContext> parameters) {
        Optional<ClassOrInterfaceDeclaration> decOpt = n.findAncestor(ClassOrInterfaceDeclaration.class);
        if (decOpt.isEmpty()) return null;

        ClassOrInterfaceDeclaration dec = decOpt.get();
        List<MethodDeclaration> methods = dec.getMethods();

        List<String> parameterNames = parameters.stream().map(TypeContext::getType).collect(Collectors.toList());

        for (MethodDeclaration mDec : methods) {
            if (!mDec.getNameAsString().equals(methodName)) continue;
            if (!mDec.getParameters().stream().map(NodeWithType::getTypeAsString).collect(Collectors.toList()).equals(parameterNames)) continue;
            return mDec;
        }
        return null;
    }

    private List<TypeContext> getParameterTypes(NodeWithArguments<?> node) {
        // infer parameter types
        List<TypeContext> parameterTypes = new ArrayList<>();
        for (Expression arg : node.getArguments()) {
            TypeContext paramType = inferenceEngine.inferType(arg);
            if (paramType == null) continue;

            parameterTypes.add(paramType);
        }
        return parameterTypes;
    }

    private String findPackageName(TypeContext typeContext) {
        // check if the type needs another context for finding the package name
        CompilationUnit contextCu = typeContext.getContext();
        List<ImportDeclaration> imports = contextCu == null ? this.imports : ImportUtil.getRelevantImportsFromCU(contextCu);

        String typeName = typeContext.getType();
        // check if it's an Unknown type
        if (typeName.equals(UnknownType.CLASS)) return UnknownType.PACKAGE;
        // check if the type is already resolvable within the project
        String resolvedType = tryResolve(typeName);
        if (resolvedType != null) return resolvedType;
        // check if there is a direct import to class match
        for (ImportDeclaration id : imports) {
            if (id.isAsterisk()) continue;
            if (id.isStatic()) continue;

            String[] splitImport = id.getNameAsString().split("\\.");
            String importClassName = splitImport[splitImport.length - 1];

            if (importClassName.equals(typeName)) return ImportUtil.getPackageFromImportName(id.getNameAsString());
        }

        // check if the class is in the same package as the currently processed compilation unit
        if (isInSamePackageAsCompilationUnit(typeName, contextCu)) return cuPackage;

        // if there is no match, check if there is only a single asterisk import which can be matched
        List<ImportDeclaration> asteriskImports = imports.stream()
                .filter(ImportDeclaration::isAsterisk)
                .collect(Collectors.toList());
        if (asteriskImports.size() == 1) {
            return asteriskImports.get(0).getNameAsString();
        }

        if (failOnAmbiguity) throw new AmbiguityException("Cannot find a directly matchable import declaration for the following class: " + typeName);
        // need to add the import to unknown package here!
        return UnknownType.PACKAGE;
    }

    private boolean isInSamePackageAsCompilationUnit(String className, CompilationUnit cu) {
        String packageName = cuPackage;
        if (cu != null) {
            Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
            if (packageDeclaration.isPresent()) packageName = packageDeclaration.get().getNameAsString();
        }

        for (String packageRoot : packageRoots) {
            String path = packageRoot + File.separator + packageName.replace(".", File.separator);
            if (!new File(path).exists()) continue;
            List<String> allJavaFiles = FileUtil.getAllJavaFiles(path);
            if (allJavaFiles.stream().anyMatch(file -> file.endsWith(File.separator + className + ".java"))) return true;
        }
        return false;
    }

    private void insertClass(Map<String, ClassType> stubClasses, TypeContext typeContext, int typeParameters) {
        ClassType ct = getOrCreateClassType(stubClasses, typeContext);
        ct.setTypeParameters(typeParameters);
    }

    private void insertInterface(Map<String, ClassType> stubClasses, TypeContext typeContext, int typeParameters) {
        ClassType ct = getOrCreateClassType(stubClasses, typeContext);
        ct.setInterfaceType(true);
        ct.setTypeParameters(typeParameters);
    }

    private void insertAnnotationDeclaration(Map<String, ClassType> stubClasses, TypeContext typeContext) {
        ClassType ct = getOrCreateClassType(stubClasses, typeContext);
        ct.setAnnotationType(true);
    }

    private void insertField(Map<String, ClassType> stubClasses, TypeContext typeContext, FieldType ft) {
        ClassType ct = getOrCreateClassType(stubClasses, typeContext);
        ct.addFieldType(ft);
        if (ft.getType() == null) return;

        addImport(ct, ft.getType());
    }

    private void insertMethod(Map<String, ClassType> stubClasses, TypeContext typeContext, MethodType mt) {
        ClassType ct = getOrCreateClassType(stubClasses, typeContext);
        ct.addMethodType(mt);
        addImport(ct, mt.getParameterTypes());
        if (mt.getReturnType() == null) return;
        addImport(ct, mt.getReturnType());
    }

    private ClassType getOrCreateClassType(Map<String, ClassType> stubClasses, TypeContext typeContext) {
        if (typeContext == null) return null;

        TypeContext cleanTypeContext = ImportUtil.getCleanTypeContext(typeContext);
        String cleanClassName = cleanTypeContext.getType();
        String packageName = findPackageName(cleanTypeContext);

        // this adds the import to the unknown package to the currently processed unit, whenever an unresolvable type is mapped to the unknown package
//        if (packageName.equals(UnknownType.PACKAGE) && !cuClass.isEmpty()) {
//            String currentCuFQN = !cuPackage.isEmpty() ? cuPackage + "." + cuClass : cuClass;
//            ClassType currentCuType = stubClasses.getOrDefault(currentCuFQN, new ClassType(cuClass, cuPackage));
//            currentCuType.addImport(UnknownType.PACKAGE + ".*");
//            stubClasses.putIfAbsent(currentCuFQN, currentCuType);
//        }

        String fixedClassName = cleanClassName.replace(packageName + ".", "");
        String fqn = packageName + "." + fixedClassName;
        TypeContext newTypeContext = new TypeContext(fixedClassName, cleanTypeContext.getContext());

        ClassType ct = stubClasses.getOrDefault(fqn, new ClassType(newTypeContext, packageName));

        // do not include reflective types into the stublist
        if (ResolutionUtil.isReflectiveType(fixedClassName)) return ct;
        if (ResolutionUtil.isReflectiveType(fqn)) return ct;
        if (ResolutionUtil.isInDefaultPackage(fixedClassName)) return ct;
        if (ImportUtil.isPrimitiveType(fixedClassName)) return ct;

        stubClasses.putIfAbsent(fqn, ct);
        return ct;
    }

    private void addImport(ClassType ct, List<TypeContext> types) {
        types.forEach(type -> addImport(ct, type));
    }

    private void addImport(ClassType ct, TypeContext type) {
        if (type == null) return;

        List<TypeContext> genericTypeContexts = ImportUtil.getGenericTypes(type);

        for (TypeContext genericTypeContext : genericTypeContexts) {
            TypeContext cleanTypeContext = ImportUtil.getCleanTypeContext(genericTypeContext);
            String cleanTypeName = cleanTypeContext.getType();
            if (ImportUtil.isPrimitiveType(cleanTypeName)) continue;
            if (ct.containsImport(cleanTypeName)) continue;
            if (ResolutionUtil.isInDefaultPackage(cleanTypeName)) continue;

            String packageName = findPackageName(cleanTypeContext);

            if (packageName == null) continue;
            if (packageName.equals("")) continue;
            if (packageName.equals(ct.getPackageName())) continue;

            String fqn =  cleanTypeName.contains(".") ? cleanTypeName : packageName + "." + cleanTypeName;
            ct.addImport(fqn);
        }
    }

    private String tryResolve(String className) {
        try {
            ResolvedReferenceTypeDeclaration resolvedType = combinedTypeSolver.solveType(className);
            return resolvedType.getPackageName();
        } catch (Exception e) {
            return null;
        }
    }

}
