package com.github.safetestspace.testspace.editor;

import com.github.safetestspace.testspace.discovery.TestNameFinder;
import com.github.safetestspace.testspace.settings.TestSpaceSettings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.FoldingModel;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.FoldingListener;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Empty folds hide names; inlays draw them at the end because collapsed folds hide start-offset inlays.
 */
final class StyledTestNames implements FoldingListener, DocumentListener {
    private static final Key<StyledTestNames> KEY = Key.create("test.space.styled.names");
    private static final Key<String> DISPLAY = Key.create("test.space.display");

    private record Name(TextRange range, String display) {
    }

    private final Editor editor;
    private final Project project;
    private final Disposable disposable;
    private final Set<FoldRegion> regions = new LinkedHashSet<>();
    private final Map<FoldRegion, Inlay<TestNameRenderer>> inlays = new HashMap<>();

    private StyledTestNames(Editor editor, Project project, Disposable disposable) {
        this.editor = editor;
        this.project = project;
        this.disposable = disposable;
    }

    static void attach(Editor editor, Disposable disposable) {
        Project project = editor.getProject();
        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (project == null || file == null || !TestNameFinder.supports(file.getFileType())
                || !(editor.getFoldingModel() instanceof FoldingModelEx foldingModel)) {
            return;
        }
        StyledTestNames names = new StyledTestNames(editor, project, disposable);
        editor.putUserData(KEY, names);
        Disposer.register(disposable, () -> editor.putUserData(KEY, null));
        foldingModel.addListener(names, disposable);
        editor.getDocument().addDocumentListener(names, disposable);
        names.refresh();
    }

    static void refresh(Editor editor) {
        StyledTestNames names = editor.getUserData(KEY);
        if (names != null) {
            names.refresh();
        }
    }

    static boolean isStyled(FoldRegion region) {
        return region.getUserData(DISPLAY) != null;
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        if (TestSpaceSettings.getInstance().getState().customStyle) {
            scheduleUpdate();
        }
    }

    @Override
    public void onFoldProcessingEnd() {
        updateInlays();
    }

    private void refresh() {
        if (TestSpaceSettings.getInstance().getState().customStyle) {
            scheduleUpdate();
        } else {
            clear();
        }
    }

    private void scheduleUpdate() {
        ReadAction.nonBlocking(this::findNames)
                .inSmartMode(project)
                .withDocumentsCommitted(project)
                .expireWith(disposable)
                .expireWhen(editor::isDisposed)
                .coalesceBy(this)
                .finishOnUiThread(ModalityState.defaultModalityState(), this::apply)
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private List<Name> findNames() {
        TestSpaceSettings.Options options = TestSpaceSettings.getInstance().getState();
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (!options.customStyle || file == null || !TestNameFinder.supports(file.getFileType())) {
            return List.of();
        }
        return TestNameFinder.find(file, options).stream()
                .map(name -> new Name(name.identifier().getTextRange(), name.display()))
                .toList();
    }

    private void apply(List<Name> names) {
        if (editor.isDisposed()) {
            return;
        }
        FoldingModel foldingModel = editor.getFoldingModel();
        foldingModel.runBatchFoldingOperation(() -> {
            Map<TextRange, FoldRegion> existing = new HashMap<>();
            for (FoldRegion region : regions) {
                if (region.isValid()) {
                    existing.put(region.getTextRange(), region);
                }
            }
            regions.clear();
            for (Name name : names) {
                FoldRegion region = existing.remove(name.range());
                if (region == null) {
                    region = createRegion(name);
                } else {
                    region.putUserData(DISPLAY, name.display());
                }
                if (region != null) {
                    regions.add(region);
                }
            }
            existing.values().forEach(foldingModel::removeFoldRegion);
        });
        updateInlays();
    }

    /**
     * Replaces restored or plain folds on the same range, which would block the new fold.
     */
    private @Nullable FoldRegion createRegion(Name name) {
        FoldingModel foldingModel = editor.getFoldingModel();
        for (FoldRegion other : foldingModel.getAllFoldRegions()) {
            if (other.isValid() && other.getTextRange().equals(name.range())) {
                foldingModel.removeFoldRegion(other);
            }
        }
        FoldRegion region = foldingModel.addFoldRegion(name.range().getStartOffset(), name.range().getEndOffset(), "");
        if (region != null) {
            region.putUserData(DISPLAY, name.display());
            region.setExpanded(TestNameCaretFolding.touchesCaret(editor, region));
        }
        return region;
    }

    private void clear() {
        if (regions.isEmpty() && inlays.isEmpty()) {
            return;
        }
        FoldingModel foldingModel = editor.getFoldingModel();
        foldingModel.runBatchFoldingOperation(() -> {
            regions.stream().filter(FoldRegion::isValid).forEach(foldingModel::removeFoldRegion);
            regions.clear();
        });
        updateInlays();
    }

    private void updateInlays() {
        if (editor.isDisposed()) {
            return;
        }
        Iterator<Map.Entry<FoldRegion, Inlay<TestNameRenderer>>> iterator = inlays.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<FoldRegion, Inlay<TestNameRenderer>> entry = iterator.next();
            Inlay<TestNameRenderer> inlay = entry.getValue();
            if (!needsInlay(entry.getKey()) || !inlay.isValid() || inlay.getOffset() != entry.getKey().getEndOffset()) {
                Disposer.dispose(inlay);
                iterator.remove();
            }
        }
        for (FoldRegion region : regions) {
            if (needsInlay(region)) {
                showInlay(region, region.getUserData(DISPLAY));
            }
        }
    }

    private void showInlay(FoldRegion region, String display) {
        Inlay<TestNameRenderer> inlay = inlays.get(region);
        if (inlay == null) {
            inlay = editor.getInlayModel().addInlineElement(region.getEndOffset(), true, new TestNameRenderer(display));
            if (inlay != null) {
                inlays.put(region, inlay);
            }
        } else if (!inlay.getRenderer().getText().equals(display)) {
            inlay.getRenderer().setText(display);
            inlay.update();
        }
    }

    private boolean needsInlay(FoldRegion region) {
        return region.isValid() && !region.isExpanded() && regions.contains(region);
    }
}
