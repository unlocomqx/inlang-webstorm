package net.prestalife.inlang.intentions

import com.esotericsoftware.kryo.kryo5.minlog.Log
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.xml.XmlText
import com.intellij.util.xml.XmlName
import net.prestalife.inlang.utils.InlangUtils

class ExtractInlangMessageIntention : PsiElementBaseIntentionAction() {
    override fun getText(): String {
        return familyName
    }

    override fun getFamilyName(): String {
        return "Extract inlang message"
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        Log.info(element.elementType.toString())
        return element.parent is XmlText ||
                element is XmlText ||
                element.elementType.toString() == "XML_NAME" ||
                element.elementType.toString() == "SVELTE_HTML_TAG" ||
                element.parent.elementType.toString() == "SVELTE_HTML_TAG"
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        if (editor == null) {
            return
        }

        if (element.containingFile != null) {
            val result = InlangUtils.saveMessage(
                project,
                editor,
                element
            )

            if (result.first) {

            } else {
                // show error message

            }
        }
    }

}
