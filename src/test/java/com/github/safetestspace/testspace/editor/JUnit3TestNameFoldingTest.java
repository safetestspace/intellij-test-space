package com.github.safetestspace.testspace.editor;

import com.github.safetestspace.testspace.settings.TestSpaceSettings;
import com.intellij.lang.folding.FoldingDescriptor;

import java.util.Arrays;
import java.util.List;

public class JUnit3TestNameFoldingTest extends TestNameEditorTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addClass("package junit.framework; public abstract class TestCase {}");
    }

    public void testFoldsTestCaseMethodDeclarationOnly() {
        myFixture.configureByText("Example.java", """
                class Example extends junit.framework.TestCase {
                    public void test_saves_order() {}
                    public void helper_method() { test_saves_order(); }
                }
                """);

        FoldingDescriptor[] folds = descriptors();

        assertEquals(1, folds.length);
        assertEquals("test saves order", folds[0].getPlaceholderText());
        assertEquals(myFixture.getFile().getText().indexOf("test_saves_order"),
                folds[0].getRange().getStartOffset());
        assertEquals("test_saves_order", folds[0].getRange().substring(myFixture.getFile().getText()));
    }

    public void testRecognizesIndirectTestCaseSubclassWithCamelCase() {
        myFixture.addClass("public abstract class BaseTest extends junit.framework.TestCase {}");
        myFixture.configureByText("Example.java", """
                class Example extends BaseTest {
                    public void testApplyingCamelCaseRefreshesExistingFold() {}
                }
                """);
        assertEquals(0, descriptors().length);

        TestSpaceSettings.getInstance().loadState(new TestSpaceSettings.Options(true, true, true, false));

        assertDisplays("test applying camel case refreshes existing fold");
    }

    public void testUsesReferencedMethodCaseInCamelCaseName() {
        TestSpaceSettings.getInstance().loadState(new TestSpaceSettings.Options(true, true, true, false));
        myFixture.configureByText("Example.java", """
                class Example extends junit.framework.TestCase {
                    public void testFindById() { findById(); }
                    private void findById() {}
                }
                """);

        assertDisplays("test findById");
    }

    public void testRejectsMethodsOutsideJUnit3Convention() {
        myFixture.configureByText("Example.java", """
                class Example extends junit.framework.TestCase {
                    public void test_valid() {}
                    private void test_private() {}
                    protected void test_protected() {}
                    void test_package_private() {}
                    public int test_returns_value() { return 1; }
                    public void test_with_parameter(int value) {}
                    public void helper_method() {}
                    public void Test_wrong_case() {}
                }
                """);

        assertDisplays("test valid");
    }

    public void testDoesNotRecognizeUnrelatedTestCaseOrTestPrefix() {
        myFixture.configureByText("Example.java", """
                class TestCase {}
                class Example extends TestCase {
                    public void test_saves_order() {}
                }
                class Helper {
                    public void test_saves_order() {}
                }
                """);

        assertEquals(0, descriptors().length);
    }

    private void assertDisplays(String... expected) {
        assertEquals(List.of(expected), Arrays.stream(descriptors())
                .map(FoldingDescriptor::getPlaceholderText).toList());
    }
}
