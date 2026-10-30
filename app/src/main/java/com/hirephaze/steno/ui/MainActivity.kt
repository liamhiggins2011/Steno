package com.hirephaze.steno.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import com.hirephaze.steno.data.Phrase
import com.hirephaze.steno.ui.theme.StenoTheme

class MainActivity : ComponentActivity() {

    private val vm: StenoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StenoTheme {
                val phrases by vm.phrases.collectAsState()
                var editing by remember { mutableStateOf<EditTarget?>(null) }

                val target = editing
                if (target == null) {
                    PhraseListScreen(
                        phrases = phrases,
                        onAdd = { editing = EditTarget() },
                        onEdit = { editing = EditTarget(it.id, it.shortcut, it.expansion) },
                        onDelete = { vm.delete(it) },
                    )
                } else {
                    PhraseEditorScreen(
                        target = target,
                        onCancel = { editing = null },
                        onSave = { shortcut, expansion ->
                            vm.save(shortcut, expansion, target.id)
                            editing = null
                        },
                    )
                }
            }
        }
    }
}

/** What the editor is working on; id == null means a new phrase. */
data class EditTarget(
    val id: Long? = null,
    val shortcut: String = "",
    val expansion: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhraseListScreen(
    phrases: List<Phrase>,
    onAdd: () -> Unit,
    onEdit: (Phrase) -> Unit,
    onDelete: (Phrase) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Steno") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add phrase")
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ServiceStatusCard()
            LazyColumn(Modifier.fillMaxSize()) {
                items(phrases, key = { it.id }) { phrase ->
                    PhraseRow(phrase, onClick = { onEdit(phrase) }, onDelete = { onDelete(phrase) })
                }
            }
        }
    }
}

@Composable
private fun PhraseRow(phrase: Phrase, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    phrase.shortcut,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    phrase.expansion.replace("\n", " · "),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun ServiceStatusCard() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(isServiceEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = isServiceEnabled(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (enabled) return
    Card(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Steno is off",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Turn on the Steno accessibility service so it can expand your shortcuts as you type. It runs on-device and never uses the internet.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }) {
                Text("Open accessibility settings")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhraseEditorScreen(
    target: EditTarget,
    onCancel: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var shortcut by remember { mutableStateOf(target.shortcut) }
    var expansion by remember { mutableStateOf(target.expansion) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (target.id == null) "New phrase" else "Edit phrase") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = shortcut,
                onValueChange = { shortcut = it },
                label = { Text("Shortcut") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = expansion,
                onValueChange = { expansion = it },
                label = { Text("Expands to") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            )
            Text(
                "Placeholders: {date}, {time}, {nl} for a new line, {cursor} for where the caret lands.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { onSave(shortcut, expansion) },
                enabled = shortcut.isNotBlank() && expansion.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}
