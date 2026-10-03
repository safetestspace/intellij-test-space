package com.github.safetestspace.testspace.editor;

import com.github.safetestspace.testspace.settings.TestSpaceSettings;
import com.intellij.util.xmlb.XmlSerializer;

import java.util.List;

public class PreservedTestNamesTest extends TestNameEditorTestCase {
    public void testDefaultNameIsPreservedWithoutReference() {
        TestSpaceSettings.getInstance().loadState(new TestSpaceSettings.Options(true, true, true, false));
        configureTest("supportsTestNgTests", "");

        assertEquals("supports TestNg tests", descriptors()[0].getPlaceholderText());
    }

    public void testConfiguredCaseOverridesReferencedIdentifier() {
        TestSpaceSettings.Options options = new TestSpaceSettings.Options(true, true, true, false);
        options.preservedNames = "FindById";
        TestSpaceSettings.getInstance().loadState(options);
        configureTest("testFindById", "findById();");

        assertEquals("test FindById", descriptors()[0].getPlaceholderText());
    }

    public void testNamesRoundTripAndDoNotShareMutableState() {
        TestSpaceSettings.Options options = new TestSpaceSettings.Options();
        options.preservedNames = " TestNg \n\nMockito\nTestNg\n";
        TestSpaceSettings.Options restored = XmlSerializer.deserialize(
                XmlSerializer.serialize(options), TestSpaceSettings.Options.class);
        assertEquals(options.preservedNames, restored.preservedNames);
        assertEquals(List.of("TestNg", "Mockito"), List.copyOf(restored.preservedWords()));

        TestSpaceSettings.getInstance().loadState(restored);
        restored.preservedNames = "changed";
        TestSpaceSettings.Options snapshot = TestSpaceSettings.getInstance().getState();
        assertEquals(options.preservedNames, snapshot.preservedNames);
        snapshot.preservedNames = "changed again";
        assertEquals(options.preservedNames, TestSpaceSettings.getInstance().getState().preservedNames);
    }

    public void testEmptyNamesStayEmptyAfterPersistence() {
        TestSpaceSettings.Options options = new TestSpaceSettings.Options();
        options.preservedNames = "";
        TestSpaceSettings.Options restored = XmlSerializer.deserialize(
                XmlSerializer.serialize(options), TestSpaceSettings.Options.class);
        assertEquals("", restored.preservedNames);
        assertTrue(restored.preservedWords().isEmpty());
    }

    private void configureTest(String name, String body) {
        myFixture.configureByText("Example.java", """
                import org.junit.jupiter.api.Test;
                class Example {
                    @Test void %s() { %s }
                    void findById() {}
                }
                """.formatted(name, body));
    }
}
