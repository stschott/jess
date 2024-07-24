package de.upb.sse.jess.finder;

import de.upb.sse.jess.util.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackageFinder {
    private final static String PACKAGE_REGEX = "package\\s+([\\d|\\w|.]+)\\s*;";
    private final static Pattern PACKAGE_PATTERN = Pattern.compile(PACKAGE_REGEX);

    public static Set<String> findPackageRoots(String dir) {
        return findPackageRoots(dir, true);
    }

    public static Set<String> findPackageRoots(String dir, boolean blacklistEnabled) {
        Set<String> packages = new HashSet<>();
        List<String> allJavaFiles = FileUtil.getAllJavaFiles(dir);

        for (String javaFile : allJavaFiles) {
            try {
                String packageName = getPackageRoot(javaFile);
                if (packageName == null) continue;
                if (blacklistEnabled && isBlacklisted(packageName)) continue;

                packages.add(packageName);
            } catch (IOException ignored) {}
        }

        return packages;
    }

    private static String getPackageRoot(String javaFile) throws IOException {
        String packageDec = findPackage(javaFile);
        if (packageDec == null) return null;

        Matcher m = PACKAGE_PATTERN.matcher(packageDec);
        if (!m.find()) return null;
        if (m.group(1) == null) return null;

        String packageName = m.group(1);
        String packageSubPath = packageName.replace(".", File.separator);
        int packageIndex = javaFile.lastIndexOf(packageSubPath);
        if (packageIndex == -1) return null;
        return javaFile.substring(0, packageIndex);
    }

    private static String findPackage(String filePath) throws IOException {
        Path path = Path.of(filePath);

        boolean commentScope = false;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();

                if (trimmedLine.startsWith("package")) return trimmedLine;
                if (trimmedLine.startsWith("/*")) commentScope = true;
                if (!commentScope && !trimmedLine.startsWith("//") && !trimmedLine.startsWith("*") && !trimmedLine.isEmpty()) {
                    break;
                }

                if (commentScope) {
                    int closeCommentIndex = trimmedLine.indexOf("*/");
                    int openCommentIndex = trimmedLine.lastIndexOf("/*");
                    if (closeCommentIndex > -1 && openCommentIndex < closeCommentIndex) commentScope = false;
                }
            }
            return null;
        }
    }

    private static boolean isBlacklisted(String packagePath) {
        String lcPackagePath = packagePath.toLowerCase();
        if (lcPackagePath.contains("test" + File.separator)) return true;
        return false;
    }
}
