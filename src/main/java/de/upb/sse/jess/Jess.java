package de.upb.sse.jess;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import de.upb.sse.jess.annotation.Annotator;
import de.upb.sse.jess.configuration.JessConfiguration;
import de.upb.sse.jess.dependency.MavenDependencyResolver;
import de.upb.sse.jess.exceptions.AmbiguityException;
import de.upb.sse.jess.generation.StubGenerator;
import de.upb.sse.jess.inference.InferenceEngine;
import de.upb.sse.jess.model.ImportContext;
import de.upb.sse.jess.model.ResolutionInformation;
import de.upb.sse.jess.model.stubs.ClassType;
import de.upb.sse.jess.stats.StubbingStats;
import de.upb.sse.jess.util.FileUtil;
import de.upb.sse.jess.util.ImportUtil;
import de.upb.sse.jess.visitors.*;
import de.upb.sse.jess.visitors.pre.InternalKeptTypeResolutionVisitor;
import de.upb.sse.jess.visitors.pre.InternalResolutionVisitor;
import de.upb.sse.jess.visitors.pre.PreSlicingVisitor;
import de.upb.sse.jess.visitors.slicing.SlicingVisitor;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Jess {
    public static final String SRC_OUTPUT = "gen";
    public static final String CLASS_OUTPUT = "output";
    public static final String JAR_DIRECTORY = "jars";
    private static JavaSymbolSolver symbolSolver;

    private CompilationUnit cleanRoot;
    @Getter private final JessConfiguration config;
    @Getter private final StubbingStats stubbingStats = new StubbingStats();
    private final List<String> packageRoots = new ArrayList<>();
    private final CombinedTypeSolver combinedTypeSolver;

    public Jess() {
        this(new JessConfiguration(), Collections.emptyList(), Collections.emptyList());
    }

    public Jess(Collection<String> packageRoots, Collection<String> jars) {
        this(new JessConfiguration(), packageRoots, jars);
    }

    public Jess(JessConfiguration config, Collection<String> packageRoots) {
        this(config, packageRoots, MavenDependencyResolver.getJars());
    }

    public Jess(JessConfiguration config, Collection<String> packageRoots, Collection<String> jars) {
        this.config = config;

        ReflectionTypeSolver reflectiveSolver = new ReflectionTypeSolver();
        combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(reflectiveSolver);

        for (String packageRoot : packageRoots) {
            JavaParserTypeSolver javaSolver = new JavaParserTypeSolver(Paths.get(packageRoot));
            combinedTypeSolver.add(javaSolver);
            this.packageRoots.add(packageRoot);
        }

        for (String jar : jars) {
            try {
                JarTypeSolver jarSolver = new JarTypeSolver(Paths.get(jar));
                combinedTypeSolver.add(jarSolver);
            } catch (IOException e) {
                System.err.println("Could not load JarTypeSolver for " + jar);
            }
        }

        symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
    }

    public int parse(String targetClass) {
        return this.parse(targetClass, CLASS_OUTPUT);
    }

    public int parse(String targetClass, String classOutput) {
        try {
            CompilationUnit root = getCompilationUnit(targetClass);
            if (this.cleanRoot == null) {
                this.cleanRoot = root;
            }
            root = this.cleanRoot;

            // Check for resolvable and unresolvable method/field and type usages
            Annotator ann = new Annotator();

            ResolutionVisitor rv = new ResolutionVisitor(ann);
            rv.visit(root, null);

            Map<String, CompilationUnit> annotatedUnits = ann.getAnnotatedUnits();
            annotatedUnits.remove(getFullyQualifiedRootName(root));

            SignatureTypeUsageVisitor ituv = new SignatureTypeUsageVisitor(ann);
            annotatedUnits.forEach((k, v) -> {
                symbolSolver.inject(v);
                ituv.visit(v, null);
            });

            // Slice away unused methods/fields and slice bodies of used methods
            Slicer slicer = new Slicer(config, getFullyQualifiedRootName(this.cleanRoot), symbolSolver, annotatedUnits);
            Map<String, CompilationUnit> types = slicer.slice();

            // Remove artificial marker annotations
            MarkerAnnotationRemovalVisitor marv = new MarkerAnnotationRemovalVisitor();
            types.forEach((fqn, cu) -> marv.visit(cu, null));

            // Extract the sliced classes into respective files
            TypeExtractor ex = new TypeExtractor(SRC_OUTPUT);
            ex.extract(types);

            // Remove unused imports of original file (due to javadoc comments, etc.)
            root = getCleanRoot();
            marv.visit(root, null);
            ResolutionInformation usedTypes = new ResolutionInformation();
            TypeUsageVisitor tuv = new TypeUsageVisitor();
            tuv.visit(root, usedTypes);
            UnusedImportsVisitor uiv = new UnusedImportsVisitor(usedTypes.getAllTypes(), true, config.isKeepAsteriskImports());
            uiv.visit(root, null);

            // Extract original file with adjusted imports
            ex.extract(getFullyQualifiedRootName(root), root);

            // Stub unresolvable types if not disabled
            if (!config.isDisableStubbing()) {
                // Compile sliced files
                boolean successfulPreCompilation = compile(targetClass, classOutput, true);
                if (successfulPreCompilation) return 0;

                stub(SRC_OUTPUT);
            }

            // Compile sliced files
            boolean successfulCompilation = compile(targetClass, classOutput, false);
            return successfulCompilation ? 0 : 1;
        } catch (AmbiguityException e) {
          throw e;
        } catch (Throwable e) {
            if (e instanceof StackOverflowError) {
                System.err.println("Stackoverflow");
            } else {
                e.printStackTrace();
            }

            if (config.isExitOnParsingFail()) System.exit(1);
            return 2;
        } finally {
            JavaParserFacade.clearInstances();
        }
    }

    public void preSlice(String targetClass, List<String> methodsToKeep, List<String> keepClinit, List<String> keepInit) throws IOException {
        Path targetClassPath = Paths.get(targetClass);

        ParserConfiguration parserConfig = new ParserConfiguration();
        parserConfig.setSymbolResolver(symbolSolver);
        JavaParser jp = new JavaParser(parserConfig);

        ParseResult<CompilationUnit> parseResult = jp.parse(targetClassPath);
        CompilationUnit root = parseResult.getResult().get();

        Annotator annotator = new Annotator();
        annotator.keep(root);
        InternalResolutionVisitor irv = new InternalResolutionVisitor(annotator, methodsToKeep, keepClinit, keepInit, config.isLooseSignatureMatching());
        irv.visit(root, null);

        InternalKeptTypeResolutionVisitor iktrv = new InternalKeptTypeResolutionVisitor(annotator);
        iktrv.visit(root, null);

        symbolSolver.inject(root);
        PreSlicingVisitor psv = new PreSlicingVisitor(getFullyQualifiedRootName(root), annotator);
        psv.visit(root, null);

        SlicingVisitor sv = new SlicingVisitor(getFullyQualifiedRootName(root), true);
        sv.visit(root, null);

        this.cleanRoot = root;
    }

    public void preSlice(String targetClass, List<String> methodToKeep) throws IOException {
        preSlice(targetClass, methodToKeep, Collections.emptyList(), Collections.emptyList());
    }

    private void stub(String srcOutput) throws IOException, AmbiguityException {
        Map<String, ClassType> stubClasses = new HashMap<>();
        List<ImportContext> asteriskImports = new ArrayList<>();

        InferenceEngine inferenceEngine = new InferenceEngine(config.isFailOnAmbiguity());
        UnresolvableTypeVisitor utv = new UnresolvableTypeVisitor(combinedTypeSolver, inferenceEngine, packageRoots, config.isFailOnAmbiguity());

        List<String> generatedSrcFiles = FileUtil.getAllJavaFiles(srcOutput);
        for (String srcFile : generatedSrcFiles) {
            // skip the generated annotation files
            if (srcFile.endsWith(Annotator.KEEP_ALL_ANNOTATION + ".java")) continue;
            if (srcFile.endsWith(Annotator.TARGET_METHOD_ANNOTATION + ".java")) continue;

            CompilationUnit cu = getCompilationUnit(srcFile);
            symbolSolver.inject(cu);
            utv.visit(cu, stubClasses);

            asteriskImports.addAll(ImportUtil.getAsteriskImportNames(cu));
        }

        StubGenerator stubGen = new StubGenerator(srcOutput, stubbingStats);
        stubGen.generate(stubClasses.values());
        stubGen.generatePackages(asteriskImports);
    }

    private boolean compile(String targetClass, String classOutput, boolean silentCompilation) {
        CompilerInvoker compiler = new CompilerInvoker(config.getTargetVersion(), silentCompilation);
        boolean successfulCompilation = compiler.compileFile(SRC_OUTPUT, classOutput);

        if (successfulCompilation) {
            System.out.println("Successful compilation");
        } else {
            if (!silentCompilation) {
                System.out.println("Compilation failed for file: " + targetClass);
                if (config.isExitOnCompilationFail()) System.exit(1);
            }
        }

        return successfulCompilation;
    }

    private void generatePackages(String srcOutput) throws IOException {
        List<ImportContext> asteriskImports = new ArrayList<>();
        List<String> generatedSrcFiles = FileUtil.getAllJavaFiles(srcOutput);
        for (String srcFile : generatedSrcFiles) {
            // skip the generated annotation files
            if (srcFile.endsWith(Annotator.KEEP_ALL_ANNOTATION + ".java")) continue;

            CompilationUnit cu = getCompilationUnit(srcFile);
            asteriskImports.addAll(ImportUtil.getAsteriskImportNames(cu));
        }

        StubGenerator stubGen = new StubGenerator(srcOutput);
        stubGen.generatePackages(asteriskImports);
    }

    private CompilationUnit getCompilationUnit(String targetClass) throws IOException {
        Path targetClassPath = Paths.get(targetClass);

        ParserConfiguration parserConfig = new ParserConfiguration();
        parserConfig.setSymbolResolver(symbolSolver);
        JavaParser jp = new JavaParser(parserConfig);

        ParseResult<CompilationUnit> parseResult = jp.parse(targetClassPath);
        return parseResult.getResult().get();
    }

    private String getFullyQualifiedRootName(CompilationUnit cu) {
        return (String) cu.findFirst(TypeDeclaration.class).get().getFullyQualifiedName().get();
    }

    private CompilationUnit getCleanRoot() {
        return this.cleanRoot.clone();
    }

    public static void inject(Node n) {
        Optional<CompilationUnit> compilationUnitOpt = n.findCompilationUnit();
        if (compilationUnitOpt.isEmpty()) return;

        symbolSolver.inject(compilationUnitOpt.get());
    }

}
