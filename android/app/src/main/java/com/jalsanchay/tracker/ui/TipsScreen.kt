package com.jalsanchay.tracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jalsanchay.tracker.model.TrackerUiState
import com.jalsanchay.tracker.model.UiState
import com.jalsanchay.tracker.viewmodel.TrackerViewModel
import java.time.LocalDate

@Composable
internal fun TipsScreen(uiState: TrackerUiState, palette: AppPalette, viewModel: TrackerViewModel) {
    var open by remember { mutableIntStateOf(0) }
    var openedTips by remember { mutableStateOf(setOf<Int>()) }
    var question by remember { mutableStateOf("") }
    var qaHistory by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    val aiTips by viewModel.aiTips.collectAsStateWithLifecycle()
    val season = seasonInfo(LocalDate.now().monthValue)
    LaunchedEffect(Unit) {
        viewModel.glossaryAnswer.collect { pair ->
            qaHistory = qaHistory + pair
        }
    }
    val tips = listOf(
        "Clean your roof before monsoon" to "Remove leaves, dust, and debris before heavy rain.",
        "Install a first-flush diverter" to "Divert initial dirty rainwater away from your tank.",
        "Check tank for cracks before season" to "Inspect joints, outlet, overflow, and lid.",
        "Use ferro-cement or food-grade plastic" to "These materials are durable for household storage.",
        "Cover your tank" to "Reduce mosquito breeding and prevent debris entry.",
        "Understand runoff coefficient" to "It is the fraction of rainfall that reaches storage.",
        "Inspect gutters monthly" to "Blocked gutters reduce collection efficiency.",
        "Keep overflow safe" to "Route overflow to garden or recharge pits.",
        "Use a mesh filter" to "Filter leaves and insects before storage.",
        "Record zero-rain days" to "It keeps reports accurate."
    )
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Tips & Education", palette)
        Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = palette.primary)
                    Text(season.text, color = palette.text, modifier = Modifier.padding(start = 12.dp))
                }
            }
            AppCard(palette) {
                Text("You have explored ${openedTips.size} of 10 tips", color = palette.muted)
                LinearProgressIndicator(progress = { openedTips.size / 10f }, modifier = Modifier.fillMaxWidth())
            }
            tips.forEachIndexed { index, tip ->
                AppCard(palette, Modifier.clickable {
                    open = if (open == index) -1 else index
                    openedTips = openedTips + index
                }.animateContentSize()) {
                    Text(tip.first, color = palette.text, fontWeight = FontWeight.Bold)
                    AnimatedVisibility(open == index) { Text(tip.second, color = palette.muted) }
                }
            }
            AppCard(palette) {
                Text("AI Personalised Tips", color = palette.text, fontWeight = FontWeight.Bold)
                Button(onClick = viewModel::fetchTips, enabled = uiState.isOnline) { Text("Get AI Tips") }
                when (val state = aiTips) {
                    UiState.Idle -> Unit
                    UiState.Loading -> CircularProgressIndicator()
                    is UiState.Success -> Text(state.value, color = palette.muted)
                    is UiState.Error -> Text(state.message, color = palette.danger)
                }
            }
            AppCard(palette) {
                Text("Glossary", color = palette.text, fontWeight = FontWeight.Bold)
                Text("Runoff Coefficient, Catchment Area, First Flush, Water Harvesting Potential, CPHEEO Standard", color = palette.muted)
                OutlinedTextField(question, { question = it }, label = { Text("Ask anything about rainwater harvesting") }, modifier = Modifier.fillMaxWidth())
                Button({ if (question.isNotBlank()) { viewModel.askGlossary(question); question = "" } }, enabled = uiState.isOnline) { Text("Ask") }
                qaHistory.forEach { pair ->
                    Text("Q: ${pair.first}", color = palette.primary, fontWeight = FontWeight.Bold)
                    Text(pair.second, color = palette.text)
                }
            }
        }
    }
}
