package net.prestalife.inlang.utils

import java.io.File
import java.io.IOException
import java.net.JarURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarEntry
import java.util.jar.JarFile


fun getResource(path: String): URL? =
    object {}.javaClass.getResource(path)

@Throws(IOException::class)
fun copyJarResourcesRecursively(destination: Path, jarConnection: JarURLConnection) {
    val jarFile: JarFile = jarConnection.getJarFile()
    val it: Iterator<JarEntry> = jarFile.entries().asIterator()
    while (it.hasNext()) {
        val entry: JarEntry = it.next()
        if (entry.getName().startsWith(jarConnection.getEntryName())) {
            if (!entry.isDirectory()) {
                jarFile.getInputStream(entry).use { entryInputStream ->
                    Files.copy(entryInputStream, Paths.get(destination.toString(), entry.getName()))
                }
            } else {
                Files.createDirectories(Paths.get(destination.toString(), entry.getName()))
            }
        }
    }
}

fun copyResourceFileInsideJarToPath(resourceName: String, toPath: File): Path? {
    val fromURL: URL = getResource(resourceName) ?: return null

    JarUtil.copyFolderFromJar(fromURL.path, toPath, JarUtil.CopyOption.COPY_IF_NOT_EXIST)

    return toPath.toPath()
}