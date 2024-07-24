package de.upb.sse.jess.slicing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractSlicingTests {

    protected CompilationUnit parseFile(String filepath) throws IOException {
        Path targetClassPath = Paths.get(filepath);
        JavaParser jp = new JavaParser();
        ParseResult<CompilationUnit> parseResult = jp.parse(targetClassPath);
        return parseResult.getResult().get();
    }
}
