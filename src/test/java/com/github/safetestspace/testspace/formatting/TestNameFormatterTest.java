package com.github.safetestspace.testspace.formatting;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TestNameFormatterTest {
    @Test
    public void replacesEachUnderscoreWithoutChangingCase() {
        assertEquals("should save Order", TestNameFormatter.format("should_save_Order", true, false));
        assertEquals(" given  then ", TestNameFormatter.format("_given__then_", true, false));
    }

    @Test
    public void splitsIntoLowercaseWordsAndKeepsAcronyms() {
        assertEquals("should save order", TestNameFormatter.format("shouldSaveOrder", false, true));
        assertEquals("handles HTTPError", TestNameFormatter.format("handlesHTTPError", false, true));
        assertEquals("HTTP", TestNameFormatter.format("HTTP", false, true));
        assertEquals("version2 works", TestNameFormatter.format("version2Works", false, true));
        assertEquals("über größe", TestNameFormatter.format("überGröße", false, true));
        assertEquals("get AValue", TestNameFormatter.format("getAValue", false, true));
        assertEquals("Should save order", TestNameFormatter.format("ShouldSaveOrder", false, true));
        assertEquals("should Save order", TestNameFormatter.format("should_SaveOrder", true, true));
    }

    @Test
    public void keepsAdjacentUppercaseLettersTogether() {
        assertEquals("JUnit3", TestNameFormatter.format("JUnit3", true, true));
        assertEquals("supports JUnit3 tests", TestNameFormatter.format("supportsJUnit3Tests", true, true));
        assertEquals("JUnit3 test", TestNameFormatter.format("JUnit3_test", true, true));
        assertEquals("uses XMLParser", TestNameFormatter.format("usesXMLParser", true, true));
    }

    @Test
    public void supportsIndependentOptions() {
        assertEquals("should_return HTTPError", TestNameFormatter.format("should_returnHTTPError", false, true));
        assertEquals("should return HTTPError", TestNameFormatter.format("should_returnHTTPError", true, true));
        assertEquals("should_returnHTTPError", TestNameFormatter.format("should_returnHTTPError", false, false));
        assertEquals("already readable", TestNameFormatter.format("already readable", true, true));
        assertEquals("", TestNameFormatter.format("", true, true));
    }

    @Test
    public void keepsReferencedNamesTogether() {
        Set<String> referenced = Set.of("getUserName", "HttpClient");
        assertEquals("getUserName returns null",
                TestNameFormatter.format("getUserName_returnsNull", true, true, referenced));
        assertEquals("calls getUserName once",
                TestNameFormatter.format("callsGetUserNameOnce", true, true, referenced));
        assertEquals("uses HttpClient",
                TestNameFormatter.format("usesHttpClient", true, true, referenced));
        assertEquals("get user names",
                TestNameFormatter.format("getUserNames", true, true, referenced));
        assertEquals("getUserName",
                TestNameFormatter.format("getUserName", true, false, referenced));
    }

    @Test
    public void restoresReferencedIdentifierCaseAfterSplit() {
        assertEquals("test findById",
                TestNameFormatter.format("testFindById", true, true, Set.of("findById")));
        assertEquals("test folds TestCase method declaration only",
                TestNameFormatter.format("testFoldsTestCaseMethodDeclarationOnly", true, true,
                        Set.of("folds", "TestCase")));
        assertEquals("uses HttpClient",
                TestNameFormatter.format("usesHttpClient", true, true, Set.of("HttpClient")));
    }
}
