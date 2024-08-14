package net.prestalife.inlang.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/*******************************************************************************
 * Copyright (C) 2017 wysohn
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

object JarUtil {
    private const val JAR_SEPARATOR: Char = '/'

    @JvmOverloads
    @Throws(IOException::class)
    fun copyFolderFromJar(folderName: String, destFolder: File, option: CopyOption, trimmer: PathTrimmer? = null) {
        if (!destFolder.exists()) destFolder.mkdirs()

        val buffer = ByteArray(1024)

        var fullPath: File? = null
        var path = JarUtil::class.java.protectionDomain.codeSource.location.path
        if (trimmer != null) path = trimmer.trim(path)
        try {
            if (!path.startsWith("file")) path = "file://$path"

            fullPath = File(URI(path))
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
        val zis = ZipInputStream(FileInputStream(fullPath))

        var entry: ZipEntry?
        while ((zis.nextEntry.also { entry = it }) != null) {
            if (!entry!!.name.startsWith(folderName + JAR_SEPARATOR)) continue

            val fileName = entry!!.name

            if (fileName[fileName.length - 1] == JAR_SEPARATOR) {
                val file = File(destFolder.toString() + File.separator + fileName)
                if (file.isFile) {
                    file.delete()
                }
                file.mkdirs()
                continue
            }

            val file = File(destFolder.toString() + File.separator + fileName)
            if (option == CopyOption.COPY_IF_NOT_EXIST && file.exists()) continue

            if (!file.parentFile.exists()) file.parentFile.mkdirs()

            if (!file.exists()) file.createNewFile()
            val fos = FileOutputStream(file)

            var len: Int
            while ((zis.read(buffer).also { len = it }) > 0) {
                fos.write(buffer, 0, len)
            }
            fos.close()
        }

        zis.closeEntry()
        zis.close()
    }

    @Throws(IOException::class)
    @JvmStatic
    fun main(ar: Array<String>) {
        copyFolderFromJar("SomeFolder", File(""), CopyOption.REPLACE_IF_EXIST)
    }

    enum class CopyOption {
        COPY_IF_NOT_EXIST, REPLACE_IF_EXIST
    }

    fun interface PathTrimmer {
        fun trim(original: String?): String
    }
}