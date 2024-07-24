package de.upb.sse.jess.generation;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import de.upb.sse.jess.generation.unknown.UnknownType;
import de.upb.sse.jess.model.ImportContext;
import de.upb.sse.jess.model.stubs.*;
import de.upb.sse.jess.stats.StubbingStats;
import de.upb.sse.jess.util.FileUtil;
import de.upb.sse.jess.util.SlicingUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class StubGenerator {
    private final String outputDir;
    private final StubbingStats stubbingStats;
    private final List<String> generatedStubs = new ArrayList<>();

    public StubGenerator(String outputDir) {
        this(outputDir, new StubbingStats());
    }

    public StubGenerator(String outputDir, StubbingStats stubbingStats) {
        this.outputDir = outputDir;
        this.stubbingStats = stubbingStats;
    }

    public void generate(Collection<ClassType> stubClasses) {
        for (ClassType stubClass : stubClasses) {
            generate(stubClass);
        }
    }

    public void generate(ClassType stubClass) {
        String packageName = stubClass.getPackageName();
        String className = stubClass.getClassName();
        String fqn = stubClass.getFQN();
        Path outputPath = Path.of(outputDir, fqn.replace(".", "/") + ".java");

        if (generatedStubs.contains(fqn)) return;
        // check for keywords that are blacklisted
        if (isBlacklisted(className, packageName)) return;

        CompilationUnit genCu;
        try {
            // if the respective file already exists use the existing cu
            genCu = outputPath.toFile().exists() ? StaticJavaParser.parse(outputPath) : new CompilationUnit();
        } catch (IOException e) {
            genCu = new CompilationUnit();
        }

        if (packageName.length() > 0 && genCu.getPackageDeclaration().isEmpty()) genCu.setPackageDeclaration(packageName);
        addImports(genCu, stubClass.getImports());

        boolean existingTypeDec = false;
        TypeDeclaration<?> typeDec = genCu.findFirst(TypeDeclaration.class, td -> td.getNameAsString().equals(className)).orElse(null);
        if (typeDec == null) {
            if (stubClass.isAnnotationType()) {
                typeDec = genCu.addAnnotationDeclaration(className, Modifier.Keyword.PUBLIC);
                addAnnotationMembers(typeDec, stubClass.getFieldTypes());
            } else if (stubClass.isInterfaceType()){
                typeDec = genCu.addInterface(className, Modifier.Keyword.PUBLIC);
            } else {
                typeDec = genCu.addClass(className, Modifier.Keyword.PUBLIC);
            }
        } else {
            existingTypeDec = true;
        }

        if (typeDec instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration ciDec = (ClassOrInterfaceDeclaration) typeDec;

            addFields(typeDec, stubClass.getFieldTypes());
            addMethods(typeDec, stubClass.getMethodTypes());

            if (!existingTypeDec) {
                addTypeParameters(ciDec, stubClass.getTypeParameters());
                addInterfaces(ciDec, stubClass.getInterfaceImplementations());
                addExtendedTypes(ciDec, stubClass.getExtendedTypes());
            }
        }

        generatedStubs.add(fqn);

        stubbingStats.addStubbedFiles(1);
        stubbingStats.addStubbedLines(genCu.toString().split("\r?\n").length);

        FileUtil.printCompilationUnit(genCu, outputPath);
    }

    public void generatePackages(Collection<ImportContext> packages) {
        for (ImportContext importContext : packages) {
            if (importContext.getName().startsWith("java.")) continue;
            if (importContext.getName().startsWith("javax.")) continue;
            if (importContext.getName().startsWith("sun.")) continue;

            if (importContext.isStaticImport()) {
                FileUtil.createStaticPackageDirectory(outputDir, importContext.getName());
            } else {
                FileUtil.createPackageDirectory(outputDir, importContext.getName());
            }
        }
    }

    private void addImports(CompilationUnit cu, Set<String> imports) {
        Set<String> existingImports = cu.getImports().stream().map(NodeWithName::getNameAsString).collect(Collectors.toSet());
        for (String imp : imports) {
            if (existingImports.contains(imp)) continue;
            cu.addImport(imp);
        }
    }

    private void addFields(TypeDeclaration<?> typeDec, Set<FieldType> fields) {
        for (FieldType field : fields) {
            if (typeDec.getFieldByName(field.getName()).isPresent()) continue;

            Modifier.Keyword visibility = field.getVisibility() == Visibility.PROTECTED ? Modifier.Keyword.PROTECTED : Modifier.Keyword.PUBLIC;
            // TODO: generate placeholder type if unknown
            String fieldType = field.getType() == null ? UnknownType.CLASS : field.getType().getType();
            FieldDeclaration fieldDec = typeDec.addField(fieldType.replace("java.lang.", ""), field.getName(), visibility);
            fieldDec.setStatic(field.isStaticField());
            stubbingStats.incrementStubbedFields();
        }
    }

    private void addMethods(TypeDeclaration<?> typeDec, Set<MethodType> methods) {
        for (MethodType method : methods) {
            if (methodAlreadyExists(typeDec, method)) continue;

            // handle constructors
            if (method.isConstructor()) {
                addConstructor(typeDec, method);
                continue;
            }

            Modifier.Keyword visibility = method.getVisibility() == Visibility.PROTECTED ? Modifier.Keyword.PROTECTED : Modifier.Keyword.PUBLIC;
            MethodDeclaration methodDec = typeDec.addMethod(method.getName(), visibility);
            methodDec.setStatic(method.isStaticMethod());
            stubbingStats.incrementStubbedMethods();

            // set parameters
            boolean nullParameter = false;
            for (int i = 0; i < method.getParameterTypes().size(); i++) {
                TypeContext parameterTypeContext = method.getParameterTypes().get(i);
                if (parameterTypeContext == null) {
                    typeDec.remove(methodDec);
                    nullParameter = true;
                    break;
                }
                String parameterName = "arg" + i;
                methodDec.addParameter(parameterTypeContext.getType().replace("java.lang.", ""), parameterName);
            }

            if (nullParameter) continue;

            // set return type
            String methodReturnType = method.getReturnType() == null ? "void" : method.getReturnType().getType();
            methodDec.setType(methodReturnType);
            if (methodReturnType.equals("void")) continue;

            // add generic return statement if method has a non-void return type
//            if (typeDec instanceof ClassOrInterfaceDeclaration && !((ClassOrInterfaceDeclaration) typeDec).isInterface()) {
                addGenericReturnStatement(methodDec, methodReturnType);
//            }

            for (ReferenceType thrownException : method.getThrownExceptions()) {
                methodDec.addThrownException(thrownException);
            }
        }
    }

    private void addConstructor(TypeDeclaration<?> typeDec, MethodType constructor) {
        if (constructorAlreadyExists(typeDec, constructor)) return;
        ConstructorDeclaration constDec = typeDec.addConstructor(Modifier.Keyword.PUBLIC);
        stubbingStats.incrementStubbedConstructors();

        // set parameters
        for (int i = 0; i < constructor.getParameterTypes().size(); i++) {
            String parameterType = constructor.getParameterTypes().get(i).getType();
            if (parameterType == null) {
                typeDec.remove(constDec);
                return;
            }
            String parameterName = "arg" + i;
            constDec.addParameter(parameterType.replace("java.lang.", ""), parameterName);
        }
    }

    private void addTypeParameters(ClassOrInterfaceDeclaration typeDec, int amountOfTypeParameters) {
        for (int i = 0; i < amountOfTypeParameters; i++) {
            typeDec.addTypeParameter("T" + i);
        }
    }

    private void addInterfaces(ClassOrInterfaceDeclaration typeDec, Set<TypeContext> implementedInterfaces) {
//        Set<String> existingInterfaces = typeDec.getImplementedTypes().stream().map(NodeWithSimpleName::getNameAsString).collect(Collectors.toSet());
        for (TypeContext implementedInterface : implementedInterfaces) {
//            if (existingInterfaces.contains(implementedInterface)) continue;
            typeDec.addImplementedType(implementedInterface.getType());
        }
    }

    private void addExtendedTypes(ClassOrInterfaceDeclaration typeDec, Set<TypeContext> extendedTypes) {
        if (typeDec.isInterface()) {
            for (TypeContext extendedType : extendedTypes) {
                typeDec.addExtendedType(extendedType.getType());
            }
        } else {
            if (!extendedTypes.isEmpty()) typeDec.addExtendedType(extendedTypes.iterator().next().getType());
        }
    }

    private void addAnnotationMembers(TypeDeclaration<?> typeDec, Set<FieldType> members) {
        for (FieldType member : members) {
            if (member.getName().isEmpty()) continue;
            if (member.getType().getType().isEmpty()) continue;

            AnnotationMemberDeclaration memDec = new AnnotationMemberDeclaration();
            memDec.setType(member.getType().getType());
            memDec.setName(member.getName());
            memDec.setDefaultValue(SlicingUtil.getGenericReturnExpression(member.getType().getType()));
            typeDec.addMember(memDec);
        }
    }

    private NodeList<ClassOrInterfaceType> getImplementedTypes(Set<String> interfaceImplementations) {
        // TODO: create interface typedecs and link here
        return null;
    }

    private void addGenericReturnStatement(MethodDeclaration methodDec, String returnType) {
        BlockStmt methodBody = new BlockStmt();
        ReturnStmt returnStmt = new ReturnStmt();
        Expression genericReturn = SlicingUtil.getGenericReturnExpression(returnType);
        returnStmt.setExpression(genericReturn);
        methodBody.addStatement(returnStmt);
        methodDec.setBody(methodBody);
    }

    private boolean isBlacklisted(String className, String packageName) {
//        if (!packageName.equals(UnknownType.PACKAGE)) return false;
        switch (className) {
            case "java":
//            case "lang":
            case "org":
            case "com":
            case "de":
                return true;
            default:
                return false;
        }
    }

    private boolean methodAlreadyExists(TypeDeclaration<?> typeDec, MethodType method) {
        String[] parameters = method.getParameterTypes().stream()
                .map(param -> param.getType().replace("java.lang.String", "String"))
                .toArray(String[]::new);
        List<MethodDeclaration> methodsBySignature = typeDec.getMethodsBySignature(method.getName(), parameters);
        return !methodsBySignature.isEmpty();
    }

    private boolean constructorAlreadyExists(TypeDeclaration<?> typeDec, MethodType method) {
        String[] parameters = method.getParameterTypes().stream()
                .map(param -> param.getType().replace("java.lang.String", "String"))
                .toArray(String[]::new);
        Optional<ConstructorDeclaration> constructorBySignature = typeDec.getConstructorByParameterTypes(parameters);
        return constructorBySignature.isPresent();
    }

}
