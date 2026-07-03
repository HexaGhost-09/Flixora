package com.hexaghost.flixora.data.provider

import android.util.Log
import com.hexaghost.flixora.domain.model.StreamResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.io.File
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
 * KEY ARCHITECTURE NOTE:
 *   All Java→JS bridges MUST use BaseFunction subclasses.
 *   Context.javaToJS(kotlinLambda) creates a NativeJavaObject which is NOT callable
 *   from JavaScript — it throws "TypeError: X is not a function" which silently kills
 *   the entire scraper execution and returns empty results.
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

    private fun doFetch(url: String, options: NativeObject?): Map<String, Any?> {
        val method = options?.get("method")?.toString() ?: "GET"
        val headersMap = mutableMapOf<String, String>()
        val headersObj = options?.get("headers")
        if (headersObj is NativeObject) {
            headersObj.ids.forEach { key ->
                headersMap[key.toString()] = headersObj[key.toString()]?.toString() ?: ""
            }
        }
        val bodyStr = options?.get("body")?.toString()
        val builder = okhttp3.Request.Builder()
        return try {
            builder.url(url)
            headersMap.forEach { (k, v) -> builder.addHeader(k, v) }
            if (method.equals("POST", ignoreCase = true) || method.equals("PUT", ignoreCase = true)) {
                val ct = headersMap.entries.firstOrNull { it.key.equals("Content-Type", true) }?.value ?: "application/json"
                builder.method(method.uppercase(), (bodyStr ?: "").toRequestBody(ct.toMediaTypeOrNull()))
            } else {
                builder.method(method.uppercase(), null)
            }
            val response = okHttpClient.newCall(builder.build()).execute()
            val body = response.body?.string() ?: ""
            val respHeaders = mutableMapOf<String, String>()
            response.headers.names().forEach { n -> respHeaders[n] = response.header(n) ?: "" }
            Log.d(TAG, "fetch OK ${response.code} $url")
            mapOf("status" to response.code, "ok" to response.isSuccessful, "bodyText" to body, "headers" to respHeaders)
        } catch (e: Exception) {
            Log.e(TAG, "fetch error $url: ${e.message}")
            mapOf("status" to 500, "ok" to false, "bodyText" to (e.message ?: ""), "headers" to emptyMap<String, String>())
        }
    }

    private fun parseHtml(html: String, baseUri: String = ""): JsoupDocument =
        JsoupDocument(Jsoup.parse(html, baseUri))

    // ── Rhino BaseFunction helpers ─────────────────────────────────────────────
    // These create *real* JavaScript-callable functions (not NativeJavaObjects).
    // Context.javaToJS(lambda) produces NativeJavaObject which is NOT callable.

    private fun makeFetchFn(scope: Scriptable): BaseFunction = object : BaseFunction(scope, ScriptableObject.getFunctionPrototype(scope)) {
        override fun getFunctionName() = "__javaFetch"
        override fun getArity() = 2
        override fun call(cx: Context, callScope: Scriptable, thisObj: Scriptable?, args: Array<out Any?>): Any {
            val url = args.getOrNull(0)?.toString() ?: return jsUndefined()
            val opts = args.getOrNull(1) as? NativeObject
            val result = doFetch(url, opts)
            // Return the map as a NativeObject so JS can call result.get('key')
            val obj = NativeObject()
            obj.put("status", obj, result["status"])
            obj.put("ok", obj, result["ok"])
            obj.put("bodyText", obj, result["bodyText"])
            val headersNative = NativeObject()
            @Suppress("UNCHECKED_CAST")
            (result["headers"] as? Map<String, String>)?.forEach { (k, v) ->
                headersNative.put(k, headersNative, v)
            }
            obj.put("headers", obj, headersNative)
            return obj
        }
    }

    private fun makeJsoupFn(scope: Scriptable): BaseFunction = object : BaseFunction(scope, ScriptableObject.getFunctionPrototype(scope)) {
        override fun getFunctionName() = "__jsoupParse"
        override fun getArity() = 2
        override fun call(cx: Context, callScope: Scriptable, thisObj: Scriptable?, args: Array<out Any?>): Any {
            val html = args.getOrNull(0)?.toString() ?: ""
            val baseUri = args.getOrNull(1)?.toString() ?: ""
            return Context.javaToJS(parseHtml(html, baseUri), callScope)
        }
    }

    private fun makeLogFn(scope: Scriptable): BaseFunction = object : BaseFunction(scope, ScriptableObject.getFunctionPrototype(scope)) {
        override fun getFunctionName() = "__androidLog"
        override fun getArity() = 2
        override fun call(cx: Context, callScope: Scriptable, thisObj: Scriptable?, args: Array<out Any?>): Any {
            val tag = args.getOrNull(0)?.toString() ?: TAG
            val msg = args.getOrNull(1)?.toString() ?: ""
            Log.d(tag, msg)
            return jsUndefined()
        }
    }

    private fun jsUndefined(): Any = Context.getUndefinedValue()

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

            // ── Java bridges as real Rhino BaseFunction (CALLABLE from JS) ────
            ScriptableObject.putProperty(scope, "__javaFetch", makeFetchFn(scope))
            ScriptableObject.putProperty(scope, "__jsoupParse", makeJsoupFn(scope))
            ScriptableObject.putProperty(scope, "__logTag", TAG)
            ScriptableObject.putProperty(scope, "__androidLog", makeLogFn(scope))

            // ── Polyfills ────────────────────────────────────────────────────
            rhino.evaluateString(scope, buildPolyfills(), "polyfills.js", 1, null)

            // ── CommonJS shim ─────────────────────────────────────────────────
            rhino.evaluateString(scope, buildCommonJsShim(), "commonjs.js", 1, null)

            // ── Cheerio shim (cheerio-without-node-native) ────────────────────
            rhino.evaluateString(scope, buildCheerioShim(), "cheerio.js", 1, null)

            // ── Execute provider code ─────────────────────────────────────────
            rhino.evaluateString(scope, jsCode, "provider.js", 1, null)

            // ── Capture module.exports after provider executes ────────────────
            rhino.evaluateString(scope, "__moduleExports = module.exports;", "capture.js", 1, null)

            // ── Build the info / invoke getStreams ─────────────────────────────
            val nuvioType = if (mediaType.equals("tv", ignoreCase = true)) "tv" else "movie"
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
                        __callError = e ? (e.message || e.toString()) : 'Unknown error';
                    }
                })();
            """.trimIndent(), "call.js", 1, null)

            // Drain microtask queue — providers chain many async fetch calls
            repeat(50) { rhino.processMicrotasks() }

            val errorRaw = scope.get("__callError", scope)
            if (errorRaw != null && errorRaw != ScriptableObject.NOT_FOUND && errorRaw.toString() != "null") {
                Log.e(TAG, "[$providerName] JS error: $errorRaw")
            }

            val resultRaw = scope.get("__callResult", scope)
            Log.d(TAG, "[$providerName] result type=${resultRaw?.javaClass?.simpleName}")
            parseJsResult(resultRaw, providerName)
        } catch (e: Exception) {
            Log.e(TAG, "JS execution failed for '$providerName': ${e.message}", e)
            emptyList()
        } finally {
            Context.exit()
        }
    }

    // ── Polyfills ──────────────────────────────────────────────────────────────

    private fun buildPolyfills(): String = """
        // ── Console ──────────────────────────────────────────────────────────────
        var console = {
            log:   function() { var a = Array.prototype.slice.call(arguments).join(' '); __androidLog(__logTag, a); },
            error: function() { var a = Array.prototype.slice.call(arguments).join(' '); __androidLog(__logTag, 'ERROR: ' + a); },
            warn:  function() { var a = Array.prototype.slice.call(arguments).join(' '); __androidLog(__logTag, 'WARN: '  + a); },
            info:  function() { var a = Array.prototype.slice.call(arguments).join(' '); __androidLog(__logTag, 'INFO: '  + a); },
            debug: function() { var a = Array.prototype.slice.call(arguments).join(' '); __androidLog(__logTag, 'DEBUG: ' + a); }
        };

        // ── Timers — synchronous stubs ────────────────────────────────────────────
        var setTimeout  = function(fn, delay) { return 0; };
        var clearTimeout  = function(id) {};
        var setInterval   = function(fn, delay) { return 0; };
        var clearInterval = function(id) {};

        // ── Base64 ────────────────────────────────────────────────────────────────
        var atob = function(b64) {
            var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
            var str = String(b64).replace(/[=]+${'$'}/, '');
            var output = '';
            for (var bc = 0, bs, buffer, i = 0;
                 buffer = str.charAt(i++);
                 ~buffer && (bs = bc % 4 ? bs * 64 + buffer : buffer, bc++ % 4)
                     ? output += String.fromCharCode(255 & bs >> (-2 * bc & 6)) : 0) {
                buffer = chars.indexOf(buffer);
            }
            return output;
        };
        var btoa = function(str) {
            var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
            var out = '', i = 0;
            while (i < str.length) {
                var c1 = str.charCodeAt(i++), c2 = str.charCodeAt(i++), c3 = str.charCodeAt(i++);
                out += chars[c1>>2] + chars[((c1&3)<<4)|(c2>>4)] +
                       (isNaN(c2)?'=':chars[((c2&15)<<2)|(c3>>6)]) +
                       (isNaN(c2)||isNaN(c3)?'=':chars[c3&63]);
            }
            return out;
        };

        // ── Buffer ────────────────────────────────────────────────────────────────
        var Buffer = {
            from: function(data, encoding) {
                if (encoding === 'base64') return { toString: function(e) { return atob(data); } };
                return { toString: function(e) {
                    if (e === 'base64') return btoa(typeof data === 'string' ? data : String.fromCharCode.apply(null, data));
                    return typeof data === 'string' ? data : '';
                }};
            },
            alloc: function(size) { return { toString: function() { return ''; } }; }
        };

        // ── process ───────────────────────────────────────────────────────────────
        var process = { env: {}, nextTick: function(fn) { fn(); }, version: 'v16.0.0', platform: 'android' };

        // ── URL ───────────────────────────────────────────────────────────────────
        function URL(url, base) {
            var full = url;
            if (base && !url.match(/^https?:\/\//)) {
                full = base.replace(/\/[^\/]*${'$'}/, '/') + url.replace(/^\//, '');
            }
            var m = /^(https?:)?\/\/([^/?#]*)([^?#]*)(\?[^#]*)?(#.*)?${'$'}/.exec(full) || [];
            this.href     = full;
            this.protocol = m[1] || 'https:';
            this.host     = m[2] || '';
            this.hostname = (m[2] || '').split(':')[0];
            this.port     = ((m[2] || '').split(':')[1]) || '';
            this.pathname = m[3] || '/';
            this.search   = m[4] || '';
            this.hash     = m[5] || '';
            this.origin   = this.protocol + '//' + this.host;
            this.toString = function() { return this.href; };
        }

        // ── URLSearchParams ───────────────────────────────────────────────────────
        function URLSearchParams(init) {
            this._p = {};
            if (typeof init === 'string') {
                init.replace(/^\?/, '').split('&').forEach(function(pair) {
                    var kv = pair.split('=');
                    if (kv[0]) this._p[decodeURIComponent(kv[0])] = decodeURIComponent(kv[1] || '');
                }, this);
            }
            this.get    = function(k) { return this._p[k] !== undefined ? this._p[k] : null; };
            this.set    = function(k, v) { this._p[k] = String(v); };
            this.append = function(k, v) { this._p[k] = String(v); };
            this.toString = function() {
                return Object.keys(this._p).map(function(k) {
                    return encodeURIComponent(k) + '=' + encodeURIComponent(this._p[k]);
                }, this).join('&');
            };
        }

        // ── global reference ──────────────────────────────────────────────────────
        var global = this;

        // ── fetch ─────────────────────────────────────────────────────────────────
        // Wraps the synchronous __javaFetch BaseFunction in a Promise so providers
        // can use the standard async fetch() API with await/yield.
        var fetch = function(url, options) {
            return new Promise(function(resolve, reject) {
                try {
                    var result = __javaFetch(url, options || null);
                    var bodyText = result.bodyText || '';
                    var status   = result.status   || 0;
                    var ok       = result.ok       || false;
                    var hdrs     = result.headers  || {};
                    var response = {
                        status: status,
                        ok: ok,
                        url: url,
                        redirected: false,
                        headers: {
                            get: function(name) {
                                if (!hdrs) return null;
                                var v = hdrs[name];
                                if (v !== undefined && v !== null) return v;
                                for (var k in hdrs) {
                                    if (k.toLowerCase() === (name || '').toLowerCase()) return hdrs[k];
                                }
                                return null;
                            },
                            has: function(name) { return this.get(name) !== null; }
                        },
                        text: function() { return Promise.resolve(bodyText); },
                        json: function() {
                            return new Promise(function(res, rej) {
                                try { res(JSON.parse(bodyText)); } catch(e) { rej(e); }
                            });
                        },
                        arrayBuffer: function() { return Promise.resolve(bodyText); },
                        clone: function() { return response; }
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
        var __registry = {};
        var __moduleExports = null;
        var module = { exports: {} };
        var exports = module.exports;

        var __cryptoStub = {
            AES: {
                encrypt: function(s, k) { return { toString: function() { return btoa(typeof s === 'string' ? s : String(s)); } }; },
                decrypt: function(s, k) { return { toString: function(e) { try { return atob(s.toString()); } catch(_) { return ''; } } }; }
            },
            enc: { Utf8: 'utf8', Base64: 'base64' },
            MD5: function(s) { return { toString: function() { return s; } }; },
            SHA256: function(s) { return { toString: function() { return s; } }; },
            HmacSHA256: function(msg, key) { return { toString: function() { return ''; } }; }
        };

        var __axiosStub = {
            get:    function(url, cfg)      { return fetch(url, { method: 'GET',  headers: (cfg && cfg.headers) || {} }); },
            post:   function(url, data, cfg){ return fetch(url, { method: 'POST', headers: (cfg && cfg.headers) || {}, body: JSON.stringify(data) }); },
            create: function(d)             { return __axiosStub; },
            defaults: { headers: { common: {} } }
        };

        var require = function(moduleName) {
            if (__registry[moduleName]) return __registry[moduleName];
            if (moduleName === 'cheerio-without-node-native' ||
                moduleName === 'react-native-cheerio' ||
                moduleName === 'cheerio') {
                return __cheerio;
            }
            if (moduleName === 'crypto-js' || moduleName === 'crypto') return __cryptoStub;
            if (moduleName === 'axios') return __axiosStub;
            console.warn('require: unknown module "' + moduleName + '", returning {}');
            return {};
        };
    """.trimIndent()

    // ── Cheerio Shim backed by Jsoup ──────────────────────────────────────────

    private fun buildCheerioShim(): String = """
        var __cheerio = (function() {
            function CheerioWrap(jEls) {
                this._els = jEls || [];
                this.length = this._els.length;
            }

            CheerioWrap.prototype.find = function(sel) {
                var all = [];
                for (var i = 0; i < this._els.length; i++) {
                    try { var f = this._els[i].select(sel); for (var j = 0; j < f.size(); j++) all.push(f.get(j)); } catch(e) {}
                }
                return new CheerioWrap(all);
            };

            CheerioWrap.prototype.text = function() {
                var out = '';
                for (var i = 0; i < this._els.length; i++) { try { out += (i > 0 ? ' ' : '') + this._els[i].text(); } catch(e) {} }
                return out;
            };

            CheerioWrap.prototype.html = function() {
                if (!this._els.length) return '';
                try { return this._els[0].html(); } catch(e) { return ''; }
            };

            CheerioWrap.prototype.attr = function(name) {
                if (!this._els.length) return undefined;
                try {
                    if (!this._els[0].hasAttr(name)) return undefined;
                    return this._els[0].attr(name);
                } catch(e) { return undefined; }
            };

            CheerioWrap.prototype.each = function(fn) {
                for (var i = 0; i < this._els.length; i++) {
                    fn.call(new CheerioWrap([this._els[i]]), i, this._els[i]);
                }
                return this;
            };

            CheerioWrap.prototype.map = function(fn) {
                var out = [];
                for (var i = 0; i < this._els.length; i++) out.push(fn.call(new CheerioWrap([this._els[i]]), i, this._els[i]));
                return out;
            };

            CheerioWrap.prototype.filter = function(fn) {
                var out = [];
                for (var i = 0; i < this._els.length; i++) {
                    if (fn.call(new CheerioWrap([this._els[i]]), i, this._els[i])) out.push(this._els[i]);
                }
                return new CheerioWrap(out);
            };

            CheerioWrap.prototype.eq = function(idx) {
                if (idx < 0) idx = this._els.length + idx;
                return new CheerioWrap(idx >= 0 && idx < this._els.length ? [this._els[idx]] : []);
            };

            CheerioWrap.prototype.first = function() { return this.eq(0); };
            CheerioWrap.prototype.last  = function() { return this.eq(this._els.length - 1); };

            CheerioWrap.prototype.parent = function() {
                if (!this._els.length) return new CheerioWrap([]);
                try { var p = this._els[0].parent(); return p ? new CheerioWrap([p]) : new CheerioWrap([]); } catch(e) { return new CheerioWrap([]); }
            };

            CheerioWrap.prototype.children = function(sel) {
                var all = [];
                for (var i = 0; i < this._els.length; i++) {
                    try { var k = this._els[i].children(); for (var j = 0; j < k.size(); j++) all.push(k.get(j)); } catch(e) {}
                }
                var w = new CheerioWrap(all);
                if (!sel) return w;
                return w.filter(function(i, el) { try { return el.is(sel); } catch(e) { return false; } });
            };

            CheerioWrap.prototype.next = function() {
                if (!this._els.length) return new CheerioWrap([]);
                try { var n = this._els[0].nextElementSibling(); return n ? new CheerioWrap([n]) : new CheerioWrap([]); } catch(e) { return new CheerioWrap([]); }
            };

            CheerioWrap.prototype.prev = function() {
                if (!this._els.length) return new CheerioWrap([]);
                try { var p = this._els[0].previousElementSibling(); return p ? new CheerioWrap([p]) : new CheerioWrap([]); } catch(e) { return new CheerioWrap([]); }
            };

            CheerioWrap.prototype.get = function(i) { return this._els[i]; };
            CheerioWrap.prototype.toArray = function() { return this._els.slice(); };
            CheerioWrap.prototype.val = function() { return this.attr('value') || ''; };
            CheerioWrap.prototype.data = function(k) { return this.attr('data-' + k); };
            CheerioWrap.prototype.is = function(sel) {
                if (!this._els.length) return false;
                try { return this._els[0].is(sel); } catch(e) { return false; }
            };
            CheerioWrap.prototype.add = function(other) {
                var more = other instanceof CheerioWrap ? other._els : [other];
                return new CheerioWrap(this._els.concat(more));
            };

            var load = function(html, opts, isDoc) {
                var jDoc = __jsoupParse(typeof html === 'string' ? html : '', '');
                var ${'$'} = function(selector) {
                    if (!selector) return new CheerioWrap([]);
                    if (typeof selector === 'string') {
                        try {
                            var els = jDoc.select(selector);
                            var arr = [];
                            for (var i = 0; i < els.size(); i++) arr.push(els.get(i));
                            return new CheerioWrap(arr);
                        } catch(e) { return new CheerioWrap([]); }
                    } else if (selector instanceof CheerioWrap) {
                        return selector;
                    } else if (Array.isArray(selector)) {
                        return new CheerioWrap(selector);
                    } else {
                        return new CheerioWrap([selector]);
                    }
                };
                ${'$'}.html  = function(w) { return w ? w.html() : jDoc.html(); };
                ${'$'}.text  = function(w) { return w ? w.text() : jDoc.text(); };
                ${'$'}.root  = function() { return new CheerioWrap([jDoc]); };
                ${'$'}.load  = load;
                return ${'$'};
            };

            return { load: load, default: { load: load } };
        })();
    """.trimIndent()

    // ── Result Parsing ─────────────────────────────────────────────────────────

    private fun parseJsResult(raw: Any?, providerName: String): List<StreamResult> {
        if (raw == null || raw == ScriptableObject.NOT_FOUND) return emptyList()
        return when (raw) {
            is NativeArray -> (0 until raw.length).mapNotNull { i ->
                val item = raw[i.toInt()]
                if (item is NativeObject) parseStreamObject(item, providerName) else null
            }
            is NativeObject -> {
                val streams = raw["streams"]
                if (streams is NativeArray) parseJsResult(streams, providerName)
                else emptyList()
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
        val name = obj["name"]?.toString()?.takeIf { it.isNotBlank() } ?: providerName
        return StreamResult(url = url, quality = quality, headers = headers, providerName = name)
    }

    private fun extractQuality(name: String): String {
        val q = listOf("4K", "2160p", "1080p", "720p", "480p", "360p", "HDR", "HEVC", "BluRay", "WEB-DL")
        return q.firstOrNull { name.contains(it, ignoreCase = true) } ?: "Auto"
    }

    private fun kotlinStringToJs(str: String): String =
        "\"${str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
}

/**
 * Thin Kotlin wrapper around a Jsoup Document to expose it to Rhino via LiveConnect.
 * Methods are called by name from the cheerio JS shim.
 */
class JsoupDocument(private val doc: org.jsoup.nodes.Document) {
    fun select(css: String): org.jsoup.select.Elements = try { doc.select(css) } catch (e: Exception) { org.jsoup.select.Elements() }
    fun text(): String = doc.text()
    fun html(): String = doc.html()
    fun body(): org.jsoup.nodes.Element? = doc.body()
    fun title(): String = doc.title()
}
