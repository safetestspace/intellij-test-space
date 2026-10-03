package com.github.safetestspace.testspace.editor;

import com.github.safetestspace.testspace.settings.TestSpaceSettings;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;

import java.util.ArrayList;
import java.util.List;

/**
 * Expands names touched by a caret; collapses the rest.
 */
final class TestNameCaretFolding {
    private TestNameCaretFolding() {
    }

    static void update(Editor editor) {
        if (editor.isDisposed() || !TestSpaceSettings.getInstance().getState().enabled) {
            return;
        }
        List<FoldRegion> toExpand = new ArrayList<>();
        List<FoldRegion> toCollapse = new ArrayList<>();
        for (FoldRegion region : editor.getFoldingModel().getAllFoldRegions()) {
            if (!region.isValid() || !isTestName(editor, region)) {
                continue;
            }
            boolean touched = touchesCaret(editor, region);
            if (touched && !region.isExpanded()) {
                toExpand.add(region);
            } else if (!touched && region.isExpanded()) {
                toCollapse.add(region);
            }
        }
        if (!toExpand.isEmpty() || !toCollapse.isEmpty()) {
            editor.getFoldingModel().runBatchFoldingOperation(() -> {
                toExpand.forEach(region -> region.setExpanded(true));
                toCollapse.forEach(region -> region.setExpanded(false));
            });
        }
    }

    /**
     * Reformatting cannot identify folds: placeholders also depend on names referenced in the body.
     */
    static boolean isTestName(Editor editor, FoldRegion region) {
        if (StyledTestNames.isStyled(region)) {
            return true;
        }
        String placeholder = region.getPlaceholderText();
        if (placeholder.indexOf(' ') < 0) {
            return false;
        }
        String original = editor.getDocument().getText(region.getTextRange());
        return !original.equals(placeholder) && withoutSeparators(original).equalsIgnoreCase(withoutSeparators(placeholder));
    }

    private static String withoutSeparators(String text) {
        return text.replace("_", "").replace(" ", "");
    }

    /**
     * Both endpoints count as touching the name.
     */
    static boolean touchesCaret(Editor editor, FoldRegion region) {
        for (Caret caret : editor.getCaretModel().getAllCarets()) {
            int offset = caret.getOffset();
            if (offset >= region.getStartOffset() && offset <= region.getEndOffset()) {
                return true;
            }
        }
        return false;
    }
}
