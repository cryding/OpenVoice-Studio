package org.openvoicestudio.app

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ready by mutableStateOf(false)
    private var message by mutableStateOf("Starting installed Android TTS…")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent {
            MaterialTheme {
                var text by remember { mutableStateOf("Hello! This is a text-to-speech test.") }
                var rate by remember { mutableFloatStateOf(1f) }
                val export = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("audio/wav")
                ) { uri ->
                    if (uri != null && ready) {
                        val result = tts?.synthesizeToFile(text, null, cacheDir.resolve("tts-export.wav"), "export")
                        message = if (result == TextToSpeech.SUCCESS)
                            "Synthesis started. For reliable WAV export, wait for completion and copy the generated file; this starter export flow needs asynchronous completion handling."
                        else "This TTS engine could not start file synthesis."
                    }
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("OpenVoice Studio", style = MaterialTheme.typography.headlineMedium)
                    Text("Offline-first TTS starter · no account · no API key")
                    OutlinedTextField(text, { text = it }, label = { Text("Text to speak") }, modifier = Modifier.fillMaxWidth(), minLines = 5)
                    Text("Speaking rate: ${"%.1f".format(rate)}×")
                    Slider(rate, { rate = it }, valueRange = 0.6f..1.5f)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            tts?.setSpeechRate(rate)
                            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "preview")
                            message = "Speaking with installed Android TTS."
                        }, enabled = ready && text.isNotBlank()) { Text("Preview") }
                        OutlinedButton(onClick = { export.launch("openvoice-output.wav") }, enabled = ready && text.isNotBlank()) { Text("Export WAV") }
                    }
                    Text(message)
                    HorizontalDivider()
                    Text("Model status", style = MaterialTheme.typography.titleMedium)
                    Text("No open-source neural model is bundled in this starter. Offline behaviour and available languages depend on your installed TTS engine. Model integration and licence validation remain future work.")
                }
            }
        }
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        message = if (ready) "TTS ready. Voice/language availability depends on installed voice data." else "TTS initialization failed."
        tts?.language = Locale.getDefault()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}