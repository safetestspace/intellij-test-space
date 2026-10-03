package com.github.safetestspace.testspace.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.SimpleModificationTracker;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

@Service(Service.Level.APP)
@State(name = "TestSpaceSettings", storages = @Storage("test-space.xml"))
public final class TestSpaceSettings extends SimpleModificationTracker
        implements PersistentStateComponent<TestSpaceSettings.Options> {
    public static final class Options {
        public boolean enabled = true;
        public boolean replaceUnderscores = true;
        public boolean splitCamelCase = true;
        public boolean customStyle = true;
        public String preservedNames = "TestNg";

        public Options() {
        }

        public Options(boolean enabled, boolean replaceUnderscores, boolean splitCamelCase, boolean customStyle) {
            this.enabled = enabled;
            this.replaceUnderscores = replaceUnderscores;
            this.splitCamelCase = splitCamelCase;
            this.customStyle = customStyle;
        }

        private Options copy() {
            Options copy = new Options(enabled, replaceUnderscores, splitCamelCase, customStyle);
            copy.preservedNames = preservedNames;
            return copy;
        }

        public Set<String> preservedWords() {
            Set<String> words = new LinkedHashSet<>();
            preservedNames.lines().map(String::strip).filter(word -> !word.isEmpty()).forEach(words::add);
            return words;
        }
    }

    private volatile Options options = new Options();

    public static TestSpaceSettings getInstance() {
        return ApplicationManager.getApplication().getService(TestSpaceSettings.class);
    }

    @Override
    public @NotNull Options getState() {
        return options.copy();
    }

    @Override
    public void loadState(@NotNull Options state) {
        options = state.copy();
        incModificationCount();
    }
}
