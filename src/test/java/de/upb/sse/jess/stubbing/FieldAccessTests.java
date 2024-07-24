package de.upb.sse.jess.stubbing;

import de.upb.sse.jess.Jess;
import de.upb.sse.jess.exceptions.AmbiguityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FieldAccessTests {
    private static Jess jess;

    @BeforeEach
    void setupTests() {
        jess = new Jess();
    }

    @Test
    @DisplayName("Simple field access")
    void fieldAccess1() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/fieldaccess/FieldAccess1.java"));
    }

    @Test
    @DisplayName("Branching field access")
    void fieldAccess2() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/fieldaccess/FieldAccess2.java"));
    }

    @Test
    @DisplayName("Unused field access")
    void fieldAccess3() {
        assertThrows(AmbiguityException.class, () -> jess.parse("src/test/resources/stubbing/fieldaccess/FieldAccess3.java"));
    }

    @Test
    @DisplayName("Field access in field initialization")
    void fieldAccess4() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/fieldaccess/FieldAccess4.java"));
    }

    @Test
    @DisplayName("Indirect field access")
    void fieldAccess5() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/fieldaccess/FieldAccess5.java"));
    }

    @Test
    @DisplayName("Array field access")
    void fieldAccess6() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/fieldaccess/FieldAccess6.java"));
    }

    @Test
    @DisplayName("Field access assigned to array")
    void fieldAccess7() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/fieldaccess/FieldAccess7.java"));
    }

    @Test
    @DisplayName("Field access used as index for array access")
    void fieldAccess8() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/fieldaccess/FieldAccess8.java"));
    }
}
