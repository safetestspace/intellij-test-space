package com.github.safetestspace.testspace.editor;

import com.github.safetestspace.testspace.settings.TestSpaceSettings;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Arrays;
import java.util.List;

public abstract class LanguageTestNameEditorTestCase extends BasePlatformTestCase {
    private TestSpaceSettings.Options previousSettings;

    @Override
    protected void setUp() throws Exception {
        com.github.safetestspace.testspace.rust.RustTestEnvironment.prepare();
        super.setUp();
        previousSettings = TestSpaceSettings.getInstance().getState();
        TestSpaceSettings.getInstance().loadState(new TestSpaceSettings.Options(true, true, true, false));
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            TestSpaceSettings.getInstance().loadState(previousSettings);
            awaitRefresh();
        } finally {
            super.tearDown();
        }
    }

    protected final FoldingDescriptor[] descriptors() {
        return new TestNameFoldingBuilder().buildFoldRegions(
                myFixture.getFile(), myFixture.getEditor().getDocument(), false);
    }

    protected final void assertDisplays(String... expected) {
        assertEquals(List.of(expected), Arrays.stream(descriptors())
                .map(FoldingDescriptor::getPlaceholderText).toList());
    }

    protected final void assertStyledLifecycle(String originalName, String display) {
        var editor = myFixture.getEditor();
        String original = editor.getDocument().getText();
        editor.getCaretModel().moveToOffset(0);
        apply(new TestSpaceSettings.Options(true, true, true, true));
        assertEquals(List.of(display), inlayTexts());
        var fold = Arrays.stream(editor.getFoldingModel().getAllFoldRegions())
                .filter(StyledTestNames::isStyled).findFirst().orElseThrow();
        assertEquals(originalName, original.substring(fold.getStartOffset(), fold.getEndOffset()));
        editor.getCaretModel().moveToOffset(fold.getStartOffset());
        assertTrue(fold.isExpanded());
        assertEquals(List.of(), inlayTexts());
        editor.getCaretModel().moveToOffset(0);
        assertFalse(fold.isExpanded());
        assertEquals(List.of(display), inlayTexts());

        apply(new TestSpaceSettings.Options(false, true, true, true));
        assertEquals(List.of(), inlayTexts());
        assertFalse(fold.isValid());
        apply(new TestSpaceSettings.Options(true, true, true, true));
        assertEquals(List.of(display), inlayTexts());
        apply(new TestSpaceSettings.Options(true, true, true, false));
        assertEquals(List.of(), inlayTexts());
        assertTrue(Arrays.stream(editor.getFoldingModel().getAllFoldRegions())
                .anyMatch(region -> display.equals(region.getPlaceholderText())));
        assertEquals(original, editor.getDocument().getText());
    }

    private List<String> inlayTexts() {
        return myFixture.getEditor().getInlayModel().getInlineElementsInRange(
                        0, myFixture.getEditor().getDocument().getTextLength(), TestNameRenderer.class)
                .stream().map(inlay -> inlay.getRenderer().getText()).toList();
    }

    private void apply(TestSpaceSettings.Options options) {
        TestSpaceSettings.getInstance().loadState(options);
        TestSpaceFoldingRefresh.refreshOpenEditors();
        awaitRefresh();
    }

    private void awaitRefresh() {
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }
}
