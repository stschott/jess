package de.upb.sse.jess.util;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.Optional;

public class TraversalUtil {

    public static MethodDeclaration getDeclaringMethod(Node node) {
        Node currNode = node;
        while (currNode != null) {
            if (currNode instanceof MethodDeclaration) {
                return (MethodDeclaration) currNode;
            }
            Optional<Node> parentNodeOpt = currNode.getParentNode();
            if (parentNodeOpt.isEmpty()) return null;

            currNode = parentNodeOpt.get();
        }
        return null;
    }

    public static ClassOrInterfaceDeclaration getDeclaringClass(Node node) {
        Node currNode = node;
        while (currNode != null) {
            if (currNode instanceof ClassOrInterfaceDeclaration) {
                return (ClassOrInterfaceDeclaration) currNode;
            }
            Optional<Node> parentNodeOpt = currNode.getParentNode();
            if (parentNodeOpt.isEmpty()) return null;

            currNode = parentNodeOpt.get();
        }
        return null;
    }

    public static boolean isStatic(NodeList<Modifier> modifiers) {
        return modifiers.contains(Modifier.staticModifier());
    }
}
