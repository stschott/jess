package de.upb.sse.jess.slicing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import de.upb.sse.jess.Jess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PreSlicingTests extends AbstractSlicingTests {
    private static Jess jess;

    @BeforeEach
    void setupTests() {
        jess = new Jess();
        jess.getConfig().setLooseSignatureMatching(false);
    }

    @Test
    @DisplayName("Internal reference to method with non-resolvable Parameter type")
    void pre1() throws IOException {
        jess.preSlice("src/test/resources/slicing/pre/Pre1.java", List.of("Pre1.keptMethod(SomeObject)"), Collections.emptyList(), Collections.emptyList());
        assertEquals(0, jess.parse("src/test/resources/slicing/pre/Pre1.java"));
        CompilationUnit compilationUnit = parseFile("gen/Pre1.java");
        assertEquals(2, compilationUnit.findAll(MethodDeclaration.class).size());
    }

    @Test
    @DisplayName("Internal reference to field with non-resolvable type")
    void pre2() throws IOException {
        jess.preSlice("src/test/resources/slicing/pre/Pre2.java", List.of("Pre2.keptMethod(SomeObject)"), Collections.emptyList(), Collections.emptyList());
        assertEquals(0, jess.parse("src/test/resources/slicing/pre/Pre2.java"));
        CompilationUnit compilationUnit = parseFile("gen/Pre2.java");
        assertEquals(1, compilationUnit.findAll(FieldDeclaration.class).size());
    }
}