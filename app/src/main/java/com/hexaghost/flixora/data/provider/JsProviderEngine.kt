package com.hexaghost.flixora.data.provider

import android.util.Log
import com.hexaghost.flixora.domain.model.StreamResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.ScriptableObject
import java.io.File
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes Nuvio-compatible JavaScript provider files using Mozilla Rhino.
 *
 * Nuvio provider JS files follow the CommonJS pattern:
 *   module.exports = { getStreams }
 *
 * getStreams signature:
 *   getStreams(tmdbId, mediaType, seasonNum, episodeNum) -> Promise<StreamResult[]>
 *
 * Where StreamResult has shape:
 *   { url, quality, name, title, headers, provider }
 *
 * The engine provides the following polyfills / shims:
 *   - CommonJS module system (require / module.exports / exports)
 *   - cheerio-without-node-native → backed by Jsoup (HTML parser)
 *   - fetch (synchronous HTTP backed by OkHttp)
 *   - console.{log, error, warn, info}
 *   - URL, URLSearchParams
 *   - atob, btoa
 *   - setTimeout, clearTimeout, setInterval, clearInterval (synchronous stubs)
 *   - process (with env stub)
 *   - global reference
 *   - Buffer (minimal base64 stub)
 *   - Promise (native Rhino ES6)
 *   - Date.now()
 */
