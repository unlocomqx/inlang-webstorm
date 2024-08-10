package net.prestalife.inlang.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*


@Serializable
data class Settings(val sourceLanguageTag: String)

class InlangUtils {
    companion object {
        fun generateFunctionName(selection: String): String {
            // convert selection to a valid javascript function name
            // cut at 20 characters
            return selection.replace(" ", "_")
                .lowercase(Locale.getDefault())
                .take(20)
        }

        fun saveMessage(
            project: Project,
            editor: Editor,
            element: PsiElement,
            fnName: String,
            selection: String
        ): Pair<Boolean, String> {
            val psiFile = element.containingFile
            var file: VirtualFile = psiFile.virtualFile ?: return Pair(false, "No changes made")

            val folderName = "project.inlang"
            var folder: VirtualFile = file
            while (file.parent != null) {
                val folders = file.parent.children
                if (folders.any { it.name == folderName }) {
                    // save message to inlang folder
                    folder = folders.first { it.name == folderName }
                }
                file = file.parent
            }

            if (folder.name != folderName) {
                return Pair(false, "The project does not contain a folder named $folderName")
            }

            val settings = folder.findChild("settings.json") ?: return Pair(
                false,
                "The project does not contain a settings.json file"
            )
            // parse json
            val decodeFormat = Json { ignoreUnknownKeys = true }
            val json = decodeFormat.decodeFromString<Settings>(settings.inputStream.reader().readText())
            val jsonFile = folder.parent.findChild("messages")?.findFile("${json.sourceLanguageTag}.json")
                ?: return Pair(false, "The project does not contain a messages/${json.sourceLanguageTag}.json file")

            val str = ",\"$fnName\": \"$selection\""

            val document: Document = jsonFile.findDocument() ?: return Pair(false, "No changes made")
            CommandProcessor.getInstance().executeCommand(project, {
                ApplicationManager.getApplication().runWriteAction {
                    val lastBraceIndex = document.text.lastIndexOf('}')
                    document.insertString(lastBraceIndex, str)

                    val jsonPsiFile = jsonFile.findPsiFile(project) ?: return@runWriteAction
                    val codeStyleManager = CodeStyleManager.getInstance(project)
                    codeStyleManager.reformatText(jsonPsiFile, lastBraceIndex, lastBraceIndex + str.length)

                    val message = "{m.$fnName()}"
                    editor.document.replaceString(
                        element.parent.textRange.startOffset,
                        element.parent.textRange.endOffset,
                        message
                    )

                    PsiDocumentManager.getInstance(project).commitDocument(editor.document);
                    PsiDocumentManager.getInstance(project).commitDocument(document);

                    Importer.insertImport(editor, psiFile, "import * as m from '\$lib/paraglide/messages';")
                }
            }, "Extract Inlang Message", null)


            return Pair(true, "Message saved")
        }
    }

}
