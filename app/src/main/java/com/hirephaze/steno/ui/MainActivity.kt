package com.hirephaze.steno.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import com.hirephaze.steno.data.Phrase
import com.hirephaze.steno.ui.theme.StenoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val vm: StenoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StenoTheme {
                val phrases by vm.phrases.collectAsState()
                var editing by remember { mutableStateOf<EditTarget?>(null) }

                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                var smartCase by remember { mutableStateOf(vm.smartCase()) }

                val exportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json"),
                ) { uri ->
                    if (uri != null) scope.launch(Dispatchers.IO) {
                        val json = vm.exportJson()
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(json.toByteArray())
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Phrases exported", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    if (uri != null) scope.launch(Dispatchers.IO) {
                        val text = context.contentResolver.openInputStream(uri)
                            ?.bufferedReader()?.use { it.readText() }
                        val count = text?.let { runCatching { vm.importJson(it, replace = false) }.getOrNull() }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                if (count != null) "Imported $count phrases" else "Couldn't read that file",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }

                val target = editing
                if (target == null) {
                    PhraseListScreen(
                        phrases = phrases,
                        smartCase = smartCase,
                        onToggleSmartCase = { value -> smartCase = value; vm.setSmartCase(value) },
                        onExport = { exportLauncher.launch("steno-backup.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
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
    smartCase: Boolean,
    onToggleSmartCase: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Phrase) -> Unit,
    onDelete: (Phrase) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Steno") },
                actions = {
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (smartCase) "Smart case: on" else "Smart case: off") },
                            onClick = { onToggleSmartCase(!smartCase); menuOpen = false },
                            leadingIcon = {
                                if (smartCase) Icon(Icons.Default.Check, contentDescription = null)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Export phrases") },
                            onClick = { menuOpen = false; onExport() },
                        )
                        DropdownMenuItem(
                            text = { Text("Import phrases") },
                            onClick = { menuOpen = false; onImport() },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add phrase")
            }
        },
    ) { padding ->
        // Tapping the trash icon stages a phrase here; the delete only runs once
        // the user confirms, so one stray tap can't wipe a phrase.
        var pendingDelete by remember { mutableStateOf<Phrase?>(null) }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ServiceStatusCard()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Clear the floating + button so the last row's delete stays tappable.
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(phrases, key = { it.id }) { phrase ->
                    PhraseRow(phrase, onClick = { onEdit(phrase) }, onDelete = { pendingDelete = phrase })
                }
            }
        }

        pendingDelete?.let { phrase ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Delete this phrase?") },
                text = {
                    Text(
                        "\"${phrase.shortcut.replace("|", "  /  ")}\" will be removed. " +
                            "This can't be undone.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete(phrase)
                        pendingDelete = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text("Cancel")
                    }
                },
            )
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
                    phrase.shortcut.replace("|", "  /  "),
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
            Spacer(Modifier.height(8.dp))
            Text(
                "Installed Steno from an APK? Android may say the accessibility setting is " +
                    "restricted and block it. If that happens, open App info, tap the ⋮ menu " +
                    "at the top right, choose \"Allow restricted settings\", then come back and " +
                    "turn Steno on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }) {
                Text("Open accessibility settings")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            }) {
                Text("Open app info")
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
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
            Text(
                "Need a few triggers for one phrase? Separate them with |.\nExample:  " +
                    "phoneno | pno. Capitalized typing like Phoneno already works on its own " +
                    "while Smart case is on (menu at the top right).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
