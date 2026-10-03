package com.github.safetestspace.testspace.discovery;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.util.InheritanceUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class TestMethods {
    private static final Set<String> NAMES = Set.of(
            "org.junit.Test",
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.RepeatedTest",
            "org.junit.jupiter.api.TestFactory",
            "org.junit.jupiter.api.TestTemplate",
            "org.junit.jupiter.params.ParameterizedTest",
            "org.junit.platform.commons.annotation.Testable",
            "org.testng.annotations.Test"
    );

    private TestMethods() {
    }

    static boolean isTest(PsiMethod method) {
        return isJUnit3Test(method) || Arrays.stream(method.getAnnotations())
                .anyMatch(annotation -> isTestAnnotation(annotation, new HashSet<>()));
    }

    private static boolean isJUnit3Test(PsiMethod method) {
        if (!method.getName().startsWith("test")
                || !method.hasModifierProperty(PsiModifier.PUBLIC)
                || !method.getParameterList().isEmpty()
                || !PsiTypes.voidType().equals(method.getReturnType())) {
            return false;
        }
        PsiClass owner = method.getContainingClass();
        return InheritanceUtil.isInheritor(owner, "junit.framework.TestCase");
    }

    /**
     * Follows meta-annotations; visited names prevent cycles.
     */
    private static boolean isTestAnnotation(PsiAnnotation annotation, Set<String> visited) {
        String name = annotation.getQualifiedName();
        if (name == null || !visited.add(name)) {
            return false;
        }
        if (NAMES.contains(name)) {
            return true;
        }
        PsiClass declaration = annotation.resolveAnnotationType();
        return declaration != null && Arrays.stream(declaration.getAnnotations())
                .anyMatch(metaAnnotation -> isTestAnnotation(metaAnnotation, visited));
    }
}
