package de.upb.sse.jess.comparison;

import de.upb.sse.jess.annotation.Annotator;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BytecodeComparator {
    private final Path generatedPath;
    private final Path groundTruthpath;

    public BytecodeComparator(String generatedPathString, String groundTruthPathString) {
        this.generatedPath = Path.of(generatedPathString);
        this.groundTruthpath = Path.of(groundTruthPathString);
    }

    public List<MethodComparison> compareMethods(String targetClass) {
        List<Path> generatedFilePaths = getAllFiles(generatedPath);
        List<Path> relevantFilePaths = generatedFilePaths.stream()
                .filter(gfp -> gfp.getFileName().toString().contains(targetClass))
                .collect(Collectors.toList());

        List<MethodComparison> allComparisons = new ArrayList<>();
        for (Path relevantFilePath : relevantFilePaths) {
            List<MethodNode> allGeneratedMethods = getMethods(relevantFilePath);
            List<MethodNode> generatedMethodsToCompare = allGeneratedMethods.stream()
                    .filter(mn -> {
                        return isMethodForComparison(mn);
                    })
                    .collect(Collectors.toList());
            if (generatedMethodsToCompare.isEmpty()) continue;

            Path groudTruthPath = getCorrespondingGroundTruthPath(relevantFilePath);
            if (groudTruthPath == null) continue;

            List<MethodNode> groundTruthMethodsToCompare = getMatchingMethods(groudTruthPath, generatedMethodsToCompare);

            List<MethodComparison> comparisons = compareMethods(generatedMethodsToCompare, groundTruthMethodsToCompare);
            comparisons.forEach(comp -> comp.setClassName(targetClass));
            allComparisons.addAll(comparisons);
        }

        return allComparisons;
    }

    private List<MethodComparison> compareMethods(List<MethodNode> actualMethods, List<MethodNode> expectedMethods) {
        List<MethodComparison> comparisons = new ArrayList<>();
        for (MethodNode actualMethod : actualMethods) {
            for (MethodNode expectedMethod : expectedMethods) {
                String actualMethodDescriptor = actualMethod.name + actualMethod.desc;
                String expectedMethodDescriptor = expectedMethod.name + expectedMethod.desc;
                if (!actualMethodDescriptor.equals(expectedMethodDescriptor)) continue;

                MethodComparison comparison = compareMethods(actualMethod, expectedMethod);
                comparisons.add(comparison);
            }
        }

        return comparisons;
    }

    private MethodComparison compareMethods(MethodNode actualMethod, MethodNode expectedMethod) {
        String actualMethodContent = getStringRepresentationOfMethod(actualMethod);
        String expectedMethodContent = getStringRepresentationOfMethod(expectedMethod);

        return new MethodComparison(actualMethod.name + actualMethod.desc, actualMethodContent, expectedMethodContent);
    }

    private Path getCorrespondingGroundTruthPath(Path path) {
        Path fixedPath = Path.of(path.toString().replace(generatedPath.toString() + File.separator, ""));
        List<String> allRelevantClassFiles = getAllRelevantClassFiles(groundTruthpath.toString());
        Optional<String> correspondingClassFile = allRelevantClassFiles.stream()
                .filter(cf -> Path.of(cf).toString().endsWith(fixedPath.toString()))
                .findFirst();

        if (correspondingClassFile.isEmpty()) return null;

        return Path.of(correspondingClassFile.get());
    }

    private List<MethodNode> getMatchingMethods(Path path, List<MethodNode> referenceMethods) {
        List<String> referenceMethodDecriptors = referenceMethods.stream().map(m -> m.name + m.desc).collect(Collectors.toList());
        List<MethodNode> allMethods = getMethods(path);

        List<MethodNode> matchedMethods = new ArrayList<>();
        for (MethodNode method : allMethods) {
            String methodDescriptor = method.name + method.desc;
            if (!referenceMethodDecriptors.contains(methodDescriptor)) continue;

            matchedMethods.add(method);
        }
        return matchedMethods;
    }

    private List<MethodNode> getMethods(Path path) {
        try {
            InputStream is = new FileInputStream(path.toFile());
            ClassReader reader = new ClassReader(is);
            ClassNode cn = new ClassNode();
            reader.accept(cn, ClassReader.SKIP_DEBUG);
            return cn.methods;
        } catch (IOException e) {

        }
        return new ArrayList<>();
    }

    private String getStringRepresentationOfMethod(MethodNode mn) {
        Printer printer = new CodeOnlyTextifier();
        TraceMethodVisitor methodPrinter = new TraceMethodVisitor(printer);
        mn.accept(methodPrinter);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        printer.print(printWriter);
        printer.getText().clear();

        String methodContent = stringWriter.toString();
        String annotationRegex = ".*" + "TargetMethod;\\(\\) // invisible" + ".*\\R";

        return methodContent.replaceAll(annotationRegex, "").trim();
    }

    private List<MethodNode> getMethodDisjunction(List<MethodNode> methods1, List<MethodNode> methods2) {
        List<String> methodSignatures1 = methods1.stream().map(m -> m.name + m.desc).collect(Collectors.toList());
        List<String> methodSignatures2 = methods2.stream().map(m -> m.name + m.desc).collect(Collectors.toList());

        List<MethodNode> methodsDisjuction1 = methods1.stream()
                .filter(m -> !methodSignatures2.contains(m.name + m.desc))
                .collect(Collectors.toList());

        List<MethodNode> methodsDisjuction2 = methods2.stream()
                .filter(m -> !methodSignatures1.contains(m.name + m.desc))
                .collect(Collectors.toList());

        return Stream.concat(methodsDisjuction1.stream(), methodsDisjuction2.stream()).collect(Collectors.toList());
    }

    private List<Path> getAllFiles(Path path) {
        List<Path> files = new ArrayList<>();
        try {
            files = Files.find(path, 999,
                            (p, bfa) -> bfa.isRegularFile()
                    )
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }

    private boolean isMethodForComparison(MethodNode mn) {
        List<AnnotationNode> annotations = mn.invisibleAnnotations;
        if (annotations == null) return false;

        for (AnnotationNode annotation : annotations) {
            String annotationDesc = annotation.desc;
            int startIndex = annotationDesc.lastIndexOf('/') + 1;
            int endIndex = annotationDesc.lastIndexOf(';');
            String annotationName = annotationDesc.substring(startIndex, endIndex);

            if (annotationName.equals(Annotator.TARGET_METHOD_ANNOTATION)) return true;
        }

        return false;
    }

    private List<String> getAllRelevantClassFiles(String dir) {
        List<String> classFiles = new ArrayList<>();
        try {
            classFiles = Files.find(Paths.get(dir), 999,
                            (p, bfa) -> bfa.isRegularFile() &&
                                    p.getFileName().toString().matches(".*\\.class") &&
                                    !p.getFileName().toString().contains("package-info") &&
                                    !p.getFileName().toString().contains("module-info") &&
                                    !p.toString().contains("test")
                    ).map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classFiles;
    }
}
