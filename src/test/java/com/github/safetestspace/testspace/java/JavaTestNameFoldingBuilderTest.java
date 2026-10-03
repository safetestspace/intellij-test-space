package com.github.safetestspace.testspace.java;

import com.github.safetestspace.testspace.editor.TestNameEditorTestCase;

import com.github.safetestspace.testspace.settings.TestSpaceSettings;
import com.intellij.lang.folding.FoldingDescriptor;

import java.util.Arrays;
import java.util.List;

public class JavaTestNameFoldingBuilderTest extends TestNameEditorTestCase {
    public void testOnlyAnnotatedMethodDeclarationIsFolded() {
        myFixture.configureByText("Example.java", """
                import org.junit.jupiter.api.Test;
                class Example {
                    @Test void should_save_order() { helper_method(); }
                    void helper_method() { should_save_order(); }
                    String value = "should_save_order";
                }
                """);
        FoldingDescriptor[] folds = descriptors();
        assertEquals(1, folds.length);
        assertEquals("should save order", folds[0].getPlaceholderText());
        assertEquals("should_save_order", folds[0].getRange().substring(myFixture.getFile().getText()));
        assertEquals(myFixture.getFile().getText().indexOf("should_save_order"), folds[0].getRange().getStartOffset());
    }

    public void testSupportsFullyQualifiedJUnit4AndTestNg() {
        myFixture.addClass("package org.junit; public @interface Test {}");
        myFixture.addClass("package org.testng.annotations; public @interface Test {}");
        myFixture.configureByText("Example.java", """
                class Example {
                    @org.junit.Test void junit_test() {}
                    @org.testng.annotations.Test void testng_test() {}
                }
                """);
        assertDisplays("junit test", "testng test");
    }

    public void testSupportsParameterizedTest() {
        myFixture.addClass("package org.junit.jupiter.params; public @interface ParameterizedTest {}");
        myFixture.configureByText("Example.java", """
                import org.junit.jupiter.params.ParameterizedTest;
                class Example {
                    @ParameterizedTest void saves_each_order(int order) {}
                }
                """);
        FoldingDescriptor[] folds = descriptors();
        assertEquals(1, folds.length);
        assertEquals("saves each order", folds[0].getPlaceholderText());
    }

    public void testSupportsOtherJupiterAndComposedTestAnnotations() {
        myFixture.addClass("package org.junit.jupiter.api; public @interface RepeatedTest { int value(); }");
        myFixture.addClass("package org.junit.jupiter.api; public @interface TestFactory {}");
        myFixture.addClass("package org.junit.jupiter.api; public @interface TestTemplate {}");
        myFixture.addClass("package org.junit.platform.commons.annotation; public @interface Testable {}");
        myFixture.addClass("""
                package net.jqwik.api;
                @org.junit.platform.commons.annotation.Testable public @interface Property {}
                """);
        myFixture.configureByText("Example.java", """
                import org.junit.jupiter.api.*;
                @Test @interface FastTest {}
                @FastTest @interface VeryFastTest {}
                class Example {
                    @RepeatedTest(3) void repeated_test() {}
                    @TestFactory Object dynamic_tests() { return null; }
                    @TestTemplate void template_test() {}
                    @FastTest void composed_test() {}
                    @VeryFastTest void nested_composed_test() {}
                    @net.jqwik.api.Property void property_test() {}
                }
                """);
        assertDisplays("repeated test", "dynamic tests", "template test", "composed test",
                "nested composed test", "property test");
    }

    public void testDoesNotFoldUnrelatedTestAnnotations() {
        myFixture.configureByText("Example.java", """
                @interface Test {}
                class Example { @Test void custom_test() {} }
                """);
        assertEquals(0, descriptors().length);
    }

    public void testUnchangedAndBlankNamesAreNotFolded() {
        myFixture.configureByText("Example.java", """
                import org.junit.jupiter.api.Test;
                class Example {
                    @Test void plain() {}
                    @Test void ____() {}
                }
                """);
        assertEquals(0, descriptors().length);
    }

    public void testCamelCaseFoldingRequiresOptIn() {
        myFixture.configureByText("Example.java", """
                import org.junit.jupiter.api.Test;
                class Example { @Test void shouldSaveHTTPError() {} }
                """);
        assertEquals(0, descriptors().length);

        TestSpaceSettings.getInstance().loadState(new TestSpaceSettings.Options(true, true, true, false));

        assertDisplays("should save HTTPError");
    }

    public void testDisabledPluginProducesNoDescriptors() {
        openSimpleTest();
        TestSpaceSettings.getInstance().loadState(new TestSpaceSettings.Options(false, true, true, false));

        assertEquals(0, descriptors().length);
    }

    public void testCamelCaseKeepsNamesUsedInTestBody() {
        myFixture.configureByText("Example.java", """
                import org.junit.jupiter.api.Test;
                class Example {
                    String userName;
                    String getUserName() { return userName; }
                    @Test void getUserName_returnsUserName() { getUserName(); }
                }
                """);
        TestSpaceSettings.getInstance().loadState(new TestSpaceSettings.Options(true, true, true, false));
        assertDisplays("getUserName returns user name");
    }

    private void assertDisplays(String... expected) {
        assertEquals(List.of(expected), Arrays.stream(descriptors())
                .map(FoldingDescriptor::getPlaceholderText).toList());
    }
}