@Singleton
class JsProviderEngine @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val TAG = "JsProviderEngine"

    // ── Public API ─────────────────────────────────────────────────────────────

    suspend fun resolveStreams(
        jsFile: File,
        tmdbId: Int,
        title: String,
        mediaType: String,
        year: Int,
        providerName: String,
        season: Int = 0,
        episode: Int = 0
    ): Result<List<StreamResult>> = withContext(Dispatchers.IO) {
        runCatching {
            val jsCode = jsFile.readText()
            executeProviderJs(jsCode, tmdbId, title, mediaType, year, providerName, season, episode)
        }
    }

    // ── HTTP Fetch ─────────────────────────────────────────────────────────────

    private fun doFetch(url: String, options: Any?): Map<String, Any?> {
        val opts = options as? NativeObject
        val method = opts?.get("method")?.toString() ?: "GET"
        val headersMap = mutableMapOf<String, String>()
        val headersObj = opts?.get("headers")
        if (headersObj is NativeObject) {
            headersObj.ids.forEach { key ->
                headersMap[key.toString()] = headersObj[key.toString()]?.toString() ?: ""
            }
        }
        val bodyStr = opts?.get("body")?.toString()
        val builder = okhttp3.Request.Builder().url(url)
        headersMap.forEach { (k, v) -> builder.addHeader(k, v) }

        if (method.equals("POST", ignoreCase = true) || method.equals("PUT", ignoreCase = true)) {
            val contentType = headersMap["Content-Type"] ?: "application/json"
            builder.method(method.uppercase(), (bodyStr ?: "").toRequestBody(contentType.toMediaTypeOrNull()))
        } else {
            builder.method(method.uppercase(), null)
        }

        return try {
            val response = okHttpClient.newCall(builder.build()).execute()
            val body = response.body?.string() ?: ""
            val respHeaders = mutableMapOf<String, String>()
            response.headers.names().forEach { n -> respHeaders[n] = response.header(n) ?: "" }
            mapOf("status" to response.code, "ok" to response.isSuccessful, "bodyText" to body, "headers" to respHeaders)
        } catch (e: Exception) {
            Log.e(TAG, "fetch error for $url: ${e.message}")
            mapOf("status" to 500, "ok" to false, "bodyText" to (e.message ?: ""), "headers" to emptyMap<String, String>())
        }
    }

    // ── Jsoup-backed HTML parsing (cheerio shim bridge) ────────────────────────

    /**
     * Parse HTML string via Jsoup and return a Java object that the JS cheerio-shim
     * can call methods on via Rhino's LiveConnect.
     */
    private fun parseHtml(html: String, baseUri: String = ""): JsoupDocument {
        return JsoupDocument(Jsoup.parse(html, baseUri))
    }

    // ── JS Execution ──────────────────────────────────────────────────────────

    private fun executeProviderJs(
        jsCode: String,
        tmdbId: Int,
        title: String,
        mediaType: String,
        year: Int,
        providerName: String,
        season: Int,
        episode: Int
    ): List<StreamResult> {
        val rhino = Context.enter()
        rhino.optimizationLevel = -1
        rhino.languageVersion = Context.VERSION_ES6

        return try {
            val scope = rhino.initStandardObjects()

            // ── Native Java bridges ──────────────────────────────────────────
            ScriptableObject.putProperty(scope, "__javaFetch",
                Context.javaToJS({ url: String, options: Any? -> doFetch(url, options) }, scope))

            ScriptableObject.putProperty(scope, "__jsoupParse",
                Context.javaToJS({ html: String, baseUri: String -> parseHtml(html, baseUri) }, scope))

            ScriptableObject.putProperty(scope, "__logTag", Context.javaToJS(TAG, scope))

            ScriptableObject.putProperty(scope, "__androidLog",
                Context.javaToJS({ tag: String, msg: Any -> Log.d(tag, msg.toString()) }, scope))

            // ── Polyfills ────────────────────────────────────────────────────
            rhino.evaluateString(scope, buildPolyfills(), "polyfills.js", 1, null)

            // ── CommonJS shim ─────────────────────────────────────────────────
            rhino.evaluateString(scope, buildCommonJsShim(), "commonjs.js", 1, null)

            // ── Cheerio shim (cheerio-without-node-native) ────────────────────
            rhino.evaluateString(scope, buildCheerioShim(), "cheerio.js", 1, null)

            // ── Execute provider code ─────────────────────────────────────────
            rhino.evaluateString(scope, jsCode, "provider.js", 1, null)

            // ── Capture module.exports after provider executes ────────────────
            // Providers either:
            //   1. Set module.exports = { getStreams } (CommonJS pattern)
            //   2. Declare global function getStreams (legacy)
            //   3. Declare global function resolve (Flixora-native pattern)
            rhino.evaluateString(scope, "__moduleExports = module.exports;", "capture.js", 1, null)

            // ── Build the info / invoke getStreams ─────────────────────────────
            val nuvioType = if (mediaType == "tv") "tv" else "movie"
            val titleJs = kotlinStringToJs(title)

            rhino.evaluateString(scope, """
                var __callResult = null;
                var __callError = null;
                (function() {
                    try {
                        var mod = __moduleExports;
                        var fn = null;
                        if (mod && typeof mod.getStreams === 'function') {
                            fn = mod.getStreams;
                        } else if (mod && mod['default'] && typeof mod['default'].getStreams === 'function') {
                            fn = mod['default'].getStreams;
                        } else if (typeof getStreams === 'function') {
                            fn = getStreams;
                        } else if (typeof scrape === 'function') {
                            fn = scrape;
                        } else if (typeof resolve === 'function') {
                            fn = function(id, type, s, e) {
                                return resolve({ tmdbId: id, title: $titleJs, type: type, year: $year });
                            };
                        }
                        if (!fn) {
                            __callError = 'No provider entry point found (expected module.exports.getStreams, getStreams, scrape, or resolve)';
                            return;
                        }
                        Promise.resolve(fn($tmdbId, '${nuvioType}', $season, $episode)).then(function(res) {
                            __callResult = res;
                        }).catch(function(err) {
                            __callError = err ? (err.message || err.toString()) : 'Unknown promise error';
                        });
                    } catch(e) {
                        __callError = e.message || e.toString();
                    }
                })();
            """.trimIndent(), "call.js", 1, null)

            // Drain microtask queue (nested async chains need multiple passes)
            repeat(30) { rhino.processMicrotasks() }

            val errorRaw = scope.get("__callError", scope)
            if (errorRaw != null && errorRaw != ScriptableObject.NOT_FOUND && errorRaw.toString() != "null") {
                Log.e(TAG, "[$providerName] JS error: $errorRaw")
            }

            val resultRaw = scope.get("__callResult", scope)
            Log.d(TAG, "[$providerName] raw result type=${resultRaw?.javaClass?.simpleName}")
            parseJsResult(resultRaw, providerName)
        } catch (e: Exception) {
            Log.e(TAG, "JS execution failed for provider '$providerName': ${e.message}", e)
            emptyList()
        } finally {
            Context.exit()
        }
    }

    // ── Polyfills ──────────────────────────────────────────────────────────────

    private fun buildPolyfills(): String = """
        // Logging
        var console = {
            log:   function() { var a = Array.prototype.slice.call(arguments); __androidLog(__logTag, a.join(' ')); },
            error: function() { var a = Array.prototype.slice.call(arguments); __androidLog(__logTag, 'ERROR: ' + a.join(' ')); },
            warn:  function() { var a = Array.prototype.slice.call(arguments); __androidLog(__logTag, 'WARN: '  + a.join(' ')); },
            info:  function() { var a = Array.prototype.slice.call(arguments); __androidLog(__logTag, 'INFO: '  + a.join(' ')); },
            debug: function() { var a = Array.prototype.slice.call(arguments); __androidLog(__logTag, 'DEBUG: ' + a.join(' ')); }
        };

        // Timers — synchronous stubs (Rhino has no event loop)
        var _timerCallbacks = [];
        var setTimeout = function(fn, delay) { _timerCallbacks.push(fn); return _timerCallbacks.length - 1; };
        var clearTimeout = function(id) { };
        var setInterval = function(fn, delay) { return 0; };
        var clearInterval = function(id) { };

        // Base64
        var atob = function(b64) {
            var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
            var str = String(b64).replace(/[=]+${'$'}/, '');
            var output = '';
            for (var bc = 0, bs, buffer, i = 0; buffer = str.charAt(i++);
                ~buffer && (bs = bc % 4 ? bs * 64 + buffer : buffer, bc++ % 4) ? output += String.fromCharCode(255 & bs >> (-2 * bc & 6)) : 0) {
                buffer = chars.indexOf(buffer);
            }
            return output;
        };
        var btoa = function(str) {
            var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
            var out = '', i = 0;
            while (i < str.length) {
                var c1 = str.charCodeAt(i++), c2 = str.charCodeAt(i++), c3 = str.charCodeAt(i++);
                out += chars[c1 >> 2] + chars[((c1 & 3) << 4) | (c2 >> 4)] +
                       (isNaN(c2) ? '=' : chars[((c2 & 15) << 2) | (c3 >> 6)]) +
                       (isNaN(c2) || isNaN(c3) ? '=' : chars[c3 & 63]);
            }
            return out;
        };

        // Buffer minimal shim
        var Buffer = {
            from: function(data, encoding) {
                if (encoding === 'base64') return { toString: function(enc) { return atob(data); } };
                return { toString: function(enc) {
                    if (enc === 'base64') return btoa(typeof data === 'string' ? data : String.fromCharCode.apply(null, data));
                    return typeof data === 'string' ? data : '';
                }};
            },
            alloc: function(size) { return { toString: function() { return ''; } }; }
        };

        // process shim
        var process = { env: {}, nextTick: function(fn) { fn(); }, version: 'v16.0.0', platform: 'android' };

        // URL shim (minimal)
        var URL = function(url, base) {
            var full = base ? (base.replace(/\/${'$'}/, '') + '/' + url.replace(/^\//, '')) : url;
            var parts = /^(https?:)?\/\/([^/?#]*)(.*?)(\?[^#]*)?(#.*)?${'$'}/.exec(full) || [];
            this.href     = full;
            this.protocol = (parts[1] || 'https:');
            this.host     = parts[2] || '';
            this.hostname = (parts[2] || '').split(':')[0];
            this.port     = ((parts[2] || '').split(':')[1]) || '';
            this.pathname = parts[3] || '/';
            this.search   = parts[4] || '';
            this.hash     = parts[5] || '';
            this.origin   = this.protocol + '//' + this.host;
            this.toString = function() { return this.href; };
        };
        URL.prototype.toString = function() { return this.href; };

        // URLSearchParams minimal shim
        var URLSearchParams = function(init) {
            this._params = {};
            if (typeof init === 'string') {
                init.replace(/^\?/, '').split('&').forEach(function(pair) {
                    var parts = pair.split('=');
                    if (parts[0]) this._params[decodeURIComponent(parts[0])] = decodeURIComponent(parts[1] || '');
                }, this);
            }
            this.get = function(key) { return this._params[key] || null; };
            this.set = function(key, val) { this._params[key] = val; };
            this.toString = function() {
                return Object.keys(this._params).map(function(k) {
                    return encodeURIComponent(k) + '=' + encodeURIComponent(this._params[k]);
                }, this).join('&');
            };
        };

        // global reference (CommonJS environments expect this)
        var global = this;

        // fetch polyfill (async wrapper around synchronous __javaFetch)
        var fetch = function(url, options) {
            return new Promise(function(resolve, reject) {
                try {
                    var result = __javaFetch(url, options || null);
                    var bodyText = result.get('bodyText') || '';
                    var status   = result.get('status') || 0;
                    var ok       = result.get('ok') || false;
                    var response = {
                        status: status,
                        ok: ok,
                        url: url,
                        headers: {
                            get: function(name) {
                                var h = result.get('headers');
                                if (!h) return null;
                                var val = h[name];
                                if (val !== undefined && val !== null) return val;
                                // Fallback to case-insensitive lookup
                                for (var k in h) {
                                    if (k.toLowerCase() === name.toLowerCase()) {
                                        return h[k];
                                    }
                                }
                                return null;
                            }
                        },
                        text: function() { return Promise.resolve(bodyText); },
                        json: function() {
                            return new Promise(function(res, rej) {
                                try { res(JSON.parse(bodyText)); } catch(e) { rej(e); }
                            });
                        },
                        arrayBuffer: function() { return Promise.resolve(bodyText); }
                    };
                    resolve(response);
                } catch(e) {
                    reject(e);
                }
            });
        };
    """.trimIndent()

    // ── CommonJS Module Shim ───────────────────────────────────────────────────

    private fun buildCommonJsShim(): String = """
        // CommonJS require/module/exports shim
        var __registry = {};
        var __moduleExports = null;

        var module = { exports: {} };
        var exports = module.exports;

        var require = function(moduleName) {
            // Return cached
            if (__registry[moduleName]) return __registry[moduleName];

            // Known built-in shims
            if (moduleName === 'cheerio-without-node-native' ||
                moduleName === 'react-native-cheerio' ||
                moduleName === 'cheerio') {
                return __cheerio;
            }
            if (moduleName === 'crypto-js' || moduleName === 'crypto') {
                return __cryptoStub;
            }
            if (moduleName === 'axios') {
                return __axiosStub;
            }
            // Unknown module — return empty object to avoid crash
            console.warn('require: unknown module "' + moduleName + '", returning {}');
            return {};
        };

        // Intercept module.exports writes: after provider executes, capture in __moduleExports
        // We wrap the whole provider execution in a factory so module/exports are local.
        // (Set after provider runs — see call.js)

        var __cryptoStub = {
            AES: { encrypt: function(s, k) { return { toString: function() { return btoa(s); } }; },
                   decrypt: function(s, k) { return { toString: function(e) { return atob(s.toString()); } }; } },
            enc: { Utf8: 'utf8', Base64: 'base64' },
            MD5: function(s) { return { toString: function() { return s; } }; },
            HmacSHA256: function(msg, key) { return { toString: function() { return ''; } }; }
        };

        var __axiosStub = {
            get: function(url, config) { return fetch(url, { method: 'GET', headers: (config && config.headers) || {} }); },
            post: function(url, data, config) { return fetch(url, { method: 'POST', headers: (config && config.headers) || {}, body: JSON.stringify(data) }); },
            create: function(defaults) { return __axiosStub; },
            defaults: { headers: { common: {} } }
        };
    """.trimIndent()

    // ── Cheerio Shim backed by Jsoup ──────────────────────────────────────────

    /**
     * Provides a cheerio-like API backed by Android Jsoup.
     * Covers the most common selectors used by Nuvio providers:
     * $('selector'), .find(), .text(), .attr(), .html(), .each(), .eq(), .first(), .length etc.
     */
    private fun buildCheerioShim(): String = """
        var __cheerio = (function() {
            // Wrapped Jsoup document/elements accessible via LiveConnect
            function CheerioWrap(jElements) {
                this._els = jElements || [];  // array of Jsoup Element Java objects
                this.length = this._els.length;
            }

            CheerioWrap.prototype.find = function(selector) {
                var all = [];
                for (var i = 0; i < this._els.length; i++) {
                    try {
                        var found = this._els[i].select(selector);
                        for (var j = 0; j < found.size(); j++) all.push(found.get(j));
                    } catch(e) {}
                }
                return new CheerioWrap(all);
            };

            CheerioWrap.prototype.text = function() {
                if (this._els.length === 0) return '';
                return this._els[0].text();
            };

            CheerioWrap.prototype.html = function() {
                if (this._els.length === 0) return '';
                return this._els[0].html();
            };

            CheerioWrap.prototype.attr = function(name) {
                if (this._els.length === 0) return undefined;
                try {
                    if (!this._els[0].hasAttr(name)) return undefined;
                    return this._els[0].attr(name);
                } catch(e) {
                    return undefined;
                }
            };

            CheerioWrap.prototype.each = function(fn) {
                for (var i = 0; i < this._els.length; i++) {
                    fn.call(new CheerioWrap([this._els[i]]), i, this._els[i]);
                }
                return this;
            };

            CheerioWrap.prototype.map = function(fn) {
                var result = [];
                for (var i = 0; i < this._els.length; i++) {
                    result.push(fn.call(new CheerioWrap([this._els[i]]), i, this._els[i]));
                }
                return result;
            };

            CheerioWrap.prototype.filter = function(fn) {
                var filtered = [];
                for (var i = 0; i < this._els.length; i++) {
                    if (fn.call(new CheerioWrap([this._els[i]]), i, this._els[i])) filtered.push(this._els[i]);
                }
                return new CheerioWrap(filtered);
            };

            CheerioWrap.prototype.eq = function(idx) {
                if (idx < 0) idx = this._els.length + idx;
                return new CheerioWrap(idx >= 0 && idx < this._els.length ? [this._els[idx]] : []);
            };

            CheerioWrap.prototype.first = function() { return this.eq(0); };
            CheerioWrap.prototype.last  = function() { return this.eq(this._els.length - 1); };

            CheerioWrap.prototype.parent = function() {
                if (this._els.length === 0) return new CheerioWrap([]);
                var p = this._els[0].parent();
                return p ? new CheerioWrap([p]) : new CheerioWrap([]);
            };

            CheerioWrap.prototype.children = function(selector) {
                var all = [];
                for (var i = 0; i < this._els.length; i++) {
                    var kids = this._els[i].children();
                    for (var j = 0; j < kids.size(); j++) all.push(kids.get(j));
                }
                var w = new CheerioWrap(all);
                return selector ? w.filter(function(i, el) {
                    try { return el.is(selector); } catch(e) { return false; }
                }) : w;
            };

            CheerioWrap.prototype.next = function() {
                if (this._els.length === 0) return new CheerioWrap([]);
                var n = this._els[0].nextElementSibling();
                return n ? new CheerioWrap([n]) : new CheerioWrap([]);
            };

            CheerioWrap.prototype.prev = function() {
                if (this._els.length === 0) return new CheerioWrap([]);
                var p = this._els[0].previousElementSibling();
                return p ? new CheerioWrap([p]) : new CheerioWrap([]);
            };

            CheerioWrap.prototype.get = function(i) { return this._els[i]; };
            CheerioWrap.prototype.toArray = function() { return this._els.slice(); };

            // Cheerio factory: $('selector', html_or_element)
            var load = function(html, opts, isDocument) {
                var jDoc = __jsoupParse(typeof html === 'string' ? html : '', '');
                var $ = function(selector) {
                    if (!selector) return new CheerioWrap([]);
                    if (typeof selector === 'string') {
                        try {
                            var els = jDoc.select(selector);
                            var arr = [];
                            for (var i = 0; i < els.size(); i++) arr.push(els.get(i));
                            return new CheerioWrap(arr);
                        } catch(e) {
                            return new CheerioWrap([]);
                        }
                    } else if (selector instanceof CheerioWrap) {
                        return selector;
                    } else if (Array.isArray(selector)) {
                        return new CheerioWrap(selector);
                    } else {
                        // Wrapping single Java elements/objects
                        return new CheerioWrap([selector]);
                    }
                };
                $.html  = function(wrap) { return wrap ? wrap.html() : jDoc.html(); };
                $.text  = function(wrap) { return wrap ? wrap.text() : jDoc.text(); };
                $.root  = function() { return new CheerioWrap([jDoc]); };
                return $;
            };

            return { load: load, default: { load: load } };
        })();
    """.trimIndent()

    // ── Result Parsing ─────────────────────────────────────────────────────────

    private fun parseJsResult(raw: Any?, providerName: String): List<StreamResult> {
        if (raw == null || raw == ScriptableObject.NOT_FOUND) return emptyList()
        return when (raw) {
            is NativeArray -> (0 until raw.length).mapNotNull { i ->
                val item = raw[i]
                if (item is NativeObject) parseStreamObject(item, providerName) else null
            }
            is NativeObject -> {
                // some providers wrap in { streams: [...] }
                val streams = raw["streams"]
                if (streams is NativeArray) parseJsResult(streams, providerName) else emptyList()
            }
            else -> emptyList()
        }
    }

    private fun parseStreamObject(obj: NativeObject, providerName: String): StreamResult? {
        val url = obj["url"]?.toString() ?: return null
        if (url.isBlank() || url == "null" || url == "undefined") return null

        val quality = obj["quality"]?.toString()
            ?: obj["name"]?.toString()?.let { extractQuality(it) }
            ?: "Auto"

        val headersObj = obj["headers"]
        val headers = mutableMapOf<String, String>()
        if (headersObj is NativeObject) {
            headersObj.ids.forEach { key ->
                headers[key.toString()] = headersObj[key.toString()]?.toString() ?: ""
            }
        }
        val name = obj["name"]?.toString() ?: providerName
        return StreamResult(url = url, quality = quality, headers = headers, providerName = name)
    }

    private fun extractQuality(name: String): String {
        val qualityMap = listOf("4K", "2160p", "1080p", "720p", "480p", "360p", "HDR", "HEVC", "BluRay", "WEB-DL")
        return qualityMap.firstOrNull { name.contains(it, ignoreCase = true) } ?: "Auto"
    }

    private fun kotlinStringToJs(str: String): String =
        "\"${str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
}

/**
 * Thin Kotlin wrapper around a Jsoup Document to expose it to Rhino via LiveConnect.
 * Rhino can directly call Java methods on objects wrapped via Context.javaToJS(),
 * so we expose select(), text(), etc. as plain Java methods.
 */
class JsoupDocument(private val doc: org.jsoup.nodes.Document) {
    fun select(css: String): org.jsoup.select.Elements = try { doc.select(css) } catch (e: Exception) { org.jsoup.select.Elements() }
    fun text(): String = doc.text()
    fun html(): String = doc.html()
    fun body(): org.jsoup.nodes.Element? = doc.body()
    fun title(): String = doc.title()
}
