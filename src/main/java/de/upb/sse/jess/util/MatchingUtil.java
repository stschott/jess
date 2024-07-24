package de.upb.sse.jess.util;

import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import de.upb.sse.jess.resolution.RobustResolver;

public class MatchingUtil {

    public static boolean areSignaturesMatching(ResolvedMethodDeclaration m1, ResolvedMethodDeclaration m2) {
        if (!m1.getName().equals(m2.getName())) return false;
        if (m1.getNumberOfParams() != m2.getNumberOfParams()) return false;

        for (int i = 0; i < m2.getNumberOfParams(); i++) {
            ResolvedParameterDeclaration param1 = m1.getParam(i);
            ResolvedParameterDeclaration param2 = m2.getParam(i);

            if (!areParametersMatching(param1, param2)) return false;
        }

        return true;
    }

    public static boolean areParametersMatching(ResolvedParameterDeclaration param1, ResolvedParameterDeclaration param2) {
        ResolvedType type1 = RobustResolver.tryResolveParamType(param1);
        ResolvedType type2 = RobustResolver.tryResolveParamType(param2);

        if (type1 == null || type2 == null) return false;

        if (type1.isTypeVariable() && type2.isTypeVariable()) return true;
        if (type1.isTypeVariable() && !type2.isTypeVariable()) return areMixedVariablesMatching(type1.asTypeVariable(), type2);
        if (!type1.isTypeVariable() && type2.isTypeVariable()) return areMixedVariablesMatching(type2.asTypeVariable(), type1);

        return type1.describe().equals(type2.describe());
    }

    public static boolean areTypeVariablesMatching(ResolvedTypeVariable tv1, ResolvedTypeVariable tv2) {
        return tv1.equals(tv2);
    }

    public static boolean areMixedVariablesMatching(ResolvedTypeVariable tv1, ResolvedType typ2) {
        return true;
    }

    public static boolean areResolvedTypesMatching(ResolvedReferenceType type1, ResolvedReferenceType type2) {
        return type1.equals(type2);
    }

    public static boolean trySignatureMatching(ResolvedMethodDeclaration rmd1, ResolvedMethodDeclaration rmd2) {
        try {
            return rmd1.getSignature().equals(rmd2.getSignature());
        } catch (Exception e) {
            return false;
        }
    }
}
