package de.upb.sse.jess.util;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.util.Optional;

public class SignatureUtil {

    public static String getClassSignature(Node node) {
        StringBuilder builder = new StringBuilder();
        TypeDeclaration typeDec;
        if (node instanceof ClassOrInterfaceDeclaration) {
            typeDec = (ClassOrInterfaceDeclaration) node;
        } else if (node instanceof EnumDeclaration) {
          typeDec = (EnumDeclaration) node;
        } else {
            Optional<TypeDeclaration> typeDecOpt = node.findAncestor(TypeDeclaration.class);
            if (typeDecOpt.isEmpty()) throw new RuntimeException("node without type declaration: " + node);
            typeDec = typeDecOpt.get();
        }

        Optional<TypeDeclaration> parentTypeOpt = typeDec.findAncestor(TypeDeclaration.class);
        while (parentTypeOpt.isPresent()) {
            TypeDeclaration parentType = parentTypeOpt.get();
            builder.append(parentType.getNameAsString());
            builder.append(".");
            parentTypeOpt = parentType.findAncestor(TypeDeclaration.class);
        }
        builder.append(typeDec.getNameAsString());
        return builder.toString();
    }

    public static String getPartialCallableSignature(CallableDeclaration<?> cd) {
        return getDeclaringClass(cd).substring(1) + "." + cd.getSignature();
    }

    private static String getDeclaringClass(Node n) {
        Optional<ClassOrInterfaceDeclaration> parentDecOpt = n.findAncestor(ClassOrInterfaceDeclaration.class);
        if (parentDecOpt.isEmpty()) return "";
        ClassOrInterfaceDeclaration parentDec = parentDecOpt.get();
        return getDeclaringClass(parentDec) + "." + parentDec.getNameAsString();
    }
}
