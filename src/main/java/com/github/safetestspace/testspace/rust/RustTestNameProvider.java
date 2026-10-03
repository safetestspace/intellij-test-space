package com.github.safetestspace.testspace.rust;

import com.github.safetestspace.testspace.discovery.TestNameProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.rust.lang.RsFileType;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsReferenceElementBase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RustTestNameProvider implements TestNameProvider {
    private static final Set<String> TEST_ATTRIBUTES = Set.of(
            "test", "tokio::test", "async_std::test", "actix_rt::test", "actix_web::test",
            "rstest", "rstest::rstest", "test_case", "test_case::test_case",
            "test_matrix", "test_case::test_matrix");

    @Override
    public boolean supports(FileType fileType) {
        return fileType == RsFileType.INSTANCE;
    }

    @Override
    public List<Declaration> find(PsiElement root, boolean collectReferences) {
        List<Declaration> declarations = new ArrayList<>();
        for (RsFunction function : PsiTreeUtil.findChildrenOfType(root, RsFunction.class)) {
            PsiElement identifier = function.getNameIdentifier();
            if (identifier != null && PsiTreeUtil.getParentOfType(function, RsFunction.class) == null
                    && function.getOuterAttrList().stream().anyMatch(attr -> isTestAttribute(attr.getMetaItem()))) {
                declarations.add(new Declaration(identifier,
                        collectReferences ? referencedNames(function) : Set.of()));
            }
        }
        return declarations;
    }

    private static boolean isTestAttribute(RsMetaItem item) {
        return item.getPath() != null && TEST_ATTRIBUTES.contains(attributePath(item.getPath()));
    }

    private static String attributePath(RsPath path) {
        return path.getPath() == null ? path.getReferenceName()
                : attributePath(path.getPath()) + "::" + path.getReferenceName();
    }

    private static Set<String> referencedNames(RsFunction function) {
        PsiElement body = PsiTreeUtil.findChildOfType(function, RsBlock.class);
        if (body == null) return Set.of();
        Set<String> names = new HashSet<>();
        for (RsReferenceElementBase reference : PsiTreeUtil.findChildrenOfType(body, RsReferenceElementBase.class)) {
            names.add(reference.getReferenceName());
        }
        return names;
    }
}
