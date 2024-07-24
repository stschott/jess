package de.upb.sse.jess.visitors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.visitor.ModifierVisitor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnusedImportsVisitor extends ModifierVisitor<Void> {
    private final Set<String> usedTypes;
    private final boolean keepAsteriskImports;
    private final boolean isTargetClass;

    public UnusedImportsVisitor(Set<String> usedTypes) {
        this(usedTypes, false, false);

    }

    public UnusedImportsVisitor(Set<String> usedTypes, boolean keepAsteriskImports) {
        this(usedTypes, keepAsteriskImports, false);
    }

    public UnusedImportsVisitor(Set<String> usedTypes, boolean keepAsteriskImports, boolean isTargetClass) {
        this.usedTypes = usedTypes;
        this.keepAsteriskImports = keepAsteriskImports;
        this.isTargetClass = isTargetClass;
    }

    @Override
    public ImportDeclaration visit(ImportDeclaration id, Void arg) {
        super.visit(id, arg);

        if (this.isTargetClass && id.isStatic()) {
            String importName = id.getNameAsString();
            String importedMember = importName.substring(importName.lastIndexOf('.') + 1);

            Optional<CompilationUnit> cuOpt = id.findCompilationUnit();
            if (cuOpt.isEmpty()) return id;

            CompilationUnit cu = cuOpt.get();
            List<NameExpr> nameExpressions = cu.findAll(NameExpr.class);
            List<MethodCallExpr> methodCalls = cu.findAll(MethodCallExpr.class);

            List<String> namedEntities = Stream.concat(
                    nameExpressions.stream().map(NodeWithSimpleName::getNameAsString),
                    methodCalls.stream().map(NodeWithSimpleName::getNameAsString)
            ).collect(Collectors.toList());

            boolean usesStaticImport = namedEntities.stream().anyMatch(ne -> ne.equals(importedMember));

            if (usesStaticImport) return id;
            return null;
        }

        if (id.isAsterisk()) {
            // wildcard import
            Set<String> packageNames = usedTypes
                    .stream()
                    .filter(type -> type.lastIndexOf(".") > -1)
                    .map(type -> type.substring(0, type.lastIndexOf(".")))
                    .collect(Collectors.toSet());

            if (!keepAsteriskImports && !packageNames.contains(id.getNameAsString())) return null;
        } else {
            if (!usedTypes.contains(id.getNameAsString()) && !usedTypes.contains(getImportClassName(id.getNameAsString()))) return null;
        }
        return id;
    }


    private String getImportClassName(String importName) {
        String[] splitImport = importName.split("\\.");
        return splitImport[splitImport.length - 1];
    }
}
