package com.github.safetestspace.testspace.editor;

import com.github.safetestspace.testspace.settings.TestSpaceSettings;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

import java.util.Arrays;

public abstract class TestNameEditorTestCase extends LightJavaCodeInsightFixtureTestCase {
    private TestSpaceSettings.Options previousSettings;

    @Override
    protected void setUp() throws Exception {
        com.github.safetestspace.testspace.rust.RustTestEnvironment.prepare();
        super.setUp();
        previousSettings = TestSpaceSettings.getInstance().getState();
        TestSpaceSettings.getInstance().loadState(new TestSpaceSettings.Options(true, true, false, false));
        myFixture.addClass("package org.junit.jupiter.api; public @interface Test {}");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (previousSettings != null) {
                TestSpaceSettings.getInstance().loadState(previousSettings);
            }
        } finally {
            super.tearDown();
        }
    }

    protected final FoldRegion findFold(String placeholder) {
        return Arrays.stream(myFixture.getEditor().getFoldingModel().getAllFoldRegions())
                .filter(region -> region.getPlaceholderText().equals(placeholder))
                .findFirst().orElse(null);
    }

    protected final FoldRegion requireFold(String placeholder) {
        FoldRegion fold = findFold(placeholder);
        assertNotNull("Expected a fold displaying: " + placeholder, fold);
        return fold;
    }

    protected final void openSimpleTest() {
        myFixture.configureByText("Example.java", """
                import org.junit.jupiter.api.Test;
                class Example {
                    @Test void should_save_order() {}
                }
                """);
    }

    protected final void awaitFoldingRefresh() {
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    protected final FoldingDescriptor[] descriptors() {
        return new TestNameFoldingBuilder().buildFoldRegions(
                myFixture.getFile(), myFixture.getEditor().getDocument(), false);
    }
}
