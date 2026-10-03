package com.github.safetestspace.testspace.discovery;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiElement;

import java.util.List;
import java.util.Set;

public interface TestNameProvider {
    ExtensionPointName<TestNameProvider> EP = ExtensionPointName.create(
            "com.github.safetestspace.testspace.testNameProvider");

    record Declaration(PsiElement identifier, Set<String> referencedNames) {
    }

    boolean supports(FileType fileType);

    List<Declaration> find(PsiElement root, boolean collectReferences);
}
