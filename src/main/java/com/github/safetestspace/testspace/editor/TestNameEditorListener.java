package com.github.safetestspace.testspace.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

public final class TestNameEditorListener implements EditorFactoryListener {
    private static final Key<Disposable> DISPOSABLE = Key.create("test.space.editor.disposable");

    private static final CaretListener CARET_LISTENER = new CaretListener() {
        @Override
        public void caretPositionChanged(@NotNull CaretEvent event) {
            TestNameCaretFolding.update(event.getEditor());
        }

        @Override
        public void caretAdded(@NotNull CaretEvent event) {
            TestNameCaretFolding.update(event.getEditor());
        }

        @Override
        public void caretRemoved(@NotNull CaretEvent event) {
            TestNameCaretFolding.update(event.getEditor());
        }
    };

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        Disposable disposable = Disposer.newDisposable("Test Space editor support");
        editor.putUserData(DISPOSABLE, disposable);
        editor.getCaretModel().addCaretListener(CARET_LISTENER, disposable);
        StyledTestNames.attach(editor, disposable);
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        Disposable disposable = editor.getUserData(DISPOSABLE);
        if (disposable != null) {
            editor.putUserData(DISPOSABLE, null);
            Disposer.dispose(disposable);
        }
    }
}
