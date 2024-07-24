package de.upb.sse.jess.util;

import com.github.javaparser.ast.CompilationUnit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtil {

    public static void deleteRecursively(File file) {
        File[] allContents = file.listFiles();
        if (allContents != null) {
            for (File f : allContents) {
                deleteRecursively(f);
            }
        }
        file.delete();
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

    public static void printCompilationUnit(CompilationUnit cu, Path outputPath) {
        File outputFile = outputPath.toFile();
        outputFile.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(cu.toString());
        } catch (IOException e) {
            System.out.println("Failed to generate file: " + outputPath);
            e.printStackTrace();
        }
    }

    public static void createPackageDirectory(String outputDir, String packageName) {
        Path outputPath = Path.of(outputDir, packageName.replace(".", "/"));
        File packageFile = outputPath.toFile();
        packageFile.mkdirs();
        if (packageFile.list().length > 0) return;

        File placeholderFile = outputPath.resolve("Empty.java").toFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(placeholderFile))) {
            writer.write("package " + packageName + ";\nclass Empty {}");
        } catch (IOException e) {
            System.out.println("Failed to generate file: " + outputPath);
            e.printStackTrace();
        }
    }

    public static void createStaticPackageDirectory(String outputDir, String fullName) {
        int dotIndex = fullName.lastIndexOf(".");
        if (dotIndex < 0) return;
        String packagePath = fullName.substring(0, dotIndex);
        String className = fullName.substring(dotIndex + 1);

        Path outputPath = Path.of(outputDir, packagePath.replace(".", "/"));
        File packageFile = outputPath.toFile();
        packageFile.mkdirs();

        File placeholderFile = outputPath.resolve(className + ".java").toFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(placeholderFile))) {
            writer.write("package " + packagePath + ";\npublic class "+ className + " {}");
        } catch (IOException e) {
            System.out.println("Failed to generate file: " + outputPath);
            e.printStackTrace();
        }
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
}
