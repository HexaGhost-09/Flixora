package com.hexaghost.flixora.data.provider

import android.util.Log
import com.hexaghost.flixora.domain.model.StreamResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.ScriptableObject
import okhttp3.OkHttpClient
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
class JsProviderEngine @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

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

    private fun fetchSync(url: String, options: Any?): Map<String, Any?> {
        val method = if (options is NativeObject) {
            options["method"]?.toString() ?: "GET"
        } else "GET"

        val headersMap = mutableMapOf<String, String>()
        if (options is NativeObject) {
            val headersObj = options["headers"]
            if (headersObj is NativeObject) {
                headersObj.ids.forEach { key ->
                    headersMap[key.toString()] = headersObj[key.toString()]?.toString() ?: ""
                }
            }
        }

        val bodyStr = if (options is NativeObject) {
            options["body"]?.toString()
        } else null

        val builder = okhttp3.Request.Builder().url(url)
        headersMap.forEach { (k, v) -> builder.addHeader(k, v) }

        if (method.equals("POST", ignoreCase = true) || method.equals("PUT", ignoreCase = true)) {
            val mediaType = headersMap["Content-Type"] ?: "application/json"
            val reqBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse(mediaType), bodyStr ?: "")
            builder.method(method.uppercase(), reqBody)
        } else {
            builder.method(method.uppercase(), null)
        }

        return try {
            val response = okHttpClient.newCall(builder.build()).execute()
            val responseBody = response.body?.string() ?: ""
            val responseCode = response.code

            val resHeaders = mutableMapOf<String, String>()
            response.headers.names().forEach { name ->
                resHeaders[name] = response.headers[name] ?: ""
            }

            mapOf(
                "status" to responseCode,
                "ok" to response.isSuccessful,
                "bodyText" to responseBody,
                "headers" to resHeaders
            )
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "fetchSync error: ${e.message}", e)
            mapOf(
                "status" to 500,
                "ok" to false,
                "bodyText" to (e.message ?: "Unknown error"),
                "headers" to emptyMap<String, String>()
            )
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

            // Inject fetchSync helper
            ScriptableObject.putProperty(scope, "__fetchSync", Context.javaToJS({ url: String, options: Any? ->
                fetchSync(url, options)
            }, scope))

            val polyfills = """
                var console = {
                    log: function(msg) { log(msg); },
                    error: function(msg) { log("ERROR: " + msg); },
                    warn: function(msg) { log("WARN: " + msg); },
                    info: function(msg) { log("INFO: " + msg); }
                };
                var setTimeout = function(fn, delay) { fn(); return 0; };
                var clearTimeout = function() {};
                var fetch = function(url, options) {
                    return new Promise(function(resolve, reject) {
                        try {
                            var result = __fetchSync(url, options);
                            var response = {
                                status: result.get("status"),
                                ok: result.get("ok"),
                                text: function() {
                                    return Promise.resolve(result.get("bodyText"));
                                },
                                json: function() {
                                    return Promise.resolve(JSON.parse(result.get("bodyText")));
                                }
                            };
                            resolve(response);
                        } catch (e) {
                            reject(e);
                        }
                    });
                };
            """.trimIndent()
            rhino.evaluateString(scope, polyfills, "polyfills.js", 1, null)

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

            // Call the resolve function and wait for promise
            rhino.evaluateString(scope, """
                var __resolvedResult = null;
                var __resolvedError = null;
                Promise.resolve(resolve(__info)).then(function(res) {
                    __resolvedResult = res;
                }).catch(function(err) {
                    __resolvedError = err ? err.toString() : "Unknown JS error";
                });
            """.trimIndent(), "call.js", 1, null)

            // Drain microtasks
            rhino.processMicrotasks()

            val resultRaw = scope.get("__resolvedResult", scope)
            val errorRaw = scope.get("__resolvedError", scope)

            if (errorRaw != null && errorRaw != ScriptableObject.NOT_FOUND) {
                Log.e(TAG, "JS resolve rejected: ${Context.toString(errorRaw)}")
            }

            parseJsResult(resultRaw, providerName)
        } catch (e: Exception) {
            Log.e(TAG, "JS execution failed: ${e.message}", e)
            emptyList()
        } finally {
            Context.exit()
        }
    }

    private fun parseJsResult(raw: Any?, providerName: String): List<StreamResult> {
        if (raw == null || raw == ScriptableObject.NOT_FOUND) return emptyList()
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
