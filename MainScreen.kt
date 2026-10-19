package com.saveclip.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saveclip.app.DownloaderViewModel
import com.saveclip.app.EngineState
import com.saveclip.app.HistoryItem
import com.saveclip.app.Mode
import com.saveclip.app.Phase
import com.saveclip.app.Platform
import com.saveclip.app.Quality
import com.saveclip.app.UiState
import com.saveclip.app.detectPlatform
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(24.dp)

@Composable
fun MainScreen(vm: DownloaderViewModel, onOpen: (HistoryItem) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val engine by vm.engine.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.events.collect { msg ->
            launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(msg)
            }
        }
    }

    Scaffold(
        containerColor = Palette.Bg,
        snackbarHost = {
            SnackbarHost(snackbar) {
                Snackbar(
                    snackbarData = it,
                    containerColor = Palette.SurfaceHigh,
                    contentColor = Palette.Text,
                    shape = RoundedCornerShape(14.dp),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Header(onUpdate = vm::updateEngine) }

            if (engine !is EngineState.Ready) {
                item { EngineBanner(engine) }
            }

            item { InputCard(state = state, vm = vm) }

            item {
                AnimatedVisibility(
                    visible = state.phase != Phase.IDLE,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    ProgressCard(state)
                }
            }

            item { SectionTitle("Мои загрузки", state.history.size) }

            if (state.history.isEmpty()) {
                item { EmptyHistory() }
            } else {
                items(state.history, key = { it.time }) { item ->
                    HistoryRow(item = item, onOpen = onOpen, onRemove = vm::removeFromHistory)
                }
            }
        }
    }
}

// ---------- Шапка ----------
@Composable
private fun Header(onUpdate: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AccentBrush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("SaveClip", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Palette.Text)
            Text("Видео по ссылке за пару секунд", fontSize = 13.sp, color = Palette.Muted)
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Меню", tint = Palette.Muted)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = Palette.SurfaceHigh,
            ) {
                DropdownMenuItem(
                    text = { Text("Обновить движок", color = Palette.Text) },
                    leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Palette.Muted) },
                    onClick = {
                        menuOpen = false
                        onUpdate()
                    },
                )
            }
        }
    }
}

@Composable
private fun EngineBanner(engine: EngineState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.SurfaceHigh)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (engine) {
            is EngineState.Failed -> {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Palette.Error)
                Spacer(Modifier.width(12.dp))
                Text("Не удалось запустить загрузчик: ${engine.message}", color = Palette.Text, fontSize = 13.sp)
            }
            else -> {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Palette.Accent1)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Подготовка… Первый запуск может занять до минуты",
                    color = Palette.Muted,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

// ---------- Карточка ввода ----------
@Composable
private fun InputCard(state: UiState, vm: DownloaderViewModel) {
    val clipboard = LocalClipboardManager.current
    val busy = state.phase != Phase.IDLE
    val platform = detectPlatform(state.url)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Palette.Surface)
            .border(1.dp, Palette.Stroke, CardShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.url,
            onValueChange = vm::onUrlChange,
            enabled = !busy,
            singleLine = true,
            placeholder = { Text("Вставьте ссылку на видео") },
            leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
            trailingIcon = {
                if (state.url.isEmpty()) {
                    IconButton(onClick = { clipboard.getText()?.text?.let(vm::onPaste) }) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = "Вставить")
                    }
                } else if (!busy) {
                    IconButton(onClick = { vm.onUrlChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Очистить")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Palette.Text,
                unfocusedTextColor = Palette.Text,
                disabledTextColor = Palette.Muted,
                focusedContainerColor = Palette.SurfaceHigh,
                unfocusedContainerColor = Palette.SurfaceHigh,
                disabledContainerColor = Palette.SurfaceHigh,
                focusedBorderColor = Palette.Accent1,
                unfocusedBorderColor = Palette.Stroke,
                disabledBorderColor = Palette.Stroke,
                cursorColor = Palette.Accent1,
                focusedLeadingIconColor = Palette.Accent1,
                unfocusedLeadingIconColor = Palette.Muted,
                focusedTrailingIconColor = Palette.Text,
                unfocusedTrailingIconColor = Palette.Muted,
                focusedPlaceholderColor = Palette.Muted,
                unfocusedPlaceholderColor = Palette.Muted,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { vm.download() }),
            modifier = Modifier.fillMaxWidth(),
        )

        // Подсветка распознанной платформы
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Platform.entries.forEach { p ->
                PlatformPill(p, active = p == platform, modifier = Modifier.weight(1f))
            }
        }

        // Видео / Аудио
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.SurfaceHigh)
                .padding(4.dp),
        ) {
            SegmentItem("Видео", Icons.Rounded.Movie, state.mode == Mode.VIDEO, !busy) { vm.onModeChange(Mode.VIDEO) }
            SegmentItem("Аудио MP3", Icons.Rounded.MusicNote, state.mode == Mode.AUDIO, !busy) { vm.onModeChange(Mode.AUDIO) }
        }

        // Качество
        AnimatedVisibility(visible = state.mode == Mode.VIDEO) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Quality.entries.forEach { q ->
                    QualityChip(
                        label = q.label,
                        selected = q == state.quality,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) { vm.onQualityChange(q) }
                }
            }
        }

        MainButton(busy = busy, onClick = { if (busy) vm.cancel() else vm.download() })
    }
}

