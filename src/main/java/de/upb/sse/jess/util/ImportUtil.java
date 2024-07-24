package de.upb.sse.jess.util;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import de.upb.sse.jess.generation.unknown.UnknownType;
import de.upb.sse.jess.model.ImportContext;
import de.upb.sse.jess.model.stubs.TypeContext;
import org.checkerframework.checker.units.qual.A;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ImportUtil {

    public static List<ImportDeclaration> getRelevantImportsFromCU(CompilationUnit cu) {
        return cu.findAll(ImportDeclaration.class).stream()
                .filter(im -> !im.getNameAsString().equals(UnknownType.PACKAGE))
                .collect(Collectors.toList());
    }

    public static Collection<ImportContext> getAsteriskImportNames(CompilationUnit cu) {
        List<ImportDeclaration> imports = cu.findAll(ImportDeclaration.class);
        return imports.stream()
                .filter(ImportDeclaration::isAsterisk)
                .map(i -> new ImportContext(i.getNameAsString(), i.isStatic()))
                .collect(Collectors.toList());
    }

    public static String getPackageFromImportName(String importName) {
        int lastDotIndex = importName.lastIndexOf(".");
        if (lastDotIndex == -1) return importName;
        return importName.substring(0, lastDotIndex);
    }

    public static TypeContext getCleanTypeContext(TypeContext typeContext) {
        return new TypeContext(getCleanClassName(typeContext.getType()), typeContext.getContext());
    }

    public static String getCleanClassName(String className) {
        String cleanName = className;
        cleanName = cutoffAfterFirstCharOccurrence(cleanName, '<');
        cleanName = cutoffAfterFirstCharOccurrence(cleanName, '[');
        return cleanName;
    }

    public static String cutoffAfterFirstCharOccurrence(String string, Character cutoff) {
        int index = string.indexOf(cutoff);
        if (index != -1) {
            return string.substring(0, index);
        }
        return string;
    }

    public static String cutoffAfterLastCharOccurrence(String string, Character cutoff) {
        int index = string.lastIndexOf(cutoff);
        if (index != -1) {
            return string.substring(index + 1, string.length());
        }
        return string;
    }

    public static List<TypeContext> getGenericTypes(TypeContext typeContext) {
        return getGenericTypes(typeContext.getType()).stream()
                .map(gt -> new TypeContext(gt, typeContext.getContext()))
                .collect(Collectors.toList());
    }

    public static List<String> getGenericTypes(String type) {
        if (type == null) return Collections.emptyList();

        String fixedType = type.replaceAll("<", " ")
                .replaceAll(">", " ")
                .replaceAll("super", "")
                .replaceAll("extends", "")
                .replaceAll("\\?", "")
                .replaceAll(",", " ")
                .trim();

        String[] allTypes = fixedType.split("\\s+");
        return Arrays.asList(allTypes);
    }

    public static boolean isPrimitiveType(String type) {
        switch (type) {
            case "int" :
            case "String":
            case "java.lang.String":
            case "short":
            case "byte":
            case "double":
            case "float":
            case "char":
            case "long":
            case "boolean":
            case "void":
                return true;
            default:
                return false;
        }
    }

    public static boolean isBlacklisted(String className) {
        switch (className) {
            case "java":
            case "lang":
                return true;
            default:
                return false;
        }
    }

//    private static void getGenericTypes(String type, List<String> types) {
//        if (type == null) return;
//
//        String[] splitTypes = type.split(",");
//        for (String splitType : splitTypes) {
//            String fixType = splitType.trim();
//            int firstIndex = fixType.indexOf("<");
//
//            if (fixType.isEmpty()) return;
//
//            String genericType = fixType;
//            if (firstIndex > -1) {
//                genericType = fixType.substring(0, firstIndex).trim();
//            }
//            String fixedGenericType = genericType.replace("extends", "").replace("super", "").replace("?", "").trim();
//            if (!fixedGenericType.isEmpty()) types.add(fixedGenericType);
//
//            if (firstIndex > -1) getGenericTypes(fixType.substring(firstIndex + 1, fixType.length() - 1), types);
//        }
//    }

//    public static List<TypeContext> getGenericTypes(TypeContext typeContext) {
//        List<String> genericTypes = new ArrayList<>();
//        getGenericTypes(typeContext.getType(), genericTypes);
//
//        return genericTypes.stream().map(gt -> new TypeContext(gt, typeContext.getContext())).collect(Collectors.toList());
//    }
//
//    public static List<String> getGenericTypes(String className) {
//        List<String> genericTypes = new ArrayList<>();
//        getGenericTypes(className, genericTypes);
//        return genericTypes;
//    }

}
