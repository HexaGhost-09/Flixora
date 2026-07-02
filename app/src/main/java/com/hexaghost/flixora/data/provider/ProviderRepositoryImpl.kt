package com.hexaghost.flixora.data.provider

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hexaghost.flixora.domain.model.InstalledProvider
import com.hexaghost.flixora.domain.model.ProviderInfo
import com.hexaghost.flixora.domain.model.Repository
import com.hexaghost.flixora.domain.model.StreamResult
import com.hexaghost.flixora.domain.repository.ProviderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val jsEngine: JsProviderEngine,
    private val okHttpClient: OkHttpClient
) : ProviderRepository {

    private val prefs = context.getSharedPreferences("flixora_providers", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val providersDir: File get() = File(context.filesDir, "providers").also { it.mkdirs() }

    // ── Repositories ─────────────────────────────────────────────────────────

    override fun getSavedRepositoryUrls(): List<String> {
        val json = prefs.getString("repo_urls", "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
    }

    override fun saveRepositoryUrl(url: String) {
        val current = getSavedRepositoryUrls().toMutableList()
        if (url !in current) {
            current.add(url)
            prefs.edit().putString("repo_urls", gson.toJson(current)).apply()
        }
    }

    override fun removeRepositoryUrl(url: String) {
        val current = getSavedRepositoryUrls().toMutableList()
        current.remove(url)
        prefs.edit().putString("repo_urls", gson.toJson(current)).apply()
    }

    // ── Manifest Fetch ────────────────────────────────────────────────────────

    override suspend fun fetchManifest(url: String): Result<Repository> = withContext(Dispatchers.IO) {
        runCatching {
            val manifestUrl = if (url.endsWith("manifest.json")) url else "$url/manifest.json"
            val request = Request.Builder().url(manifestUrl).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) error("HTTP ${response.code}: Failed to fetch manifest")
            val body = response.body?.string() ?: error("Empty manifest response")
            parseManifest(url, body)
        }
    }

    private fun parseManifest(repoUrl: String, json: String): Repository {
        val obj = JSONObject(json)
        val name = obj.optString("name", "Unknown Repo")
        val description = obj.optString("description", "")
        val providersArray = obj.optJSONArray("providers")
        val providers = mutableListOf<ProviderInfo>()
        if (providersArray != null) {
            for (i in 0 until providersArray.length()) {
                val p = providersArray.getJSONObject(i)
                providers.add(
                    ProviderInfo(
                        id = p.optString("id", "unknown-$i"),
                        name = p.optString("name", "Provider $i"),
                        version = p.optString("version", "1.0.0"),
                        filename = p.optString("filename", ""),
                        description = p.optString("description", ""),
                        repositoryUrl = repoUrl
                    )
                )
            }
        }
        return Repository(url = repoUrl, name = name, description = description, providers = providers)
    }

    // ── Provider Install / Uninstall ─────────────────────────────────────────

    override suspend fun installProvider(provider: ProviderInfo): Result<InstalledProvider> =
        withContext(Dispatchers.IO) {
            runCatching {
                val jsUrl = if (provider.filename.startsWith("http://") || provider.filename.startsWith("https://")) {
                    provider.filename
                } else {
                    val baseUrl = provider.repositoryUrl.trimEnd('/')
                    "$baseUrl/${provider.filename.trimStart('/')}"
                }
                val request = Request.Builder().url(jsUrl).build()
                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) error("HTTP ${response.code}: Failed to download provider JS")
                val jsCode = response.body?.string() ?: error("Empty JS response")

                val jsFile = File(providersDir, "${provider.id}.js")
                jsFile.writeText(jsCode)

                val installed = InstalledProvider(
                    id = provider.id,
                    name = provider.name,
                    version = provider.version,
                    jsFilePath = jsFile.absolutePath,
                    repositoryUrl = provider.repositoryUrl,
                    isEnabled = true
                )
                saveInstalledProvider(installed)
                installed
            }
        }

    override suspend fun uninstallProvider(id: String) {
        val file = File(providersDir, "$id.js")
        if (file.exists()) file.delete()
        removeInstalledProvider(id)
    }

    override fun toggleProvider(id: String, enabled: Boolean) {
        val list = getInstalledProviders().toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx != -1) {
            list[idx] = list[idx].copy(isEnabled = enabled)
            prefs.edit().putString("installed_providers", gson.toJson(list)).apply()
        }
    }

    // ── Installed Providers ───────────────────────────────────────────────────

    override fun getInstalledProviders(): List<InstalledProvider> {
        val json = prefs.getString("installed_providers", "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<InstalledProvider>>() {}.type) ?: emptyList()
    }

    private fun saveInstalledProvider(provider: InstalledProvider) {
        val list = getInstalledProviders().toMutableList()
        list.removeAll { it.id == provider.id }
        list.add(provider)
        prefs.edit().putString("installed_providers", gson.toJson(list)).apply()
    }

    private fun removeInstalledProvider(id: String) {
        val list = getInstalledProviders().toMutableList()
        list.removeAll { it.id == id }
        prefs.edit().putString("installed_providers", gson.toJson(list)).apply()
    }

    // ── Stream Resolution ────────────────────────────────────────────────────

    override suspend fun resolveStreams(
        provider: InstalledProvider,
        tmdbId: Int,
        title: String,
        mediaType: String,
        year: Int
    ): Result<List<StreamResult>> {
        val jsFile = File(provider.jsFilePath)
        if (!jsFile.exists()) return Result.failure(Exception("Provider JS file not found: ${jsFile.path}"))
        return jsEngine.resolveStreams(jsFile, tmdbId, title, mediaType, year, provider.name)
    }
}
