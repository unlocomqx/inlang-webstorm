package net.prestalife.inlang.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.elementType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.JarURLConnection
import java.nio.file.Files
import java.nio.file.Path


@Serializable
data class Settings(val sourceLanguageTag: String)

class InlangUtils {
    companion object {
        private fun generateFunctionName(selection: String): String? {
            // generate random human-readable function name
            // https://github.com/opral/monorepo/issues/1892

            val script = getScript("/node") ?: return null
            val result = ScriptRunner.run("node \"${script}/src/random-id.mjs\"")
            return result ?: selection
        }

        private fun getScript(scriptName: String): Path? {
            val script = getResource(scriptName) ?: return null
            val tmpdir = System.getProperty("java.io.tmpdir")
            val tmpPath = Path.of(tmpdir)
            val scriptPath = Path.of(tmpdir, script.file.substringAfterLast('/'))
            if (!Files.exists(scriptPath)) {
                copyJarResourcesRecursively(tmpPath, script.openConnection() as JarURLConnection)
            }
            return scriptPath
        }

        fun saveMessage(
            project: Project,
            editor: Editor,
            element: PsiElement,
        ): Pair<Boolean, String> {
            val psiFile = element.containingFile
            val file: VirtualFile = psiFile.virtualFile ?: return Pair(false, "No changes made")

            val folder: VirtualFile = findClosestFolder(file, "project.inlang")
                ?: return Pair(false, "The project does not contain a folder named project.inlang")

            val settings = folder.findFile("settings.json") ?: return Pair(
                false,
                "The project does not contain a settings.json file"
            )
            // parse json
            val decodeFormat = Json { ignoreUnknownKeys = true }
            val json = decodeFormat.decodeFromString<Settings>(settings.inputStream.reader().readText())
            val jsonFile = folder.parent.findDirectory("messages")?.findFile("${json.sourceLanguageTag}.json")
                ?: return Pair(false, "The project does not contain a messages/${json.sourceLanguageTag}.json file")

            var selection = editor.selectionModel.selectedText?.trim()
            val effectiveElement = getEffectiveElement(element)

            if (selection.isNullOrEmpty()) {
                selection = effectiveElement.text
            }

            if (selection.isNullOrEmpty()) {
                return Pair(false, "No changes made")
            }

            val fnName =
                generateFunctionName(selection) ?: return Pair(false, "The function name could not be generated!")

            val str = ",\"$fnName\": \"$selection\""
            val message = "{m.$fnName()}"
            val start =
                if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionStart else effectiveElement.textRange.startOffset
            val end =
                if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionEnd else effectiveElement.textRange.endOffset

            val document: Document = jsonFile.findDocument() ?: return Pair(false, "No changes made")
            CommandProcessor.getInstance().executeCommand(project, {
                ApplicationManager.getApplication().runWriteAction {
                    val lastBraceIndex = document.text.lastIndexOf('}')
                    document.insertString(lastBraceIndex, str)

                    val jsonPsiFile = jsonFile.findPsiFile(project) ?: return@runWriteAction
                    val codeStyleManager = CodeStyleManager.getInstance(project)
                    codeStyleManager.reformatText(jsonPsiFile, lastBraceIndex, lastBraceIndex + str.length)

                    editor.document.replaceString(start, end, message)

                    PsiDocumentManager.getInstance(project).commitDocument(document)
                    PsiDocumentManager.getInstance(project).commitDocument(editor.document)

                    Importer.insertImport(editor, psiFile, "import * as m from '\$lib/paraglide/messages'")
                }
            }, "Extract Inlang Message", null)

            return Pair(true, "Message saved")
        }

        private fun getEffectiveElement(element: PsiElement): PsiElement {
            return when (element.elementType.toString()) {
                "XML_NAME" -> {
                    element
                }

                "SVELTE_HTML_TAG" -> {
                    element
                }

                else -> {
                    element.parent
                }
            }
        }

        private fun findClosestFolder(
            file: VirtualFile,
            folderName: String
        ): VirtualFile? {
            var file1 = file
            var folder: VirtualFile = file1
            while (file1.parent != null) {
                val folders = file1.parent.children
                if (folders.any { it.name == folderName }) {
                    // save message to inlang folder
                    folder = folders.first { it.name == folderName }
                }
                file1 = file1.parent
            }
            return if (folder.name == folderName) folder else null
        }

        fun readMessages(psiFile: PsiFile): MutableMap<String, String> {
            val file = psiFile.virtualFile ?: return mutableMapOf()
            val folder = findClosestFolder(file, "project.inlang") ?: return mutableMapOf()

            val settings = folder.findFile("settings.json") ?: return mutableMapOf()
            // parse json
            val decodeFormat = Json { ignoreUnknownKeys = true }
            val json = decodeFormat.decodeFromString<Settings>(settings.inputStream.reader().readText())
            val jsonFile = folder.parent.findDirectory("messages")?.findFile("${json.sourceLanguageTag}.json")
                ?: return mutableMapOf()

            val jsonString = jsonFile.inputStream.reader().readText()
            val gson = Gson()

            val mapAdapter = gson.getAdapter(object : TypeToken<Map<String, Any?>>() {})
            val model: Map<String, Any?> = mapAdapter.fromJson(jsonString)

            return model.mapValues { it.value.toString() }.toMutableMap()
        }
    }

}
