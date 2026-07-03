package com.hexaghost.flixora

import com.hexaghost.flixora.data.provider.JsProviderEngine
import okhttp3.OkHttpClient
import org.junit.Test
import java.io.File

class JsProviderEngineTest {

    @Test
    fun testResolveStreams() {
        val client = OkHttpClient()
        val engine = JsProviderEngine(client)

        // Download the 4khdhub JS provider file directly for testing
        println("Downloading 4khdhub.js...")
        val request = okhttp3.Request.Builder()
            .url("https://raw.githubusercontent.com/yoruix/nuvio-providers/refs/heads/main/providers/4khdhub.js")
            .build()
        val response = client.newCall(request).execute()
        assert(response.isSuccessful)
        val jsCode = response.body?.string() ?: error("Empty code")

        val tempFile = File.createTempFile("4khdhub", ".js")
        tempFile.writeText(jsCode)
        tempFile.deleteOnExit()

        println("Executing provider JS...")
        
        // Print preprocessed code around line 38 to see what's throwing
        var debugJs = engine.javaClass.getDeclaredMethod("stripDefaultParameters", String::class.java).run {
            isAccessible = true
            invoke(engine, jsCode) as String
        }
        debugJs = debugJs.replace(".apply(__this, __arguments)", ".apply(__this || global, __arguments || [])")
        
        // Inject helper wrapper for diagnostics on esbuild's __copyProps
        val copyPropsRegex = Regex("""var\s+__copyProps\s*=\s*\(\s*to\s*,\s*from\s*,\s*except\s*,\s*desc\s*\)\s*=>\s*\{""")
        debugJs = copyPropsRegex.replace(debugJs) {
            """
            var __copyProps = (to, from, except, desc) => {
                try {
                    if (from && (typeof from === "object" || typeof from === "function")) {
                        var keys = __getOwnPropNames(from);
                        for (var i = 0; i < keys.length; i++) {
                            var key = keys[i];
                            try {
                                    if (!__hasOwnProp.call(to, key) && key !== except) {
                                    var d = __getOwnPropDesc(from, key);
                                    __defProp(to, key, { get: (function(k) { return function() { return from[k]; }; })(key), enumerable: !d || d.enumerable });
                                }
                            } catch(innerEx) {
                                console.error("Inner copy error for key: " + key + ", error: " + innerEx);
                                throw innerEx;
                            }
                        }
                    }
                } catch(ex) {
                    console.error("Outer copy error: " + ex);
                    throw ex;
                }
                return to;
            };
            var __copyProps_original_ignored = function(to, from, except, desc) {
            """.trimIndent()
        }

        // Inject console.log diagnostic into __async helper
        debugJs = debugJs.replace("var __async = (__this, __arguments, generator) => {", """
            var __async = (__this, __arguments, generator) => {
                console.log("[__async] called with: __this=" + (typeof __this) + ", __arguments=" + (typeof __arguments) + ", generator=" + (typeof generator));
        """.trimIndent())

        val lines = debugJs.lines()
        println("=== PREPROCESSED JS LINES 15-55 ===")
        for (i in 14..54) {
            if (i < lines.size) {
                println("${i + 1}: ${lines[i]}")
            }
        }
        println("===================================")

        val result = kotlinx.coroutines.runBlocking {
            engine.resolveStreams(
                jsFile = tempFile,
                tmdbId = 36657,
                title = "X-Men",
                mediaType = "movie",
                year = 2000,
                providerName = "4KHDHub",
                season = 0,
                episode = 0
            )
        }

        println("Execution Result: $result")
        if (result.isSuccess) {
            val streams = result.getOrThrow()
            println("Found ${streams.size} streams:")
            streams.forEach { stream ->
                println(" - Quality: ${stream.quality}, Provider: ${stream.providerName}, URL: ${stream.url}")
            }
        } else {
            println("Failed to resolve streams:")
            result.exceptionOrNull()?.printStackTrace()
        }
    }

    @Test
    fun testRhinoGenerator() {
        val rhino = org.mozilla.javascript.Context.enter()
        rhino.optimizationLevel = -1
        rhino.languageVersion = org.mozilla.javascript.Context.VERSION_ES6
        try {
            val scope = rhino.initStandardObjects()
            val js = """
                try {
                    var gen = function* () {
                        yield 1;
                    };
                    
                    var __applyGenerator = function(fn, thisArg, args) {
                        var a = args || [];
                        switch (a.length) {
                            case 0: return fn();
                            case 1: return fn(a[0]);
                            case 2: return fn(a[0], a[1]);
                            case 3: return fn(a[0], a[1], a[2]);
                            case 4: return fn(a[0], a[1], a[2], a[3]);
                            default: return fn(a[0], a[1], a[2], a[3], a[4]);
                        }
                    };
                    
                    var instance5 = __applyGenerator(gen, null, []);
                    java.lang.System.out.println("Test 5 OK: " + instance5.next().value);
                    
                } catch(e) {
                    java.lang.System.err.println("Generator test error: " + e);
                    throw e;
                }
            """.trimIndent()
            rhino.evaluateString(scope, js, "test.js", 1, null)
        } finally {
            org.mozilla.javascript.Context.exit()
        }
    }
}
