package com.github.safetestspace.testspace.python;

import com.github.safetestspace.testspace.discovery.TestNameProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.PythonFileType;
import com.jetbrains.python.psi.PyAssignmentStatement;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyDecorator;
import com.jetbrains.python.psi.PyDecoratorList;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.PyQualifiedNameOwner;
import com.jetbrains.python.psi.PyRecursiveElementVisitor;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.jetbrains.python.psi.PyTargetExpression;
import com.jetbrains.python.psi.types.TypeEvalContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PythonTestNameProvider implements TestNameProvider {
    @Override
    public boolean supports(FileType fileType) {
        return fileType == PythonFileType.INSTANCE;
    }

    @Override
    public List<Declaration> find(PsiElement root, boolean collectReferences) {
        List<Declaration> declarations = new ArrayList<>();
        root.accept(new PyRecursiveElementVisitor() {
            @Override
            public void visitPyFunction(@NotNull PyFunction function) {
                // Use the stable PSI contract rather than the experimental Python AST accessor.
                PsiNameIdentifierOwner namedFunction = function;
                PsiElement identifier = namedFunction.getNameIdentifier();
                if (identifier != null && isTest(function, identifier.getText())) {
                    declarations.add(new Declaration(identifier,
                            collectReferences ? referencedNames(function) : Set.of()));
                }
                // Nested functions are helpers, not independently collected tests.
            }
        });
        return declarations;
    }

    private static boolean isTest(PyFunction function, String name) {
        if (!name.startsWith("test")) return false;
        PyDecoratorList decorators = function.getDecoratorList();
        if (decorators != null) {
            for (PyDecorator decorator : decorators.getDecorators()) {
                if (isFixture(decorator)) return false;
            }
        }
        PyClass owner = function.getContainingClass();
        TypeEvalContext context = TypeEvalContext.codeAnalysis(function.getProject(), function.getContainingFile());
        if (owner != null && (owner.isSubclass("unittest.TestCase", context)
                || owner.isSubclass("unittest.case.TestCase", context))) {
            return true;
        }
        String fileName = function.getContainingFile().getName();
        if (!(fileName.startsWith("test_") || fileName.endsWith("_test.py"))) return false;
        if (excludedFromPytest(function.getContainingFile())) return false;
        for (PyClass type = owner; type != null; type = PsiTreeUtil.getParentOfType(type, PyClass.class)) {
            PsiNamedElement namedClass = type;
            if (namedClass.getName() == null || !namedClass.getName().startsWith("Test")
                    || hasPytestOptOut(type, context)
                    || hasCustomConstructor(type, "__init__", context)
                    || hasCustomConstructor(type, "__new__", context)) return false;
        }
        return true;
    }

    private static boolean isFixture(PyDecorator decorator) {
        if (!(decorator.getCallee() instanceof PyReferenceExpression reference)) return false;
        PsiElement target = reference.getReference().resolve();
        if (target instanceof PyQualifiedNameOwner named) {
            String qualifiedName = named.getQualifiedName();
            return qualifiedName != null && Set.of("pytest.fixture", "pytest.yield_fixture", "_pytest.fixtures.fixture",
                    "_pytest.fixtures.yield_fixture").contains(qualifiedName);
        }
        String name = reference.getText();
        return Set.of("pytest.fixture", "pytest.yield_fixture", "fixture", "yield_fixture").contains(name);
    }

    private static boolean hasPytestOptOut(PyClass type, TypeEvalContext context) {
        PyTargetExpression attribute = type.findClassAttribute("__test__", true, context);
        PyExpression value = attribute == null ? null : attribute.findAssignedValue();
        return value != null && "False".equals(value.getText());
    }

    private static boolean hasCustomConstructor(PyClass type, String name, TypeEvalContext context) {
        PyFunction constructor = type.findMethodByName(name, true, context);
        if (constructor == null) return false;
        PyClass owner = constructor.getContainingClass();
        String qualifiedName = owner == null ? null : owner.getQualifiedName();
        return !"builtins.object".equals(qualifiedName) && !"__builtin__.object".equals(qualifiedName);
    }

    private static boolean excludedFromPytest(PsiElement scope) {
        for (PyAssignmentStatement assignment : PsiTreeUtil.getChildrenOfTypeAsList(scope, PyAssignmentStatement.class)) {
            PyExpression value = assignment.getAssignedValue();
            if (value == null || !"False".equals(value.getText())) continue;
            for (PyExpression target : assignment.getTargets()) {
                if ("__test__".equals(target.getText())) return true;
            }
        }
        return false;
    }

    private static Set<String> referencedNames(PyFunction function) {
        Set<String> names = new HashSet<>();
        function.getStatementList().accept(new PyRecursiveElementVisitor() {
            @Override
            public void visitPyReferenceExpression(@NotNull PyReferenceExpression expression) {
                names.add(referencedName(expression));
                super.visitPyReferenceExpression(expression);
            }
        });
        return names;
    }

    private static String referencedName(PyReferenceExpression expression) {
        return expression.getReference().getRangeInElement().substring(expression.getText());
    }
}
