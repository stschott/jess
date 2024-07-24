package de.upb.sse.jess.util;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.VoidType;

import java.util.Optional;

public class SlicingUtil {

    public static MethodDeclaration emptyMethod(MethodDeclaration md) {
        Optional<BlockStmt> body = md.getBody();
        if (body.isPresent()) {
            BlockStmt emptyBody = new BlockStmt();
            // If return type is not void, add a return statement
            if (!(md.getType() instanceof VoidType)){
                ReturnStmt returnStmt = new ReturnStmt();
                returnStmt.setExpression(SlicingUtil.getGenericReturnExpression(md.getType().asString()));
                emptyBody.addStatement(returnStmt);
            }
            md.setBody(emptyBody);
        }

        return md;
    }

    public static Expression getGenericReturnExpression(String type) {
        Expression returnExpression;

        switch (type) {
            case "void": returnExpression = null;
            case "byte":
            case "short":
            case "long":
            case "float":
            case "double":
            case "int": returnExpression = new IntegerLiteralExpr("0"); break;
            case "char": returnExpression = new CharLiteralExpr('a'); break;
            case "boolean": returnExpression = new BooleanLiteralExpr(true); break;
            case "String":
            case "java.lang.String": returnExpression = new StringLiteralExpr("empty"); break;
//            default: returnExpression = createRefExpression(type);
            default: returnExpression = new NullLiteralExpr();
        }

        return returnExpression;
    }


}
