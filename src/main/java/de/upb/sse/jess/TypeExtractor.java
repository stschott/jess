package de.upb.sse.jess;

import com.github.javaparser.ast.CompilationUnit;
import de.upb.sse.jess.annotation.Annotations;
import de.upb.sse.jess.annotation.Annotator;
import de.upb.sse.jess.generation.unknown.UnknownType;
import de.upb.sse.jess.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class TypeExtractor {
    private final String output;

    public TypeExtractor(String output) {
        this.output = output;
        cleanUp();
    }

    public void extract(Map<String, CompilationUnit> types) {
        for (Map.Entry<String, CompilationUnit> entry : types.entrySet()) {
            extract(entry.getKey(), entry.getValue());
        }
        generateUnknownType();
    }

    public void extract(String fqn, CompilationUnit cu) {
        try {
            // add import to unknown package in case there is an unknown
            cu.addImport(UnknownType.PACKAGE, false, true);

            String fullyQualifiedPath = fqn.replace(".", "/") + ".java";
            Path outputPath = Paths.get(this.output, fullyQualifiedPath);
            outputPath.getParent().toFile().mkdirs();
            Files.writeString(outputPath, cu.toString(), StandardCharsets.UTF_8);
            generateMarkerAnnotation(fqn);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cleanUp() {
        File outputFile = new File(this.output);
        if (!outputFile.exists()) return;
        FileUtil.deleteRecursively(outputFile);
    }

    private void generateMarkerAnnotation(String fqn) {
        String packagePath = "";
        String annotationTemplate;
        if (fqn.lastIndexOf(".") != -1) {
            packagePath = fqn.substring(0, fqn.lastIndexOf("."));
            annotationTemplate = Annotations.TARGET_METHOD_TEMPLATE.replace("{{package}}", packagePath);
        } else {
            annotationTemplate = Annotations.TARGET_METHOD_TEMPLATE.substring(Annotations.TARGET_METHOD_TEMPLATE.indexOf('\n') + 1);
        }
        String fullyQualifiedPath = packagePath.replace(".", "/");
        Path outputPath = Paths.get(this.output, fullyQualifiedPath, Annotator.TARGET_METHOD_ANNOTATION + ".java");
        try {
            Files.writeString(outputPath, annotationTemplate, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateUnknownType() {
        String fullyQualifiedPath = UnknownType.PACKAGE.replace(".", "/");
        Path outputPath = Paths.get(this.output, fullyQualifiedPath, UnknownType.CLASS + ".java");
        try {
            outputPath.getParent().toFile().mkdirs();
            Files.writeString(outputPath, UnknownType.TEMPLATE, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
