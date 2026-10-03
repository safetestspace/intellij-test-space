package com.github.safetestspace.testspace.java;

import com.github.safetestspace.testspace.discovery.TestNameProvider;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceExpression;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class JavaTestNameProvider implements TestNameProvider {
    @Override
    public boolean supports(FileType fileType) {
        return fileType == JavaFileType.INSTANCE;
    }

    @Override
    public List<Declaration> find(PsiElement root, boolean collectReferences) {
        List<Declaration> declarations = new ArrayList<>();
        root.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitMethod(@NotNull PsiMethod method) {
                PsiIdentifier identifier = method.getNameIdentifier();
                if (!method.isConstructor() && identifier != null && TestMethods.isTest(method)) {
                    declarations.add(new Declaration(identifier,
                            collectReferences ? referencedNames(method) : Set.of()));
                }
                super.visitMethod(method);
            }
        });
        return declarations;
    }

    private static Set<String> referencedNames(PsiMethod method) {
        PsiCodeBlock body = method.getBody();
        if (body == null) return Set.of();
        Set<String> names = new HashSet<>();
        body.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitReferenceElement(@NotNull PsiJavaCodeReferenceElement reference) {
                if (reference.getReferenceName() != null) names.add(reference.getReferenceName());
                super.visitReferenceElement(reference);
            }

            @Override
            public void visitReferenceExpression(@NotNull PsiReferenceExpression expression) {
                if (expression.getReferenceName() != null) names.add(expression.getReferenceName());
                super.visitReferenceExpression(expression);
            }
        });
        return names;
    }
}
