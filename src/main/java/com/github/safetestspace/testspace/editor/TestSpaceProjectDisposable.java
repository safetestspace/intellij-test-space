package com.github.safetestspace.testspace.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;

@Service(Service.Level.PROJECT)
public final class TestSpaceProjectDisposable implements Disposable {
    @Override
    public void dispose() {
        // The platform disposes of registered children on project close or plugin unload.
    }
}
