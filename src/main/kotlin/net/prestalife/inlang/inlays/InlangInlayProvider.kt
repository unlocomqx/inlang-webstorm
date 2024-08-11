package net.prestalife.inlang.inlays

import com.esotericsoftware.kryo.kryo5.minlog.Log
import com.intellij.codeInsight.hints.declarative.*
import com.intellij.lang.javascript.macro.JSMacroUtil
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.intellij.psi.util.endOffset
import net.prestalife.inlang.utils.InlangUtils

class InlangInlayProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? {
        if (!file.name.endsWith(".svelte")) {
            return null
        }
        return ThreadingCollector(file)
    }

    private class ThreadingCollector(file:PsiFile) : SharedBypassCollector {

        private var messages = mutableMapOf<String, String>()

        init {
            messages = InlangUtils.readMessages(file)
        }

        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {

            Log.info(element.elementType.toString())
            if (element is JSCallExpression) {
                if (element.parent.elementType.toString() !== "CONTENT_EXPRESSION") {
                    return
                }

                if (!element.text.trim().startsWith("m.")) {
                    return
                }

                element.children.find { it is JSReferenceExpression }?.let {
                    val key = it.text.replace("m.", "")
                    if (messages.containsKey(key) && messages[key] != "") {
                        sink.addPresentation(
                            InlineInlayPosition(element.endOffset, true),
                            tooltip = element.text,
                            hasBackground = true,
                        ) {
                            text(messages[key] ?: "")
                        }
                    }
                }
            }
        }
    }
}
