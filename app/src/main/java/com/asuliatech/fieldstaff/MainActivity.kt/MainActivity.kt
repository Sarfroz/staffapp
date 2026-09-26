package com.asuliatech.fieldstaff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme { InstallationApp() } }
  }
}

@Composable
fun InstallationApp() {
  var step by remember { mutableStateOf(1) }
  var school by remember { mutableStateOf("") }
  var contact by remember { mutableStateOf("") }
  var terminal by remember { mutableStateOf("") }
  var imei by remember { mutableStateOf("") }
  var sim by remember { mutableStateOf("") }
  var terminals by remember { mutableStateOf(listOf<String>()) }
  var testIndex by remember { mutableStateOf(0) }
  var parentCall by remember { mutableStateOf(false) }
  var voiceOk by remember { mutableStateOf(false) }
  var evidence by remember { mutableStateOf(false) }

  Column(
    Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text("AsuliaTech", style = MaterialTheme.typography.headlineMedium)
    Text("New school installation", style = MaterialTheme.typography.titleLarge)
    Text("Installation step " + step + " of 4", color = MaterialTheme.colorScheme.primary)
    HorizontalDivider()

    when (step) {
      1 -> {
        Text("School and terminals", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(school, { school = it }, label = { Text("School name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(contact, { contact = it }, label = { Text("Contact person") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(terminal, { terminal = it }, label = { Text("Terminal ID") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(imei, { imei = it.take(6) }, label = { Text("IMEI last 6 digits") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(sim, { sim = it.take(6) }, label = { Text("SIM last 6 digits") }, modifier = Modifier.fillMaxWidth())
        Button(
          onClick = {
            terminals = terminals + (terminal + " · IMEI " + imei + " · SIM " + sim)
            terminal = ""
            imei = ""
            sim = ""
          },
          enabled = terminal.isNotBlank() && imei.length == 6 && sim.length == 6,
          modifier = Modifier.fillMaxWidth()
        ) { Text("+ Add another terminal") }
        terminals.forEachIndexed { index, item ->
          Text("Terminal " + (index + 1) + ": " + item + " · Pending activation")
        }
        Button(
          onClick = { step = 2 },
          enabled = school.isNotBlank() && contact.isNotBlank() && terminals.isNotEmpty(),
          modifier = Modifier.fillMaxWidth()
        ) { Text("Continue to activation") }
      }
      2 -> {
        Text("Activate all terminals", style = MaterialTheme.typography.titleMedium)
        Text("Confirm that every physical terminal has been activated at the school.")
        terminals.forEachIndexed { index, item ->
          Card(Modifier.fillMaxWidth()) {
            Text("Terminal " + (index + 1) + " · Active", Modifier.padding(14.dp), color = MaterialTheme.colorScheme.primary)
          }
        }
        Button(onClick = { step = 3 }, modifier = Modifier.fillMaxWidth()) { Text("Start physical tests") }
      }
      3 -> {
        Text("Physical terminal test", style = MaterialTheme.typography.titleMedium)
        Text("Terminal " + (testIndex + 1) + ": " + terminals[testIndex])
        Text("Make the parent call from this physical terminal. The app only records the result.")
        Row { Checkbox(parentCall, { parentCall = it }); Text("Parent call completed") }
        Row { Checkbox(voiceOk, { voiceOk = it }); Text("Voice clear on both sides") }
        Row { Checkbox(evidence, { evidence = it }); Text("GPS, time and photo/video evidence saved") }
        Button(
          onClick = {
            if (testIndex + 1 < terminals.size) {
              testIndex++
              parentCall = false
              voiceOk = false
              evidence = false
            } else {
              step = 4
            }
          },
          enabled = parentCall && voiceOk && evidence,
          modifier = Modifier.fillMaxWidth()
        ) { Text(if (testIndex + 1 < terminals.size) "Save and test next terminal" else "View installation report") }
      }
      else -> {
        Text("Installation report", style = MaterialTheme.typography.titleMedium)
        Text(school + " · Contact: " + contact)
        Text("Total terminals: " + terminals.size + " · Activated: " + terminals.size + " · Tested: " + terminals.size)
        Text("Evidence: Complete", color = MaterialTheme.colorScheme.primary)
        terminals.forEachIndexed { index, item -> Text("✓ Terminal " + (index + 1) + ": " + item) }
        Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Submit installation") }
      }
    }
  }
}
