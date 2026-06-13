package com.hexaghost.flixora.data.provider

import android.util.Log
import com.hexaghost.flixora.domain.model.StreamResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.ScriptableObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes a provider's JavaScript file using Mozilla Rhino (ES5 compatible engine).
 *
 * Expected JS contract:
 * ```js
 * // The provider JS file must export a resolve() function:
 * function resolve(info) {
 *   // info.tmdbId, info.title, info.type, info.year are available
 *   return [
 *     { url: "https://...", quality: "1080p", headers: {} }
 *   ];
 * }
 * ```
 */
@Singleton
class JsProviderEngine @Inject constructor() {

    private val TAG = "JsProviderEngine"

    suspend fun resolveStreams(
        jsFile: File,
        tmdbId: Int,
        title: String,
        mediaType: String,
        year: Int,
        providerName: String
    ): Result<List<StreamResult>> = withContext(Dispatchers.IO) {
        runCatching {
            val jsCode = jsFile.readText()
            executeJs(jsCode, tmdbId, title, mediaType, year, providerName)
        }
    }

    private fun executeJs(
        jsCode: String,
        tmdbId: Int,
        title: String,
        mediaType: String,
        year: Int,
        providerName: String
    ): List<StreamResult> {
        val rhino = Context.enter()
        rhino.optimizationLevel = -1 // Required for Android (no JIT compilation)
        rhino.languageVersion = Context.VERSION_ES6

        return try {
            val scope = rhino.initStandardObjects()

            // Inject a log function for provider debugging
            ScriptableObject.putProperty(scope, "log", Context.javaToJS({ msg: Any ->
                Log.d(TAG, "JS Provider: $msg")
            }, scope))

            // Execute the provider script
            rhino.evaluateString(scope, jsCode, "provider.js", 1, null)

            // Build the info object
            val infoJs = """
                var __info = {
                  tmdbId: $tmdbId,
                  title: ${kotlinStringToJs(title)},
                  type: "${mediaType}",
                  year: $year
                };
            """.trimIndent()
            rhino.evaluateString(scope, infoJs, "info.js", 1, null)

            // Call the resolve function
            val resultRaw = rhino.evaluateString(scope, "resolve(__info);", "call.js", 1, null)

            parseJsResult(resultRaw, providerName)
        } catch (e: Exception) {
            Log.e(TAG, "JS execution failed: ${e.message}", e)
            emptyList()
        } finally {
            Context.exit()
        }
    }

    private fun parseJsResult(raw: Any?, providerName: String): List<StreamResult> {
        if (raw == null) return emptyList()
        return when (raw) {
            is NativeArray -> {
                (0 until raw.length).mapNotNull { i ->
                    val item = raw[i]
                    if (item is NativeObject) parseStreamObject(item, providerName) else null
                }
            }
            else -> emptyList()
        }
    }

    private fun parseStreamObject(obj: NativeObject, providerName: String): StreamResult? {
        val url = obj["url"]?.toString() ?: return null
        if (url.isBlank()) return null
        val quality = obj["quality"]?.toString() ?: "Auto"
        val headersObj = obj["headers"]
        val headers = mutableMapOf<String, String>()
        if (headersObj is NativeObject) {
            headersObj.ids.forEach { key ->
                headers[key.toString()] = headersObj[key.toString()]?.toString() ?: ""
            }
        }
        return StreamResult(url = url, quality = quality, headers = headers, providerName = providerName)
    }

    /** Escapes a Kotlin string for safe embedding into JS source. */
    private fun kotlinStringToJs(str: String): String {
        return "\"${str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
    }
}
