package com.github.safetestspace.testspace.editor;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.testFramework.EditorTestUtil;

public class PlainTestNameFoldingTest extends TestNameEditorTestCase {
    public void testRegisteredBuilderCollapsesNameAndPreservesDocument() {
        openSimpleTest();
        String original = myFixture.getEditor().getDocument().getText();
        EditorTestUtil.buildInitialFoldingsInBackground(myFixture.getEditor());
        FoldRegion fold = requireFold("should save order");
        assertFalse(fold.isExpanded());
        myFixture.getEditor().getFoldingModel().runBatchFoldingOperation(() -> fold.setExpanded(true));
        assertTrue(fold.isExpanded());
        assertEquals(original, myFixture.getEditor().getDocument().getText());
    }

    public void testCaretTouchingNameExpandsItAndLeavingCollapsesIt() {
        openSimpleTest();
        EditorTestUtil.buildInitialFoldingsInBackground(myFixture.getEditor());
        FoldRegion fold = requireFold("should save order");
        assertFalse(fold.isExpanded());

        myFixture.getEditor().getCaretModel().moveToOffset(fold.getStartOffset() - 1);
        assertFalse(fold.isExpanded());
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT);
        assertEquals(fold.getStartOffset(), myFixture.getCaretOffset());
        assertTrue(fold.isExpanded());

        myFixture.getEditor().getCaretModel().moveToOffset(0);
        assertFalse(fold.isExpanded());

        myFixture.getEditor().getCaretModel().moveToOffset(fold.getEndOffset() + 1);
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_MOVE_CARET_LEFT);
        assertTrue(fold.isExpanded());
    }
}
