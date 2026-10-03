package com.github.safetestspace.testspace.discovery;

import com.github.safetestspace.testspace.formatting.TestNameFormatter;
import com.github.safetestspace.testspace.settings.TestSpaceSettings;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class TestNameFinder {
    public record TestName(PsiElement identifier, String display) {
    }

    private TestNameFinder() {
    }

    public static boolean supports(FileType fileType) {
        return TestNameProvider.EP.getExtensionList().stream().anyMatch(provider -> provider.supports(fileType));
    }

    /**
     * Returns test names whose display changes and is not blank.
     */
    public static List<TestName> find(@NotNull PsiElement root, TestSpaceSettings.Options options) {
        if (!options.enabled || (!options.replaceUnderscores && !options.splitCamelCase)) {
            return List.of();
        }
        List<TestName> names = new ArrayList<>();
        if (root.getContainingFile() == null) return names;
        for (TestNameProvider provider : TestNameProvider.EP.getExtensionList()) {
            if (!provider.supports(root.getContainingFile().getFileType())) continue;
            for (TestNameProvider.Declaration declaration : provider.find(root, options.splitCamelCase)) {
                String original = declaration.identifier().getText();
                Set<String> protectedNames = options.preservedWords();
                protectedNames.addAll(declaration.referencedNames());
                String display = TestNameFormatter.format(
                        original, options.replaceUnderscores, options.splitCamelCase, protectedNames);
                if (!original.equals(display) && !display.isBlank()) {
                    names.add(new TestName(declaration.identifier(), display));
                }
            }
        }
        return names;
    }
}
