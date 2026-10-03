package com.github.safetestspace.testspace.rust;

import com.github.safetestspace.testspace.editor.LanguageTestNameEditorTestCase;
import com.github.safetestspace.testspace.settings.TestSpaceSettings;

public class RustTestNameFoldingTest extends LanguageTestNameEditorTestCase {
    public void testOnlyAttributedDeclarationsFold() {
        myFixture.configureByText("checkout.rs", """
                #[cfg(test)]
                mod tests {
                    #[test]
                    fn saves_an_order() { helper_method(); }
                    fn helper_method() { saves_an_order(); }
                    // #[test] fn not_a_test() {}
                    const TEXT: &str = "#[test] fn not_a_test() {}";
                    #[bench]
                    fn benchmarks_an_order() {}
                }
                """);
        assertEquals("Rust", myFixture.getFile().getLanguage().getID());
        assertDisplays("saves an order");
        assertEquals(myFixture.getFile().getText().indexOf("saves_an_order"),
                descriptors()[0].getRange().getStartOffset());
    }

    public void testAsyncAndParameterizedAttributes() {
        myFixture.configureByText("checkout.rs", """
                #[tokio :: test(flavor = "multi_thread")]
                async fn handles_async_order() {}
                #[async_std::test]
                async fn handles_another_order() {}
                #[rstest::rstest]
                fn checks_each_order() {}
                #[test_case::test_case(1)]
                fn validates_order(value: i32) {}
                #[something::test]
                fn unrelated_attribute() {}
                """);
        assertDisplays("handles async order", "handles another order", "checks each order", "validates order");
    }

    public void testReferencedNamesAndConfiguredCase() {
        myFixture.configureByText("checkout.rs", """
                #[test]
                fn callsFindByIdWithTestNg() { repo.findById(); }
                """);
        assertDisplays("calls findById with TestNg");
        var options = TestSpaceSettings.getInstance().getState();
        options.preservedNames = "FindById\nTestNg";
        TestSpaceSettings.getInstance().loadState(options);
        assertDisplays("calls FindById with TestNg");
    }

    public void testNestedFunctionsAreNotCollected() {
        myFixture.configureByText("checkout.rs", """
                fn helper() {
                    #[test]
                    fn nested_test() {}
                }
                """);
        assertDisplays();
    }

    public void testStyledNamesCaretAndSettingsRefresh() {
        myFixture.configureByText("checkout.rs", """
                #[test]
                fn saves_an_order() {}
                """);
        assertStyledLifecycle("saves_an_order", "saves an order");
    }
}
