package de.upb.sse.jess.stubbing;

import de.upb.sse.jess.Jess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnnotationTests {
    private static Jess jess;

    @BeforeEach
    void setupTests() {
        jess = new Jess();
    }

    @Test
    @DisplayName("Marker annotation")
    void annotationTest1() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/annotations/Annotation1.java"));
    }

    @Disabled
    @Test
    @DisplayName("Single-value annotation")
    void annotationTest2() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/annotations/Annotation2.java"));
    }

    @Disabled
    @Test
    @DisplayName("Multi-value annotation")
    void annotationTest3() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/annotations/Annotation3.java"));
    }
}
