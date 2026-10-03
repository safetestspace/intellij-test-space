package com.github.safetestspace.testspace.editor;

import com.github.safetestspace.testspace.settings.TestSpaceSettings;
import com.github.safetestspace.testspace.ui.TestSpaceConfigurable;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.testFramework.EditorTestUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.util.Arrays;

public class TestSpaceSettingsRefreshTest extends TestNameEditorTestCase {
    private TestSpaceConfigurable configurable;
    private JComponent settingsPanel;
    private String original;
    private FoldRegion helperFold;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.configureByText("Example.java", """
                import org.junit.jupiter.api.Test;
                class Example {
                    @Test void should_saveOrder() {}
                    void helper() {
                        int first = 1;
                        int second = 2;
                    }
                }
                """);
        original = myFixture.getEditor().getDocument().getText();
        EditorTestUtil.buildInitialFoldingsInBackground(myFixture.getEditor());
        requireFold("should saveOrder");
        int helperBodyOffset = original.indexOf('{', original.indexOf("void helper"));
        helperFold = Arrays.stream(myFixture.getEditor().getFoldingModel().getAllFoldRegions())
                .filter(region -> region.getStartOffset() == helperBodyOffset).findFirst().orElse(null);
        assertNotNull("Expected the helper method body fold", helperFold);
        myFixture.getEditor().getFoldingModel().runBatchFoldingOperation(() -> helperFold.setExpanded(false));
        configurable = new TestSpaceConfigurable();
        settingsPanel = configurable.createComponent();
        assertFalse(configurable.isModified());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (configurable != null) {
                configurable.disposeUIResources();
            }
        } finally {
            super.tearDown();
        }
    }

    public void testApplyingCamelCaseRefreshesExistingFold() {
        checkbox("Split camelCase").doClick();
        assertTrue(configurable.isModified());

        applySettings();

        requireFold("should save order");
        assertNull(findFold("should saveOrder"));
        assertFalse(configurable.isModified());
        assertSourceAndHelperFoldUnchanged();
    }

    public void testSettingsFitNarrowPanelWithLongPreservedName() {
        JBTextArea names = UIUtil.findComponentsOfType(settingsPanel, JBTextArea.class).getFirst();
        names.setText("LongPreservedName".repeat(30));
        settingsPanel.setSize(480, 800);
        settingsPanel.doLayout();

        assertTrue("Settings minimum width exceeds the available space",
                settingsPanel.getMinimumSize().width <= 480);
        for (var component : settingsPanel.getComponents()) {
            assertTrue("Component overflows the settings panel: " + component.getClass().getSimpleName(),
                    component.getX() >= 0 && component.getX() + component.getWidth() <= 480);
        }
        JScrollPane scroll = UIUtil.findComponentsOfType(settingsPanel, JScrollPane.class).getFirst();
        assertTrue("Preserved names should remain usable", scroll.getWidth() >= 200);
    }

    public void testDisablingRemovesExistingNameFold() {
        checkbox("Show readable test method names").doClick();

        applySettings();

        assertNull(findFold("should saveOrder"));
        assertSourceAndHelperFoldUnchanged();
    }

    public void testPreservedNamesApplyResetAndRefresh() {
        checkbox("Split camelCase").doClick();
        JBTextArea names = UIUtil.findComponentsOfType(settingsPanel, JBTextArea.class).getFirst();
        assertEquals("TestNg", names.getText());
        names.setText("saveOrder\nTestNg");
        assertTrue(configurable.isModified());

        applySettings();

        requireFold("should saveOrder");
        assertFalse(configurable.isModified());
        assertEquals("saveOrder\nTestNg", TestSpaceSettings.getInstance().getState().preservedNames);
        names.setText("discarded");
        configurable.reset();
        assertEquals("saveOrder\nTestNg", names.getText());
        assertFalse(configurable.isModified());

        names.setText("");
        applySettings();
        requireFold("should save order");
        assertSourceAndHelperFoldUnchanged();
    }

    public void testReenablingCreatesCollapsedNameWithoutSourceEdit() {
        JBCheckBox enabled = checkbox("Show readable test method names");
        enabled.doClick();
        applySettings();
        assertNull(findFold("should saveOrder"));

        enabled.doClick();
        applySettings();

        assertFalse(requireFold("should saveOrder").isExpanded());
        assertSourceAndHelperFoldUnchanged();
    }

    private void applySettings() {
        configurable.apply();
        awaitFoldingRefresh();
    }

    private JBCheckBox checkbox(String labelPrefix) {
        var matches = UIUtil.findComponentsOfType(settingsPanel, JBCheckBox.class).stream()
                .filter(box -> box.getText().startsWith(labelPrefix)).toList();
        assertEquals("Expected one checkbox starting with: " + labelPrefix, 1, matches.size());
        return matches.getFirst();
    }

    private void assertSourceAndHelperFoldUnchanged() {
        assertTrue("Settings refresh removed an unrelated fold", helperFold.isValid());
        assertFalse("Settings refresh expanded an unrelated fold", helperFold.isExpanded());
        assertEquals(original, myFixture.getEditor().getDocument().getText());
    }
}
