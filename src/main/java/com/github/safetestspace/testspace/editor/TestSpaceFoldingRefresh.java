package com.github.safetestspace.testspace.editor;

import com.github.safetestspace.testspace.discovery.TestNameFinder;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.concurrency.AppExecutorUtil;

import java.util.Objects;

public final class TestSpaceFoldingRefresh {
    private static final Object REFRESH_KEY = new Object();

    private TestSpaceFoldingRefresh() {
    }

    public static void refreshOpenEditors() {
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            Project project = editor.getProject();
            if (editor.isDisposed() || project == null || project.isDisposed()) {
                continue;
            }
            StyledTestNames.refresh(editor);
            CodeFoldingManager manager = CodeFoldingManager.getInstance(project);
            // Invalidate even an empty cache, so enabling the display works without a source edit.
            manager.scheduleAsyncFoldingUpdate(editor);
            ReadAction.nonBlocking(() -> {
                        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
                        if (file == null || !TestNameFinder.supports(file.getFileType())) {
                            return null;
                        }
                        Runnable update = manager.updateFoldRegionsAsync(editor, false);
                        FoldingDescriptor[] names = new TestNameFoldingBuilder()
                                .buildFoldRegions(file, editor.getDocument(), false);
                        return new Refresh(update, names);
                    }).inSmartMode(project)
                    .withDocumentsCommitted(project)
                    .expireWith(project.getService(TestSpaceProjectDisposable.class))
                    .expireWhen(editor::isDisposed)
                    .coalesceBy(REFRESH_KEY, editor)
                    .finishOnUiThread(ModalityState.defaultModalityState(), refresh -> {
                        if (refresh == null || editor.isDisposed()) {
                            return;
                        }
                        if (refresh.update() != null) {
                            refresh.update().run();
                        }
                        // Apply our display preference without resetting unrelated code folding.
                        editor.getFoldingModel().runBatchFoldingOperation(() -> {
                            for (FoldingDescriptor name : refresh.names()) {
                                FoldRegion region = manager.findFoldRegion(editor,
                                        name.getRange().getStartOffset(), name.getRange().getEndOffset());
                                if (region != null && Objects.equals(name.getPlaceholderText(), region.getPlaceholderText())) {
                                    region.setExpanded(false);
                                }
                            }
                        });
                    }).submit(AppExecutorUtil.getAppExecutorService());
        }
    }

    private record Refresh(Runnable update, FoldingDescriptor[] names) {
    }
}
