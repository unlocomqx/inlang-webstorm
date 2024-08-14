package net.prestalife.inlang.utils

import com.esotericsoftware.kryo.kryo5.minlog.Log
import java.io.BufferedReader
import java.io.InputStreamReader

object ScriptRunner {
    fun run(script: String): String? {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("/bin/sh", "-c", script))
            val output = StringBuilder()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String? = reader.readLine()
                if (line != null) {
                    output.append(line)
                }

                while (line != null) {
                    line = reader.readLine()
                    if (line != null) {
                        output.append(line)
                    }
                }
            }
            process.waitFor()
            return output.toString()
        } catch (e: Throwable) {
            Log.error(e.message)
        }
        return null
    }
}