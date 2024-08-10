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
        val selection = editor?.selectionModel?.selectedText?.trim() ?: element.parent.text.trim()

        if (selection.isEmpty()) {
            return
        }

        if(editor == null) {
            return
        }

        val fnName = InlangUtils.generateFunctionName(selection)

        if (element.containingFile != null) {
            val result = InlangUtils.saveMessage(
                project,
                editor,
                element,
                fnName,
                selection
            )

            if (result.first) {

            } else {
                // show error message

            }
        }
    }

}
