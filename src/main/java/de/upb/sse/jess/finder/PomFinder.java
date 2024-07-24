package de.upb.sse.jess.finder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PomFinder {

    public static Set<String> findPomFilePaths(String rootDir) {
        return findPomFilePaths(rootDir, true);
    }

    public static Set<String> findPomFilePaths(String rootDir, boolean onlyOutermostPom) {
        final Set<String> pomFiles;
        Set<String> pomFilesTemp;
        try {
            pomFilesTemp = Files.find(Paths.get(rootDir), 999,
                            (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().equals("pom.xml")
                    ).map(p -> p.getParent().toString())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            pomFilesTemp = new HashSet<>();
        }
        pomFiles = pomFilesTemp;

        Set<String> topLevelPomFiles = new HashSet<>();
        for (String pomFile : pomFiles) {
            if (onlyOutermostPom && pomFiles.stream().anyMatch(p -> !p.equals(pomFile) && pomFile.startsWith(p))) continue;
            topLevelPomFiles.add(pomFile);
        }

        return topLevelPomFiles;
    }

    public static Path findClosestPomFile(String start) {
        Path path = Paths.get(start);
        if (Files.isRegularFile(path)) {
            path = path.getParent();
        }
        while (path != null) {
            File[] contents = path.toFile().listFiles();
            if (contents == null) return null;

            for (File file : contents) {
                if (file.getName().endsWith("pom.xml")) return file.toPath();
            }
            path = path.getParent();
        }
        return null;
    }

}
