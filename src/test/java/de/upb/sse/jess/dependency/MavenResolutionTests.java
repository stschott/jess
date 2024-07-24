package de.upb.sse.jess.dependency;

import de.upb.sse.jess.Jess;
import de.upb.sse.jess.finder.PomFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MavenResolutionTests {

    @BeforeEach
    void cleanup() {
        MavenDependencyResolver.cleanupJars();
    }

    private int countJars() {
        String[] filesInJarDir = new File(Jess.JAR_DIRECTORY).list();
        if (filesInJarDir == null) return 0;
        return (int) Arrays.stream(filesInJarDir).filter(f -> f.endsWith(".jar")).count();
    }

    @Test
    void pomFindingTest1() {
        Set<String> pomFiles = PomFinder.findPomFilePaths("src/test/resources/dependency/dep2");
        assertEquals(2, pomFiles.size());
    }

    @Test
    void pomFindingTest2() {
        Path pomFile = PomFinder.findClosestPomFile("src/test/resources/dependency/dep3/module-1");
        assertNotNull(pomFile);
        assertTrue(pomFile.toString().replace("\\", "/").endsWith("src/test/resources/dependency/dep3/module-1/pom.xml"));
    }

    @Test
    void mavenResolutionTest1() {
        Set<String> pomFiles = PomFinder.findPomFilePaths("src/test/resources/dependency/dep1");
        assertEquals(1, pomFiles.size());

        MavenDependencyResolver.resolveDependencies(pomFiles, true);
        assertEquals(7, countJars());
    }

    @Test
    void mavenResolutionTest2() {
        Set<String> pomFiles = PomFinder.findPomFilePaths("src/test/resources/dependency/dep3");
        assertEquals(1, pomFiles.size());

        MavenDependencyResolver.resolveDependencies(pomFiles, true);
        assertEquals(7, countJars());
    }

    @Test
    void profileExtractionTest() {
        Set<String> pomFiles = PomFinder.findPomFilePaths("src/test/resources/dependency/dep1");
        assertEquals(1, pomFiles.size());

        List<String> profiles = MavenDependencyResolver.extractProfiles(pomFiles);
        assertEquals(2, profiles.size());
    }
}
