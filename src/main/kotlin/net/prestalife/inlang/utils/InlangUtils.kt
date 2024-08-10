package net.prestalife.inlang.utils

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.testFramework.utils.vfs.getDocument
import kotlinx.serialization.*
import kotlinx.serialization.json.*
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

        fun saveMessage(project: Project, psiFile: PsiFile, fnName: String, selection: String): Pair<Boolean, String> {
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

            val text = jsonFile.inputStream.reader().readText()
            val newText = text.replaceRange(text.lastIndexOf('}'), text.length, "$str\n}")
            jsonFile.setBinaryContent(newText.toByteArray())

            WriteCommandAction.runWriteCommandAction(project) {
                ReformatCodeProcessor(psiFile, false).run()
                PsiDocumentManager.getInstance(project).commitDocument(jsonFile.getDocument())
            }

            return Pair(true, "Message saved")
        }
    }

}
