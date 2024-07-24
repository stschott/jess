package de.upb.sse.jess.slicing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import de.upb.sse.jess.Jess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class FieldSlicingTests extends AbstractSlicingTests {
    private static Jess jess;

    @BeforeEach
    void setupTests() {
        jess = new Jess();
    }

    @Test
    @DisplayName("Non initialized final field")
    void field1() throws IOException {
        jess.preSlice("src/test/resources/slicing/fields/Fields1.java", List.of("add()"), Collections.emptyList(), Collections.emptyList());
        assertEquals(0, jess.parse("src/test/resources/slicing/fields/Fields1.java"));
        CompilationUnit compilationUnit = parseFile("gen/Fields1.java");
        Optional<FieldDeclaration> fieldOpt = compilationUnit.findFirst(FieldDeclaration.class);
        assertTrue(fieldOpt.isPresent());
        FieldDeclaration field = fieldOpt.get();
        assertTrue(field.isFinal());
        NodeList<VariableDeclarator> variables = field.getVariables();
        for (VariableDeclarator varDec : variables) {
            assertTrue(varDec.getInitializer().isEmpty());
        }

    }

    @Test
    @DisplayName("Non literal, non final field")
    void field2() throws IOException {
        jess.preSlice("src/test/resources/slicing/fields/Fields2.java", List.of("add()"), Collections.emptyList(), Collections.emptyList());
        assertEquals(0, jess.parse("src/test/resources/slicing/fields/Fields2.java"));
        CompilationUnit compilationUnit = parseFile("gen/Fields2.java");
        Optional<FieldDeclaration> fieldOpt = compilationUnit.findFirst(FieldDeclaration.class);
        assertTrue(fieldOpt.isPresent());
        FieldDeclaration field = fieldOpt.get();
        assertFalse(field.isFinal());
        NodeList<VariableDeclarator> variables = field.getVariables();
        for (VariableDeclarator varDec : variables) {
            assertTrue(varDec.getInitializer().isEmpty());
        }

    }

    @Test
    @DisplayName("Non literal, final field")
    void field3() throws IOException {
        jess.preSlice("src/test/resources/slicing/fields/Fields3.java", List.of("add()"), Collections.emptyList(), Collections.emptyList());
        assertEquals(0, jess.parse("src/test/resources/slicing/fields/Fields3.java"));
        CompilationUnit compilationUnit = parseFile("gen/Fields3.java");
        Optional<FieldDeclaration> fieldOpt = compilationUnit.findFirst(FieldDeclaration.class);
        assertTrue(fieldOpt.isPresent());
        FieldDeclaration field = fieldOpt.get();
        assertTrue(field.isFinal());
        NodeList<VariableDeclarator> variables = field.getVariables();
        for (VariableDeclarator varDec : variables) {
            assertTrue(varDec.getInitializer().isEmpty());
        }
    }

    @Test
    @DisplayName("Final field with literal")
    void field4() throws IOException {
        jess.preSlice("src/test/resources/slicing/fields/Fields4.java", List.of("add()"), Collections.emptyList(), Collections.emptyList());
        assertEquals(0, jess.parse("src/test/resources/slicing/fields/Fields4.java"));
        CompilationUnit compilationUnit = parseFile("gen/Fields4.java");
        Optional<FieldDeclaration> fieldOpt = compilationUnit.findFirst(FieldDeclaration.class);
        assertTrue(fieldOpt.isPresent());
        FieldDeclaration field = fieldOpt.get();
        assertTrue(field.isFinal());
        NodeList<VariableDeclarator> variables = field.getVariables();
        for (VariableDeclarator varDec : variables) {
            assertTrue(varDec.getInitializer().isPresent());
            assertEquals(5, varDec.getInitializer().get().asIntegerLiteralExpr().asNumber().intValue());
        }
    }

    @Test
    @DisplayName("Static final field initialized in a static initializer")
    void field5() throws IOException {
        jess.preSlice("src/test/resources/slicing/fields/Fields5.java", List.of("add()"), Collections.emptyList(), Collections.emptyList());
        assertEquals(0, jess.parse("src/test/resources/slicing/fields/Fields5.java"));
        CompilationUnit compilationUnit = parseFile("gen/Fields5.java");
        Optional<FieldDeclaration> fieldOpt = compilationUnit.findFirst(FieldDeclaration.class);
        assertTrue(fieldOpt.isPresent());
        FieldDeclaration field = fieldOpt.get();
        assertTrue(field.isFinal() && field.isStatic());
        NodeList<VariableDeclarator> variables = field.getVariables();
        for (VariableDeclarator varDec : variables) {
            assertTrue(varDec.getInitializer().isPresent());
            assertEquals(0, varDec.getInitializer().get().asIntegerLiteralExpr().asNumber().intValue());
        }
    }

}
