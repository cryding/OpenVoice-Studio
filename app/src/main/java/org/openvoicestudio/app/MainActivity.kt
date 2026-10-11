package org.openvoicestudio.app

import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.Locale

private val StudioInk = androidx.compose.ui.graphics.Color(0xFF10131B)
private val StudioPanel = androidx.compose.ui.graphics.Color(0xFF1C2230)
private val StudioAccent = androidx.compose.ui.graphics.Color(0xFFB9A7FF)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ready by mutableStateOf(false)
    private var message by mutableStateOf("Preparing your device voice…")
    private var pendingExportUri: Uri? = null
    private var pendingExportFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId != "openvoice-export") return
                val target = pendingExportUri
                val source = pendingExportFile
                runOnUiThread {
                    try {
                        if (target == null || source == null || !source.exists() || source.length() == 0L) {
                            message = "Export failed: no audio file was created."
                        } else {
                            contentResolver.openOutputStream(target)?.use { output ->
                                source.inputStream().use { input -> input.copyTo(output) }
                            } ?: error("Could not open the selected save location")
                            message = "WAV saved successfully. Open it from your selected location."
                        }
                    } catch (e: Exception) {
                        message = "Export failed: ${e.localizedMessage ?: "storage error"}"
                    } finally {
                        source?.delete()
                        pendingExportUri = null
                        pendingExportFile = null
                    }
                }
            }
            override fun onError(utteranceId: String?) {
                runOnUiThread {
                    message = "The installed voice engine could not create this audio file."
                    pendingExportFile?.delete()
                    pendingExportFile = null
                    pendingExportUri = null
                }
            }
        })
        setContent {
            val colors = darkColorScheme(
                primary = StudioAccent,
                onPrimary = StudioInk,
                background = StudioInk,
                surface = StudioPanel,
                onSurface = androidx.compose.ui.graphics.Color(0xFFF4F2FA),
                secondary = androidx.compose.ui.graphics.Color(0xFF8DDCC6)
            )
            MaterialTheme(colorScheme = colors) {
                var text by remember { mutableStateOf("Hello! This is a text-to-speech test.") }
                var rate by remember { mutableFloatStateOf(1f) }
                var language by remember { mutableStateOf("Device default") }
                val export = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("audio/wav")
                ) { uri ->
                    if (uri == null) {
                        message = "Export cancelled."
                    } else if (ready && text.isNotBlank()) {
                        val file = File(cacheDir, "openvoice-${System.currentTimeMillis()}.wav")
                        pendingExportUri = uri
                        pendingExportFile = file
                        tts?.setSpeechRate(rate)
                        val result = tts?.synthesizeToFile(text, Bundle(), file, "openvoice-export")
                        message = if (result == TextToSpeech.SUCCESS) "Creating WAV… keep the app open until it finishes."
                                  else "Export could not be queued. Try another installed voice."
                        if (result != TextToSpeech.SUCCESS) {
                            file.delete()
                            pendingExportUri = null
                            pendingExportFile = null
                        }
                    } else message = "Wait for the voice engine and enter some text first."
                }
                Scaffold(containerColor = StudioInk) { insets ->
                    Column(
                        Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("OPENVOICE", style = MaterialTheme.typography.labelLarge, color = StudioAccent)
                            Text("Studio", style = MaterialTheme.typography.headlineLarge)
                            Text("Write it. Hear it. Save it.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = StudioPanel), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("Your script", style = MaterialTheme.typography.titleLarge)
                                OutlinedTextField(
                                    value = text, onValueChange = { text = it },
                                    label = { Text("Text to speak") },
                                    placeholder = { Text("Type or paste your script…") },
                                    modifier = Modifier.fillMaxWidth(), minLines = 5,
                                    supportingText = { Text("${text.length} characters") }
                                )
                                Text("Speaking speed · ${"%.1f".format(Locale.US, rate)}×")
                                Slider(value = rate, onValueChange = { rate = it }, valueRange = 0.6f..1.5f)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            tts?.setSpeechRate(rate)
                                            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "preview")
                                            message = if (result == TextToSpeech.SUCCESS) "Playing preview with your installed device voice." else "Preview failed."
                                        },
                                        enabled = ready && text.isNotBlank(), modifier = Modifier.weight(1f)
                                    ) { Text("▶  Preview") }
                                    OutlinedButton(
                                        onClick = { export.launch("openvoice-output.wav") },
                                        enabled = ready && text.isNotBlank(), modifier = Modifier.weight(1f)
                                    ) { Text("↓  Export WAV") }
                                }
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = StudioPanel), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(if (ready) "●" else "◌", color = if (ready) MaterialTheme.colorScheme.secondary else StudioAccent)
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(if (ready) "Voice engine ready" else "Voice engine starting", style = MaterialTheme.typography.titleMedium)
                                    Text(message, style = MaterialTheme.typography.bodyMedium)
                                    Text("Voice: $language", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
                                }
                            }
                        }
                        Text("Offline note", style = MaterialTheme.typography.titleMedium)
                        Text("Uses Android’s installed text-to-speech engine. Offline use and language availability depend on downloaded device voice data. An independent neural voice model is not bundled yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
                    }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        val locale = Locale.getDefault()
        language = locale.getDisplayName(locale)
        tts?.language = locale
        message = if (ready) "Ready. Enter text to preview or export a WAV file." else "TTS initialization failed. Check Android text-to-speech settings."
    }

    private var language: String = "Device default"

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        pendingExportFile?.delete()
        super.onDestroy()
    }
}