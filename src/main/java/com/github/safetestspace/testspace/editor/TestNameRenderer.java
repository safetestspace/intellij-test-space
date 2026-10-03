package com.github.safetestspace.testspace.editor;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

final class TestNameRenderer implements EditorCustomElementRenderer {
    static final TextAttributesKey ATTRIBUTES =
            TextAttributesKey.createTextAttributesKey("TEST_SPACE_TEST_NAME", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION);

    private String text;

    TestNameRenderer(String text) {
        this.text = text;
    }

    String getText() {
        return text;
    }

    void setText(String text) {
        this.text = text;
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        Editor editor = inlay.getEditor();
        Font font = font(editor, attributes(editor));
        return Math.max(1, editor.getContentComponent().getFontMetrics(font).stringWidth(text));
    }

    @Override
    public void paint(@NotNull Inlay inlay, @NotNull Graphics graphics, @NotNull Rectangle targetRegion,
                      @NotNull TextAttributes textAttributes) {
        Editor editor = inlay.getEditor();
        TextAttributes attributes = attributes(editor);
        Color background = attributes.getBackgroundColor();
        if (background != null) {
            graphics.setColor(background);
            graphics.fillRect(targetRegion.x, targetRegion.y, targetRegion.width, targetRegion.height);
        }
        Color foreground = attributes.getForegroundColor();
        graphics.setColor(foreground != null ? foreground : editor.getColorsScheme().getDefaultForeground());
        graphics.setFont(font(editor, attributes));
        graphics.drawString(text, targetRegion.x, targetRegion.y + editor.getAscent());
    }

    private static TextAttributes attributes(Editor editor) {
        TextAttributes attributes = editor.getColorsScheme().getAttributes(ATTRIBUTES);
        return attributes != null ? attributes : new TextAttributes();
    }

    private static Font font(Editor editor, TextAttributes attributes) {
        return editor.getColorsScheme().getFont(EditorFontType.forJavaStyle(attributes.getFontType()));
    }
}
