package com.github.safetestspace.testspace.discovery;

import com.github.safetestspace.testspace.formatting.TestNameFormatter;
import com.github.safetestspace.testspace.settings.TestSpaceSettings;

import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TestNameFinder {
    public record TestName(PsiIdentifier identifier, String display) {
    }

    private TestNameFinder() {
    }

    /**
     * Returns test names whose display changes and is not blank.
     */
    public static List<TestName> find(@NotNull PsiElement root, TestSpaceSettings.Options options) {
        if (!options.enabled || (!options.replaceUnderscores && !options.splitCamelCase)) {
            return List.of();
        }
        List<TestName> names = new ArrayList<>();
        root.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitMethod(@NotNull PsiMethod method) {
                TestName name = testName(method, options);
                if (name != null) {
                    names.add(name);
                }
                super.visitMethod(method);
            }
        });
        return names;
    }

    private static @Nullable TestName testName(PsiMethod method, TestSpaceSettings.Options options) {
        PsiIdentifier identifier = method.getNameIdentifier();
        if (method.isConstructor() || identifier == null || identifier.getTextLength() < 2
                || !TestMethods.isTest(method)) {
            return null;
        }
        String original = identifier.getText();
        Set<String> protectedNames = options.preservedWords();
        if (options.splitCamelCase) {
            protectedNames.addAll(referencedNames(method));
        }
        String display = TestNameFormatter.format(
                original, options.replaceUnderscores, options.splitCamelCase, protectedNames);
        if (original.equals(display) || display.isBlank()) {
            return null;
        }
        return new TestName(identifier, display);
    }

    private static Set<String> referencedNames(PsiMethod method) {
        PsiCodeBlock body = method.getBody();
        if (body == null) {
            return Set.of();
        }
        Set<String> names = new HashSet<>();
        body.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitReferenceElement(@NotNull PsiJavaCodeReferenceElement reference) {
                addName(reference.getReferenceName());
                super.visitReferenceElement(reference);
            }

            @Override
            public void visitReferenceExpression(@NotNull PsiReferenceExpression expression) {
                addName(expression.getReferenceName());
                super.visitReferenceExpression(expression);
            }

            private void addName(@Nullable String name) {
                if (name != null) {
                    names.add(name);
                }
            }
        });
        return names;
    }
}