@Composable
private fun PlatformPill(p: Platform, active: Boolean, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (active) p.color.copy(alpha = 0.16f) else Palette.SurfaceHigh, label = "pillBg")
    val fg by animateColorAsState(if (active) p.color else Palette.Muted, label = "pillFg")
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(fg),
        )
        Spacer(Modifier.width(6.dp))
        Text(p.label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun RowScope.SegmentItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (selected) Color.White else Palette.Muted
    Row(
        modifier = Modifier
            .weight(1f)
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AccentBrush else SolidColor(Color.Transparent))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = tint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QualityChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(if (selected) Palette.Accent1 else Palette.Stroke, label = "chipBorder")
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selected) Palette.Accent1.copy(alpha = 0.15f) else Palette.SurfaceHigh)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Palette.Text else Palette.Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MainButton(busy: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (busy) SolidColor(Palette.SurfaceHigh) else AccentBrush)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (busy) Icons.Rounded.Close else Icons.Rounded.Download,
            contentDescription = null,
            tint = Color.White,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (busy) "Отменить" else "Скачать",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ---------- Прогресс ----------
@Composable
private fun ProgressCard(state: UiState) {
    val animated by animateFloatAsState(state.progress, label = "progress")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Palette.Surface)
            .border(1.dp, Palette.Stroke, CardShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (state.phase) {
                    Phase.PREPARING -> "Получаю информацию о видео…"
                    Phase.DOWNLOADING -> "Скачиваю"
                    Phase.SAVING -> "Сохраняю в «Загрузки»…"
                    Phase.IDLE -> ""
                },
                color = Palette.Text,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (state.phase == Phase.DOWNLOADING) {
                Text(
                    "${(state.progress * 100).roundToInt()}%",
                    color = Palette.Accent2,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
        }

        if (state.phase == Phase.DOWNLOADING) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Palette.SurfaceHigh),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animated.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(AccentBrush),
                )
            }
            if (state.eta > 0) {
                Text("Осталось примерно ${formatEta(state.eta)}", color = Palette.Muted, fontSize = 12.sp)
            }
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = Palette.Accent1,
                trackColor = Palette.SurfaceHigh,
            )
        }
    }
}

// ---------- История ----------
@Composable
private fun SectionTitle(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Text(title, color = Palette.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (count > 0) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(Palette.SurfaceHigh)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text("$count", color = Palette.Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, Palette.Stroke, CardShape)
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Palette.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null, tint = Palette.Muted)
        }
        Spacer(Modifier.height(12.dp))
        Text("Здесь появятся скачанные видео", color = Palette.Text, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Совет: в YouTube, TikTok или Instagram нажмите «Поделиться» → SaveClip",
            color = Palette.Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, onOpen: (HistoryItem) -> Unit, onRemove: (HistoryItem) -> Unit) {
    val accent = if (item.audio) Palette.Accent2 else Palette.Accent1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Surface)
            .clickable { onOpen(item) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (item.audio) Icons.Rounded.MusicNote else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = accent,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = Palette.Text,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${if (item.audio) "MP3" else "MP4"} · ${formatTime(item.time)}",
                color = Palette.Muted,
                fontSize = 12.sp,
            )
        }
        IconButton(onClick = { onRemove(item) }) {
            Icon(Icons.Rounded.Close, contentDescription = "Убрать из списка", tint = Palette.Muted)
        }
    }
}

private fun formatEta(seconds: Long): String =
    if (seconds < 60) "$seconds сек" else "${seconds / 60} мин ${seconds % 60} сек"

private val timeFormat = SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag("ru"))
private fun formatTime(time: Long): String = timeFormat.format(Date(time))
