package com.github.safetestspace.testspace.editor;

import com.github.safetestspace.testspace.discovery.TestNameFinder;
import com.github.safetestspace.testspace.formatting.TestNameFormatter;
import com.github.safetestspace.testspace.settings.TestSpaceSettings;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Plain folds only; custom styling is handled by {@link StyledTestNames}.
 */
public final class TestNameFoldingBuilder extends FoldingBuilderEx {
    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(
            @NotNull PsiElement root, @NotNull Document document, boolean quick) {
        TestSpaceSettings settings = TestSpaceSettings.getInstance();
        TestSpaceSettings.Options options = settings.getState();
        if (options.customStyle) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        return TestNameFinder.find(root, options).stream()
                .map(name -> {
                    FoldingDescriptor descriptor = new FoldingDescriptor(name.identifier().getNode(),
                            name.identifier().getTextRange(), null, name.display(), true, Set.of(settings));
                    descriptor.setCanBeRemovedWhenCollapsed(true);
                    return descriptor;
                })
                .toArray(FoldingDescriptor[]::new);
    }

    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node) {
        TestSpaceSettings.Options options = TestSpaceSettings.getInstance().getState();
        return TestNameFormatter.format(node.getText(), options.replaceUnderscores, options.splitCamelCase);
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return true;
    }
}
