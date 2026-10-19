package com.saveclip.app

import android.app.Application
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class App : Application() {

    private val _engine = MutableStateFlow<EngineState>(EngineState.Loading)
    val engine: StateFlow<EngineState> = _engine

    override fun onCreate() {
        super.onCreate()
        // Распаковка движка (Python + yt-dlp + ffmpeg) — при первом запуске занимает время
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            _engine.value = try {
                YoutubeDL.getInstance().init(this@App)
                FFmpeg.getInstance().init(this@App)
                EngineState.Ready
            } catch (e: Throwable) {
                EngineState.Failed(e.message ?: "неизвестная ошибка")
            }
        }
    }
}
