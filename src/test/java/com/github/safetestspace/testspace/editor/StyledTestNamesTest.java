package com.github.safetestspace.testspace.editor;

import com.github.safetestspace.testspace.settings.TestSpaceSettings;
import com.intellij.openapi.editor.FoldRegion;

import java.util.Arrays;
import java.util.List;

public class StyledTestNamesTest extends TestNameEditorTestCase {
    public void testDefaultCustomStyleHidesNameAndDrawsItWithInlay() {
        FoldRegion fold = openStyledTest();
        String original = myFixture.getEditor().getDocument().getText();
        assertEquals(0, descriptors().length);
        assertEquals("", fold.getPlaceholderText());
        assertEquals("should_save_order", myFixture.getEditor().getDocument().getText(fold.getTextRange()));
        assertFalse(fold.isExpanded());
        assertEquals(List.of("should save order"), inlayTexts());
        assertInlayIsLaidOut(fold);
        assertEquals(original, myFixture.getEditor().getDocument().getText());
    }

    public void testCaretHidesInlayWhileEditingAndRestoresItOnLeaving() {
        FoldRegion fold = openStyledTest();
        String original = myFixture.getEditor().getDocument().getText();
        myFixture.getEditor().getCaretModel().moveToOffset(fold.getStartOffset());
        assertTrue(fold.isExpanded());
        assertEquals(List.of(), inlayTexts());

        myFixture.getEditor().getCaretModel().moveToOffset(0);
        assertFalse(fold.isExpanded());
        assertEquals(List.of("should save order"), inlayTexts());
        assertInlayIsLaidOut(fold);
        assertEquals(original, myFixture.getEditor().getDocument().getText());
    }

    public void testSwitchingToPlainStyleRemovesStyledFoldAndInlay() {
        FoldRegion fold = openStyledTest();
        String original = myFixture.getEditor().getDocument().getText();
        TestSpaceSettings.getInstance().loadState(new TestSpaceSettings.Options(true, true, false, false));
        TestSpaceFoldingRefresh.refreshOpenEditors();
        awaitFoldingRefresh();
        assertFalse(fold.isValid());
        assertEquals(List.of(), inlayTexts());
        assertNotNull(findFold("should save order"));
        assertEquals(original, myFixture.getEditor().getDocument().getText());
    }

    public void testCustomStyleReplacesFoldRestoredFromPreviousSession() {
        // The IDE can restore an empty fold from disk before our inlay is recreated.
        openSimpleTest();
        var editor = myFixture.getEditor();
        int start = editor.getDocument().getText().indexOf("should_save_order");
        int end = start + "should_save_order".length();
        editor.getFoldingModel().runBatchFoldingOperation(() -> {
            FoldRegion restored = editor.getFoldingModel().addFoldRegion(start, end, "");
            assertNotNull("Could not simulate the restored fold", restored);
            restored.setExpanded(false);
        });

        TestSpaceSettings.getInstance().loadState(new TestSpaceSettings.Options());
        TestSpaceFoldingRefresh.refreshOpenEditors();
        awaitFoldingRefresh();

        List<FoldRegion> regionsOnName = Arrays.stream(editor.getFoldingModel().getAllFoldRegions())
                .filter(region -> region.getStartOffset() == start && region.getEndOffset() == end).toList();
        assertEquals(1, regionsOnName.size());
        assertTrue(StyledTestNames.isStyled(regionsOnName.getFirst()));
        assertEquals(List.of("should save order"), inlayTexts());
        assertInlayIsLaidOut(regionsOnName.getFirst());
    }

    private FoldRegion openStyledTest() {
        TestSpaceSettings.getInstance().loadState(new TestSpaceSettings.Options());
        openSimpleTest();
        String original = myFixture.getEditor().getDocument().getText();
        awaitFoldingRefresh();
        assertEquals("Creating styled folds changed the source", original,
                myFixture.getEditor().getDocument().getText());
        List<FoldRegion> styled = Arrays.stream(myFixture.getEditor().getFoldingModel().getAllFoldRegions())
                .filter(StyledTestNames::isStyled).toList();
        assertEquals("Expected one styled test name", 1, styled.size());
        return styled.getFirst();
    }

    /**
     * Inlays must occupy horizontal space, not just exist in the model.
     */
    private void assertInlayIsLaidOut(FoldRegion fold) {
        var editor = myFixture.getEditor();
        int inlayWidth = editor.getInlayModel()
                .getInlineElementsInRange(fold.getStartOffset(), fold.getEndOffset(), TestNameRenderer.class)
                .getFirst().getWidthInPixels();
        int nameStart = editor.offsetToXY(fold.getStartOffset()).x;
        int afterParenthesis = editor.offsetToXY(fold.getEndOffset() + 1).x;
        assertTrue("inlay is not laid out", afterParenthesis - nameStart > inlayWidth);
    }

    private List<String> inlayTexts() {
        return myFixture.getEditor().getInlayModel()
                .getInlineElementsInRange(0, myFixture.getEditor().getDocument().getTextLength(), TestNameRenderer.class)
                .stream().map(inlay -> inlay.getRenderer().getText()).toList();
    }
}
