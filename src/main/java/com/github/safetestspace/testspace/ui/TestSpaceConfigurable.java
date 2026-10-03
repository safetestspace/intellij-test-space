package com.github.safetestspace.testspace.ui;

import com.github.safetestspace.testspace.editor.TestSpaceFoldingRefresh;
import com.github.safetestspace.testspace.formatting.TestNameFormatter;
import com.github.safetestspace.testspace.settings.TestSpaceSettings;
import com.intellij.DynamicBundle;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

public final class TestSpaceConfigurable implements SearchableConfigurable {
    private static final DynamicBundle BUNDLE =
            new DynamicBundle(TestSpaceConfigurable.class, "messages.TestSpaceBundle");

    private JPanel panel;
    private JBCheckBox enabled;
    private JBCheckBox underscores;
    private JBCheckBox camelCase;
    private JBCheckBox customStyle;
    private JBLabel preview;
    private JBTextArea preservedNames;

    @Override
    public @NotNull String getId() {
        return "com.github.safetestspace.testspace.settings";
    }

    @Override
    public String getDisplayName() {
        return BUNDLE.getMessage("settings.display.name");
    }

    @Override
    public @Nullable JComponent createComponent() {
        enabled = new JBCheckBox("Show readable test names");
        underscores = new JBCheckBox("Display underscores as spaces");
        camelCase = new JBCheckBox("Split camelCase into lowercase words");
        customStyle = new JBCheckBox("Custom style");
        preview = new JBLabel();
        preservedNames = new JBTextArea(5, 15);
        JBScrollPane namesScroll = new JBScrollPane(preservedNames);
        namesScroll.setMinimumSize(new Dimension(0, namesScroll.getPreferredSize().height));
        panel = FormBuilder.createFormBuilder()
                .addComponent(enabled)
                .addComponent(underscores)
                .addComponent(camelCase)
                .addComponent(description("Acronyms and referenced names keep their case."))
                .addComponent(new JBLabel(BUNDLE.getMessage("settings.preserved.names")))
                .addComponent(namesScroll)
                .addComponent(description(BUNDLE.getMessage("settings.preserved.names.help")))
                .addComponent(customStyle)
                .addComponent(description("Set its colors under Editor | Color Scheme | Test Space. Off: names use the Folded text style."))
                .addSeparator()
                .addLabeledComponent("Preview:", preview)
                .addComponent(description("Applies to test declarations. Move the caret to a name to edit it."))
                .addComponent(description("Source code, usages, and test runner names are preserved."))
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        enabled.addActionListener(event -> updatePreview());
        underscores.addActionListener(event -> updatePreview());
        camelCase.addActionListener(event -> updatePreview());
        customStyle.addActionListener(event -> updatePreview());
        preservedNames.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent event) {
                updatePreview();
            }
        });
        reset();
        return panel;
    }

    private static JTextArea description(String text) {
        JTextArea description = new JTextArea(text);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setEditable(false);
        description.setFocusable(false);
        description.setOpaque(false);
        JBLabel label = new JBLabel();
        description.setFont(label.getFont());
        description.setForeground(label.getForeground());
        return description;
    }

    private void updatePreview() {
        underscores.setEnabled(enabled.isSelected());
        camelCase.setEnabled(enabled.isSelected());
        customStyle.setEnabled(enabled.isSelected());
        preservedNames.setEnabled(enabled.isSelected() && camelCase.isSelected());
        String example = "should_supportTestNg";
        TestSpaceSettings.Options previewOptions = new TestSpaceSettings.Options();
        previewOptions.preservedNames = preservedNames.getText();
        preview.setText(enabled.isSelected()
                ? TestNameFormatter.format(example, underscores.isSelected(), camelCase.isSelected(),
                previewOptions.preservedWords())
                : example);
    }

    @Override
    public boolean isModified() {
        if (panel == null) {
            return false;
        }
        TestSpaceSettings.Options state = TestSpaceSettings.getInstance().getState();
        return enabled.isSelected() != state.enabled
                || underscores.isSelected() != state.replaceUnderscores
                || camelCase.isSelected() != state.splitCamelCase
                || customStyle.isSelected() != state.customStyle
                || !preservedNames.getText().equals(state.preservedNames);
    }

    @Override
    public void apply() {
        if (panel == null || !isModified()) {
            return;
        }
        TestSpaceSettings.Options state = new TestSpaceSettings.Options(
                enabled.isSelected(), underscores.isSelected(), camelCase.isSelected(), customStyle.isSelected());
        state.preservedNames = preservedNames.getText();
        TestSpaceSettings.getInstance().loadState(state);
        TestSpaceFoldingRefresh.refreshOpenEditors();
    }

    @Override
    public void reset() {
        if (panel == null) {
            return;
        }
        TestSpaceSettings.Options state = TestSpaceSettings.getInstance().getState();
        enabled.setSelected(state.enabled);
        underscores.setSelected(state.replaceUnderscores);
        camelCase.setSelected(state.splitCamelCase);
        customStyle.setSelected(state.customStyle);
        preservedNames.setText(state.preservedNames);
        updatePreview();
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        enabled = null;
        underscores = null;
        camelCase = null;
        customStyle = null;
        preview = null;
        preservedNames = null;
    }
}
