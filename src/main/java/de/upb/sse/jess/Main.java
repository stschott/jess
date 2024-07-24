package de.upb.sse.jess;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import de.upb.sse.jess.comparison.BytecodeComparator;
import de.upb.sse.jess.comparison.MethodComparison;
import de.upb.sse.jess.configuration.JessConfiguration;
import de.upb.sse.jess.dependency.MavenDependencyResolver;
import de.upb.sse.jess.finder.JarFinder;
import de.upb.sse.jess.finder.PackageFinder;
import de.upb.sse.jess.finder.PomFinder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    // io: Wrong JP resolution of constructor in 53: input\commons-io\src\main\java\org\apache\commons\io\filefilter\FileFilterUtils.java
    // io: error while cloning in 71
    // lang: Wrong JP resolution in 34: input\commons-lang\src\main\java\org\apache\commons\lang3\CharUtils.java
    // lang: 4 ArrayUtils.nullToEmpty(Boolean[]) matches Boolean[] to function with boolean[] parameters instead of Object[]
    // lang: 141 clone issue with isolated compilation
    // lang: 173 varargs matching incorrect
    // lang: 204 nested method call resolution not working
    // lang: 220 generic method mismatch

    static int totalCompilations = 0;
    static int successfulCompilations = 0;
    static boolean comparison = true;
    static JessConfiguration config = new JessConfiguration(false, false, true, true, false, false);

    public static void main(String[] args) throws Exception {
//        Set<String> packageRoots = PackageFinder.findPackageRoots("input/commons-io");

//        compileFiles("input/commons-io/src/main/java", new Integer[]{ 53, 71 }, false);
//        compileFiles("input/commons-io/src/main/java", Collections.emptySet(), new Integer[]{ }, true);
//        compileFiles("input/commons-io/src/main/java", new Integer[]{ 53, 71, 91, 99, 164 }, true);
//        compileFiles("input/commons-lang/src/main/java", new Integer[]{ 4, 33, 68, 134, 141, 181 }, false);
//        compileFiles("input/commons-lang/src/main/java", new Integer[]{ 4, 68, 134, 141, 161, 173, 204, 220 }, true);
//        compileFiles("input/commons-lang/src/main/java", new Integer[]{ }, true);

//        compileFiles("input/gson/gson/src/main/java", new Integer[]{ }, true);
//        compileFiles("input/jackson-core/src/main/java", new Integer[]{ }, false);
//        MavenDependencyResolver.cleanupJars();
//        Set<String> jars = new HashSet<>();
//
//        Set<String> pomFilePaths = PomFinder.findPomFilePaths("input/javacv");
//        MavenDependencyResolver.resolveDependencies(pomFilePaths);
//        jars = JarFinder.find(Jess.JAR_DIRECTORY);
//        compileFiles("input/javacv/src/main/java", jars, new Integer[]{ }, true);


//        Path projectPath = Path.of("repos", "dbeaver_dbeaver");
//        String targetClass = "repos/dbeaver_dbeaver/bundles/org.jkiss.utils/src/org/jkiss/utils/IOUtils.java";
//        String methodSignature = "readStreamToBuffer(java.io.InputStream, byte[])";
//        Set<String> packages = PackageFinder.findPackageRoots(projectPath.toString());
//        Jess jess = new Jess(config, packages, Collections.emptyList());
//        jess.preSlice(targetClass, Collections.singletonList(methodSignature), Collections.emptyList(), Collections.emptyList());
//        int jessResult = jess.parse(targetClass);
//
//        BytecodeComparator bc = new BytecodeComparator(CompilerInvoker.output, projectPath.toString());
//        String[] splitTargetClass = targetClass.replace("\\", "/").replace(".java", "").split("/");
//        String targetClassName = splitTargetClass[splitTargetClass.length - 1];
//        List<MethodComparison> comparisons = bc.compareMethods(targetClassName);
//        comparisons.forEach(comp -> {
//            if (comp.isEqual()) return;
//            System.out.println(comp);
//        });

//        printResult();


        Path projectPath = Path.of("repos", "alibaba_datax");
        String targetClass = "repos/alibaba_datax/otsstreamreader/src/main/java/com/alibaba/datax/plugin/reader/otsstreamreader/internal/utils/OTSHelper.java";
        String methodSignature = "getStreamResponse(SyncClientInterface, String, boolean)";

        Set<String> packages = PackageFinder.findPackageRoots(projectPath.toString());
        Jess jess = new Jess(config, packages, Collections.emptyList());
        jess.preSlice(targetClass, Collections.singletonList(methodSignature), Collections.emptyList(), Collections.emptyList());
        int jessResult = jess.parse(targetClass);
    }

    public static void compileFiles(String targetDir, Integer[] exceptions) throws IOException {
        compileFiles(targetDir, Collections.emptySet(), exceptions, false);
    }

    public static void compileFiles(String targetDir, Set<String> jars, Integer[] exceptions) throws IOException {
        compileFiles(targetDir, jars, exceptions, false);
    }

    public static void compileFiles(String targetDir, Set<String> jars, Integer[] exceptions, boolean isolatedMethods) throws IOException {
        List<String> javaFiles = getAllJavaFiles(targetDir);
        List<Integer> exceptionList = List.of(exceptions);

        for (int i = 17; i < javaFiles.size(); i++) {
            if (exceptionList.contains(i)) continue;

            System.out.println("------------------------------------");
            System.out.println("Compiling File " + i + " " + javaFiles.get(i) + ":");
            if (isolatedMethods) {
                compileMethods(targetDir, javaFiles.get(i), jars);
            } else {
                compile(targetDir, javaFiles.get(i), jars, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            }
        }
    }

    public static void compileMethods(String targetDir, String targetClass, Set<String> jars) throws IOException {
        List<String> allMethods = getMethods(targetClass);

        for (String method : allMethods) {
            if (!method.equals("DirectoryWalker()")) continue;

            System.out.println("Compiling Method: " + method);
            List<String> isolatedMethods = new ArrayList<>(Collections.singleton(method));
            compile(targetDir, targetClass, jars, isolatedMethods, Collections.emptyList(), Collections.emptyList());

        }

        System.out.println("Compiling <clinit>");
        compile(targetDir, targetClass, jars, Collections.emptyList(), getTypeDeclarations(targetClass), Collections.emptyList());

        System.out.println("Compiling <init>");
        compile(targetDir, targetClass, jars, Collections.emptyList(), Collections.emptyList(), getTypeDeclarations(targetClass));
    }

    public static void compile(String targetDir, String targetClass, Set<String> jars, List<String> methodsToKeep, List<String> keepClinit, List<String> keepInit) throws IOException {
        Jess jess = new Jess(config, Collections.singleton(targetDir), jars);
        if (methodsToKeep.size() > 0 || keepClinit.size() > 0 || keepInit.size() > 0) {
            jess.preSlice(targetClass, methodsToKeep, keepClinit, keepInit);
        }
        boolean compSuccess = jess.parse(targetClass) == 0;
        totalCompilations++;
        if (compSuccess) successfulCompilations++;
        if (!compSuccess) return;
        if (!comparison) return;

        BytecodeComparator bc = new BytecodeComparator("output", targetDir.replace("src/main/java", "target/classes"));
        String[] splitTargetClass = targetClass.replace("\\", "/").replace(".java", "").split("/");
        String targetClassName = splitTargetClass[splitTargetClass.length - 1];
        List<MethodComparison> comparisons = bc.compareMethods(targetClassName);
        comparisons.forEach(comp -> {
            if (comp.isEqual()) return;
            System.out.println(comp);
            System.exit(1);
        });
    }


    public static List<String> getMethods(String target) throws IOException {
        CompilationUnit root = StaticJavaParser.parse(Paths.get(target));
        List<CallableDeclaration> allMethodDeclarations = root.findAll(CallableDeclaration.class);

        List<String> nonAnonymousMethodDeclarationSignatures = allMethodDeclarations.stream()
                .filter(md -> {
                    Optional<Node> parentNodeOpt = md.getParentNode();
                    if (parentNodeOpt.isEmpty()) return true;
                    Node parentNode = parentNodeOpt.get();
                    if (!(parentNode instanceof ObjectCreationExpr)) return true;
                    return false;
                })
                .map(md -> md.getSignature().asString())
                .collect(Collectors.toList());

        return nonAnonymousMethodDeclarationSignatures;
    }

    public static List<String> getTypeDeclarations(String target) throws IOException {
        CompilationUnit root = StaticJavaParser.parse(Paths.get(target));
        List<TypeDeclaration> allTypeDeclarations = root.findAll(TypeDeclaration.class);

        return allTypeDeclarations.stream()
                .map(Main::getClassSignature)
                .collect(Collectors.toList());
    }


    public static List<String> getAllJavaFiles(String dir) {
        List<String> javaFiles = new ArrayList<>();
        try {
            javaFiles = Files.find(Paths.get(dir), 999,
                            (p, bfa) -> bfa.isRegularFile() &&
                                    p.getFileName().toString().matches(".*\\.java") &&
                                    !p.getFileName().toString().contains("package-info") &&
                                    !p.getFileName().toString().contains("module-info")
                    ).map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return javaFiles;
    }

    private static void printResult() {
        System.out.println("Compilation Success: " +
                successfulCompilations + "/" +
                totalCompilations + "; " +
                ((double) successfulCompilations / totalCompilations)
        );
    }

    private static String getClassSignature(TypeDeclaration<?> classDec) {
        StringBuilder builder = new StringBuilder();
        Optional<ClassOrInterfaceDeclaration> parentClassOpt = classDec.findAncestor(ClassOrInterfaceDeclaration.class);
        while (parentClassOpt.isPresent()) {
            ClassOrInterfaceDeclaration parentClass = parentClassOpt.get();
            builder.append(parentClass.getNameAsString());
            builder.append(".");
            parentClassOpt = parentClass.findAncestor(ClassOrInterfaceDeclaration.class);
        }
        builder.append(classDec.getNameAsString());
        return builder.toString();
    }

}
