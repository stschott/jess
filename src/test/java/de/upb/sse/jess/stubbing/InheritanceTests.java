package de.upb.sse.jess.stubbing;

import de.upb.sse.jess.Jess;
import de.upb.sse.jess.configuration.JessConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class InheritanceTests {
    private static Jess jess;

    @BeforeEach
    void setupTests(){
        jess = new Jess();
    }

    @Test
    @DisplayName("Implementation of external interfaces and extension of external class")
    void inheritance1() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/inheritance/Inheritance1.java"));
    }

    @Test
    @DisplayName("Access field of super class")
    void inheritance2() {
        jess = new Jess(new JessConfiguration(), Collections.singleton("src/test/resources/stubbing/inheritance"), Collections.emptyList());
        jess.getConfig().setFailOnAmbiguity(false);
        assertEquals(0, jess.parse("src/test/resources/stubbing/inheritance/inheritance2/Inheritance2.java"));
        assertTrue(new File("gen/org/example/Log.java").exists());
    }
}
