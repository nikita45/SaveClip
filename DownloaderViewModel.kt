package com.saveclip.app

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DownloaderViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App
    val engine: StateFlow<EngineState> = app.engine

    private val prefs = application.getSharedPreferences("saveclip", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(UiState(history = loadHistory()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    @Volatile private var cancelRequested = false

    // ---------- Ввод ----------
    fun onUrlChange(value: String) = _state.update { it.copy(url = value) }
    fun onModeChange(mode: Mode) = _state.update { it.copy(mode = mode) }
    fun onQualityChange(q: Quality) = _state.update { it.copy(quality = q) }

    fun onPaste(text: String) {
        onUrlChange(extractUrl(text) ?: text.trim())
    }

    fun onSharedText(text: String) {
        extractUrl(text)?.let { url -> _state.update { it.copy(url = url) } }
    }

    // ---------- Загрузка ----------
    fun download() {
        val s = _state.value
        if (s.phase != Phase.IDLE) return
        if (engine.value !is EngineState.Ready) {
            toast("Приложение ещё готовится, подождите несколько секунд")
            return
        }
        val url = extractUrl(s.url) ?: run {
            toast("Вставьте корректную ссылку")
            return
        }

        cancelRequested = false
        _state.update { it.copy(phase = Phase.PREPARING, progress = 0f, eta = 0) }

        viewModelScope.launch(Dispatchers.IO) {
            val workDir = File(app.cacheDir, "downloads").apply {
                deleteRecursively()
                mkdirs()
            }
            try {
                val request = buildRequest(url, s.mode, s.quality, workDir)

                YoutubeDL.getInstance().execute(request, PROCESS_ID) { progress, eta, _ ->
                    _state.update {
                        it.copy(
                            phase = if (progress > 0f) Phase.DOWNLOADING else it.phase,
                            progress = (progress / 100f).coerceIn(0f, 1f),
                            eta = eta,
                        )
                    }
                }

                _state.update { it.copy(phase = Phase.SAVING, progress = 1f) }

                val file = workDir.listFiles().orEmpty()
                    .filter { it.isFile && it.extension !in setOf("part", "ytdl", "temp") }
                    .maxByOrNull { it.length() }
                    ?: error("Файл не найден после загрузки")

                val uri = saveToDownloads(file)
                val title = file.nameWithoutExtension
                    .substringBeforeLast(" [")
                    .ifBlank { file.nameWithoutExtension }

                val item = HistoryItem(title, uri.toString(), s.mode == Mode.AUDIO, System.currentTimeMillis())
                val history = (listOf(item) + _state.value.history).take(50)
                saveHistory(history)
                _state.update { it.copy(history = history, url = "") }
                toast("Готово! Файл в папке «Загрузки/SaveClip»")
            } catch (e: Throwable) {
                toast(if (cancelRequested) "Загрузка отменена" else friendlyError(e))
            } finally {
                workDir.deleteRecursively()
                _state.update { it.copy(phase = Phase.IDLE, progress = 0f, eta = 0) }
            }
        }
    }

    fun cancel() {
        cancelRequested = true
        runCatching { YoutubeDL.getInstance().destroyProcessById(PROCESS_ID) }
    }

    /** Сайты часто меняются — обновление yt-dlp чинит большинство ошибок. */
    fun updateEngine() {
        if (engine.value !is EngineState.Ready) return
        toast("Обновляю движок…")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val status = YoutubeDL.getInstance().updateYoutubeDL(app, YoutubeDL.UpdateChannel._STABLE)
                toast(
                    if (status == YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE) "У вас уже последняя версия"
                    else "Движок обновлён"
                )
            } catch (e: Throwable) {
                toast("Не удалось обновить: проверьте интернет")
            }
        }
    }

    fun removeFromHistory(item: HistoryItem) {
        val history = _state.value.history - item
        saveHistory(history)
        _state.update { it.copy(history = history) }
    }

    // ---------- Внутреннее ----------
    private fun buildRequest(url: String, mode: Mode, quality: Quality, dir: File) =
        YoutubeDLRequest(url).apply {
            addOption("--no-playlist")
            addOption("--no-mtime")
            addOption("-o", "${dir.absolutePath}/%(title).80B [%(id)s].%(ext)s")
            if (mode == Mode.AUDIO) {
                addOption("-f", "bestaudio/best")
                addOption("-x")
                addOption("--audio-format", "mp3")
                addOption("--audio-quality", "0")
            } else {
                addOption("-f", quality.format)
                addOption("--merge-output-format", "mp4")
            }
        }

    /** Копирует файл в общую папку «Загрузки/SaveClip», чтобы он был виден в галерее и плеерах. */
    private fun saveToDownloads(file: File): Uri {
        val resolver = app.contentResolver
        val mime = when (file.extension.lowercase()) {
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "opus", "ogg" -> "audio/ogg"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "application/octet-stream"
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SaveClip")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Не удалось создать файл в «Загрузках»")

        resolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        } ?: error("Не удалось записать файл")

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    private fun friendlyError(e: Throwable): String {
        val m = (e.message ?: "").lowercase()
        return when {
            "login" in m || "cookies" in m || "rate-limit" in m || "sign in" in m ->
                "Сайт требует вход в аккаунт или временно ограничил доступ. Попробуйте позже."
            "private" in m -> "Это приватное видео — скачать его нельзя."
            "unsupported url" in m -> "Эта ссылка не поддерживается."
            "requested format" in m -> "Такое качество недоступно. Выберите «Лучшее»."
            "unable to download webpage" in m || "resolve host" in m || "timed out" in m ->
                "Проблема с интернетом. Проверьте подключение."
            else -> "Не получилось скачать. Попробуйте ⋮ → «Обновить движок»."
        }
    }

    private fun toast(msg: String) {
        _events.tryEmit(msg)
    }

    private fun loadHistory(): List<HistoryItem> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            HistoryItem(o.getString("t"), o.getString("u"), o.getBoolean("a"), o.getLong("d"))
        }
    }.getOrDefault(emptyList())

    private fun saveHistory(items: List<HistoryItem>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("t", it.title).put("u", it.uri).put("a", it.audio).put("d", it.time))
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    private companion object {
        const val PROCESS_ID = "saveclip-download"
        const val KEY_HISTORY = "history"
    }
}
