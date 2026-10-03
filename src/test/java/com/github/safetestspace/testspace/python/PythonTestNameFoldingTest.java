package com.github.safetestspace.testspace.python;

import com.github.safetestspace.testspace.editor.LanguageTestNameEditorTestCase;
import com.github.safetestspace.testspace.settings.TestSpaceSettings;

public class PythonTestNameFoldingTest extends LanguageTestNameEditorTestCase {
    public void testPytestDeclarationsOnly() {
        myFixture.configureByText("test_checkout.py", """
                def test_saves_order():
                    test_saves_order()
                    def test_nested_helper():
                        pass
                async def test_handles_async_request():
                    pass
                def helper_method():
                    pass
                class TestCheckout:
                    def test_applies_discount(self):
                        pass
                class Helper:
                    def test_not_collected(self):
                        pass
                """);
        assertEquals("Python", myFixture.getFile().getLanguage().getID());
        assertDisplays("test saves order", "test handles async request", "test applies discount");
        assertEquals(myFixture.getFile().getText().indexOf("test_saves_order"),
                descriptors()[0].getRange().getStartOffset());
    }

    public void testHelpersAndFixturesAreNotTests() {
        myFixture.configureByText("test_checkout.py", """
                import pytest
                @pytest.fixture
                def test_data():
                    pass
                class TestWithConstructor:
                    def __init__(self):
                        pass
                    def test_not_collected(self):
                        pass
                """);
        assertDisplays();
        myFixture.configureByText("checkout.py", "def test_connection(): pass");
        assertDisplays();
    }

    public void testUnittestSubclassWithAliasesAndInheritance() {
        myFixture.addFileToProject("unittest/__init__.py", "class TestCase: pass");
        myFixture.configureByText("checks.py", """
                from unittest import TestCase as Base
                class Parent(Base):
                    pass
                class CheckoutChecks(Parent):
                    def testFindById(self):
                        self.findById()
                    def findById(self):
                        pass
                """);
        assertDisplays("test findById");
    }

    public void testPreservedNamesAndReferences() {
        myFixture.configureByText("checkout_test.py", """
                def testFindByIdWithTestNg():
                    findById()
                """);
        assertDisplays("test findById with TestNg");
        var options = TestSpaceSettings.getInstance().getState();
        options.preservedNames = "FindById\nTestNg";
        TestSpaceSettings.getInstance().loadState(options);
        assertDisplays("test FindById with TestNg");
    }

    public void testPytestOptOut() {
        myFixture.configureByText("test_checkout.py", """
                class TestHelper:
                    __test__ = False
                    def test_not_collected(self): pass
                def test_collected(): pass
                """);
        assertDisplays("test collected");
        myFixture.configureByText("test_checkout.py", """
                __test__ = False
                def test_not_collected(): pass
                """);
        assertDisplays();
    }

    public void testDefaultObjectConstructorDoesNotExcludePytestClass() {
        myFixture.addFileToProject("builtins.py", """
                class object:
                    def __init__(self): pass
                    def __new__(cls): pass
                """);
        myFixture.configureByText("test_checkout.py", """
                from builtins import object
                class TestCheckout(object):
                    def test_saves_order(self): pass
                class Base:
                    def __init__(self): pass
                class TestCustomConstructor(Base):
                    def test_not_collected(self): pass
                """);
        assertDisplays("test saves order");
    }

    public void testStyledNamesCaretAndSettingsRefresh() {
        myFixture.configureByText("test_checkout.py", """
                # Checkout tests
                def test_saves_order():
                    pass
                """);
        assertStyledLifecycle("test_saves_order", "test saves order");
    }

    public void testFixtureAliasesAndUnrelatedDecorator() {
        myFixture.addFileToProject("pytest/__init__.py", "def fixture(func): return func");
        myFixture.configureByText("test_checkout.py", """
                from pytest import fixture as data
                @data
                def test_fixture(): pass
                def fixture(func): return func
                @fixture
                def test_actual_test(): pass
                """);
        assertDisplays("test actual test");
    }

    public void testInheritedPytestOptOutCanBeOverridden() {
        myFixture.configureByText("test_checkout.py", """
                class Base:
                    __test__ = False
                class TestHelper(Base):
                    def test_not_collected(self): pass
                class TestCheckout(Base):
                    __test__ = True
                    def test_collected(self): pass
                """);
        assertDisplays("test collected");
    }
}
