package de.upb.sse.jess.stubbing;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import de.upb.sse.jess.Jess;
import de.upb.sse.jess.generation.unknown.UnknownType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnknownTypeTests {
    private static Jess jess;

    @BeforeEach
    void setupTests() {
        jess = new Jess();
        jess.getConfig().setFailOnAmbiguity(false);
    }

    @Test
    @DisplayName("Generate stub with unknown import")
    void unknownType1() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/unknowntype/UnknownType1.java"));
        assertTrue(new File("gen/unknown/SomeObject.java").exists());
    }

    @Test
    @DisplayName("Method is called with null literal")
    void unknownType2() throws IOException {
        assertEquals(0, jess.parse("src/test/resources/stubbing/unknowntype/UnknownType2.java"));
        CompilationUnit cu = StaticJavaParser.parse(Path.of("gen/org/example/SomeObject.java"));
        Optional<MethodDeclaration> mdOpt = cu.findFirst(MethodDeclaration.class);
        assertEquals(UnknownType.CLASS, mdOpt.get().getParameter(0).getTypeAsString());
        assertEquals("void", mdOpt.get().getTypeAsString());
    }

    @Test
    @DisplayName("Nested method calls")
    void unknownType3() throws IOException {
        assertEquals(0, jess.parse("src/test/resources/stubbing/unknowntype/UnknownType3.java"));
        CompilationUnit cu = StaticJavaParser.parse(Path.of("gen/org/example/SomeObject.java"));
        Optional<MethodDeclaration> visitMethod = cu.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("visit")).findFirst();
        Optional<MethodDeclaration> visit2Method = cu.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("visit2")).findFirst();
        assertEquals("void", visitMethod.get().getTypeAsString());
        assertEquals(UnknownType.CLASS, visitMethod.get().getParameter(0).getTypeAsString());
        assertEquals(UnknownType.CLASS, visit2Method.get().getTypeAsString());
    }

    @Test
    @DisplayName("Unknown field access")
    void unknownType4() throws IOException {
        assertEquals(0, jess.parse("src/test/resources/stubbing/unknowntype/UnknownType4.java"));
        CompilationUnit cu = StaticJavaParser.parse(Path.of("gen/org/example/SomeObject.java"));
        Optional<FieldDeclaration> fieldOpt = cu.findFirst(FieldDeclaration.class);
        assertEquals(UnknownType.CLASS, fieldOpt.get().getVariables().get(0).getTypeAsString());
    }

}
