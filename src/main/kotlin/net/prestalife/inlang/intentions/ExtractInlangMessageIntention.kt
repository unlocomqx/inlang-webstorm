package net.prestalife.inlang.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlText
import net.prestalife.inlang.utils.InlangUtils

class ExtractInlangMessageIntention : PsiElementBaseIntentionAction() {
    override fun getText(): String {
        return familyName
    }

    override fun getFamilyName(): String {
        return "Extract inlang message"
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return element.parent is XmlText
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val selection = editor?.selectionModel?.selectedText ?: element.parent.text

        if (selection.isEmpty()) {
            return
        }

        val fnName = InlangUtils.generateFunctionName(selection)
        val message = "{m.$fnName()}"

        if (element.containingFile != null) {
            val result = InlangUtils.saveMessage(
                project,
                element.containingFile,
                fnName,
                selection
            )

            if (result.first) {
                editor?.document?.replaceString(
                    element.parent.textRange.startOffset,
                    element.parent.textRange.endOffset,
                    message
                )
            } else {
                // show error message

            }
        }
    }

}
