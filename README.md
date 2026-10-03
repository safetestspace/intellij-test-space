# Test Space

IntelliJ IDEA plugin that makes Java test names readable.

Kotlin lets you write test method names with spaces using backticks.
Jest takes a string as the test name, like `test('should save an order', ...)`.
Both read better, so Test Space brings this readability to Java, at least in the editor.

```text
should_save_an_order  →  should save an order
shouldSaveAnOrder     →  should save an order
```

Only test method declarations change visually.
Source code, references, and test runner names stay unchanged.
Move the caret onto a name to edit the original identifier.

Supports JUnit 3 (`TestCase`), JUnit 4, JUnit Jupiter, TestNG method annotations, and composed test annotations.
Requires IntelliJ IDEA 2024.3 or newer.

Configure formatting in **Settings → Editor → Test Space** and styling in **Editor → Color Scheme → Test Space**.
Add preserved names (one per line) to keep their spelling during camelCase splitting; `TestNg` is included by default.
