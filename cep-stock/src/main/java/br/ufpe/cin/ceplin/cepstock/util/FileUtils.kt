package br.ufpe.cin.ceplin.cepstock.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class FileUtils {

    companion object {
        fun readRawResourceToJsonString(context: Context, resId: Int) : String {
            val stringBuilder = StringBuilder()
            var line: String?

            val inputStream = context.resources.openRawResource(resId)
            val streamReader = BufferedReader(InputStreamReader(inputStream))

            line = streamReader.readLine()
            while (line != null) {
                stringBuilder.append(line)
                line = streamReader.readLine()
            }

            return stringBuilder.toString()
        }
    }
}