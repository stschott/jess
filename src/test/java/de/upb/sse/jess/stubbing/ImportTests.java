package de.upb.sse.jess.stubbing;

import de.upb.sse.jess.Jess;
import de.upb.sse.jess.exceptions.AmbiguityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ImportTests {
    private static Jess jess;

    @BeforeEach
    void setupTests(){
        jess = new Jess();
    }

    @Test
    @DisplayName("Imports 1")
    void imports1() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/imports/Imports1.java"));
    }

    @Test
    @DisplayName("Imports 2")
    void imports2() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/imports/Imports2.java"));
    }

    @Test
    @DisplayName("Imports 3")
    void imports3() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/imports/Imports3.java"));
    }

    @Test
    @DisplayName("Imports 4")
    void imports4() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/imports/Imports4.java"));
    }

    @Test
    @DisplayName("Imports 5")
    void imports5() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/imports/Imports5.java"));
    }

    @Test
    @DisplayName("Imports 6")
    void imports6() {
        assertThrows(AmbiguityException.class, () -> jess.parse("src/test/resources/stubbing/imports/Imports6.java"));
    }

    @Test
    @DisplayName("Imports 6: ambiguity fail disabled")
    void imports6fail() {
        jess.getConfig().setFailOnAmbiguity(false);
        assertEquals(0, jess.parse("src/test/resources/stubbing/imports/Imports6.java"));
    }
}
