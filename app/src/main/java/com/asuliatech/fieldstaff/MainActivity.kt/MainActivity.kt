package com.asuliatech.fieldstaff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme { InstallationApp() } }
  }
}

@Composable
fun InstallationApp() {
  var school by remember { mutableStateOf("") }
  var contact by remember { mutableStateOf("") }
  var terminal by remember { mutableStateOf("") }
  var imei by remember { mutableStateOf("") }
  var sim by remember { mutableStateOf("") }
  var terminals by remember { mutableStateOf(listOf<String>()) }
  Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("AsuliaTech", style = MaterialTheme.typography.headlineMedium)
    Text("New school installation", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(school, { school = it }, label = { Text("School name") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(contact, { contact = it }, label = { Text("Contact person") }, modifier = Modifier.fillMaxWidth())
    HorizontalDivider()
    Text("Add terminals", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(terminal, { terminal = it }, label = { Text("Terminal ID") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(imei, { imei = it.filter(Char::isDigit).take(6) }, label = { Text("IMEI last 6 digits") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(sim, { sim = it.filter(Char::isDigit).take(6) }, label = { Text("SIM last 6 digits") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    Button(onClick = { terminals = terminals + "$terminal · IMEI $imei · SIM $sim"; terminal = ""; imei = ""; sim = "" }, enabled = terminal.isNotBlank() && imei.length == 6 && sim.length == 6, modifier = Modifier.fillMaxWidth()) { Text("Add terminal") }
    Text("Added terminals: ${terminals.size}", style = MaterialTheme.typography.titleMedium)
    terminals.forEach { Text("✓ $it") }
    Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Activate all — API pending") }
    Text("Physical test call, GPS/time evidence and server verification will be enabled after the backend API is connected.")
  }
}
