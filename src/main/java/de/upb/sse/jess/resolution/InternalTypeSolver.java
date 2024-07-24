package de.upb.sse.jess.resolution;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InternalTypeSolver {

    public static ResolvedMethodDeclaration tryResolve(MethodCallExpr exp) {
        if (exp.getScope().isPresent()) {
            Expression scope = exp.getScope().get();
            if (!scope.isThisExpr()) return null;
        }

        try {
            List<String> resolvedArgumentTypes = exp.getArguments().stream()
                    .map(arg -> {
                        try {
                            return arg.calculateResolvedType().erasure().toDescriptor();
                        } catch (UnsolvedSymbolException e) {
                            return e.getName();
                        }
                    })
                    .collect(Collectors.toList());

            Optional<TypeDeclaration> typeDecOpt = exp.findAncestor(TypeDeclaration.class);
            if (typeDecOpt.isEmpty()) return null;

            TypeDeclaration typeDec = typeDecOpt.get();
            Optional<MethodDeclaration> matchingMethodDecOpt = typeDec.findFirst(MethodDeclaration.class, md -> {
                if (!md.getNameAsString().equals(exp.getNameAsString())) return false;
                NodeList<Parameter> parameters = md.getParameters();
                if (parameters.size() != resolvedArgumentTypes.size()) return false;

                for (int i = 0; i < parameters.size(); i++) {
                    if (!parameters.get(i).getTypeAsString().equals(resolvedArgumentTypes.get(i))) return false;
                }

                return true;
            });

            if (matchingMethodDecOpt.isEmpty()) return null;
            return new JavaParserMethodDeclaration(matchingMethodDecOpt.get(), new CombinedTypeSolver());
        } catch (Exception e) {
            return null;
        }
    }
}
