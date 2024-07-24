package de.upb.sse.jess.slicing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import de.upb.sse.jess.Jess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InitTests extends AbstractSlicingTests {
    private static Jess jess;

    @BeforeEach
    void setupTests() {
        jess = new Jess();
    }

    @Test
    @DisplayName("Non-nested init field")
    void init1() throws IOException {
        jess.preSlice("src/test/resources/slicing/init/Init1.java", Collections.emptyList(), Collections.emptyList(), List.of("Init1"));
        assertEquals(0, jess.parse("src/test/resources/slicing/init/Init1.java"));
        CompilationUnit compilationUnit = parseFile("gen/Init1.java");
        assertEquals(1, compilationUnit.findAll(FieldDeclaration.class, fd -> !fd.isStatic()).size());
    }

    @Test
    @DisplayName("Nested init field")
    void init2() throws IOException {
        jess.preSlice("src/test/resources/slicing/init/Init2.java", Collections.emptyList(), Collections.emptyList(), List.of("Init2.Inner"));
        assertEquals(0, jess.parse("src/test/resources/slicing/init/Init2.java"));
        CompilationUnit compilationUnit = parseFile("gen/Init2.java");
        assertEquals(1, compilationUnit.findAll(FieldDeclaration.class, fd -> !fd.isStatic()).size());
    }

    @Test
    @DisplayName("Non-nested clinit field")
    void init3() throws IOException {
        jess.preSlice("src/test/resources/slicing/init/Init3.java", Collections.emptyList(), List.of("Init3"), Collections.emptyList());
        assertEquals(0, jess.parse("src/test/resources/slicing/init/Init3.java"));
        CompilationUnit compilationUnit = parseFile("gen/Init3.java");
        assertEquals(1, compilationUnit.findAll(FieldDeclaration.class, FieldDeclaration::isStatic).size());
    }

    @Test
    @DisplayName("Nested clinit field")
    void init4() throws IOException {
        jess.preSlice("src/test/resources/slicing/init/Init4.java", Collections.emptyList(), List.of("Init4.Inner"), Collections.emptyList());
        assertEquals(0, jess.parse("src/test/resources/slicing/init/Init4.java"));
        CompilationUnit compilationUnit = parseFile("gen/Init4.java");
        assertEquals(1, compilationUnit.findAll(FieldDeclaration.class, FieldDeclaration::isStatic).size());
    }

    @Test
    @DisplayName("Nested and non-nested static initializers")
    void init5() throws IOException {
        jess.preSlice("src/test/resources/slicing/init/Init5.java", Collections.emptyList(), List.of("Init5", "Init5.Inner"), Collections.emptyList());
        assertEquals(0, jess.parse("src/test/resources/slicing/init/Init5.java"));
        CompilationUnit compilationUnit = parseFile("gen/Init5.java");
        assertEquals(3, compilationUnit.findAll(InitializerDeclaration.class, InitializerDeclaration::isStatic).size());
    }

    @Test
    @DisplayName("Nested and non-nested non-static initializers")
    void init6() throws IOException {
        jess.preSlice("src/test/resources/slicing/init/Init6.java", Collections.emptyList(), Collections.emptyList(), List.of("Init6", "Init6.OtherInner"));
        assertEquals(0, jess.parse("src/test/resources/slicing/init/Init6.java"));
        CompilationUnit compilationUnit = parseFile("gen/Init6.java");
        assertEquals(2, compilationUnit.findAll(InitializerDeclaration.class, id -> !id.isStatic()).size());
    }
}

