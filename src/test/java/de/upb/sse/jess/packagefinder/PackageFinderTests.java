package de.upb.sse.jess.packagefinder;

import de.upb.sse.jess.finder.PackageFinder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PackageFinderTests {

    @Test
    void packageTest1() {
        Set<String> packageRoots = PackageFinder.findPackageRoots("src/test/resources/packagefinder", false);
        assertEquals(2, packageRoots.size());
    }
}
