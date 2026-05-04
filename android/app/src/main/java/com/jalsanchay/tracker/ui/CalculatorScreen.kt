package com.jalsanchay.tracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jalsanchay.tracker.data.UserSetup
import com.jalsanchay.tracker.util.Calculations

@Composable
fun CalculatorScreen(
    setup: UserSetup,
    onBack: () -> Unit,
    onLogToday: (Double) -> Unit
) {
    var rainfall by remember { mutableFloatStateOf(25f) }
    var useMySetup by remember { mutableStateOf(true) }
    var customArea by remember(setup.roofArea) { mutableStateOf(setup.roofArea.toInt().toString()) }
    var customTank by remember(setup.tankCapacity) { mutableStateOf(setup.tankCapacity.toInt().toString()) }
    val roofArea = if (useMySetup) setup.roofArea else customArea.toDoubleOrNull() ?: setup.roofArea
    val tankCapacity = if (useMySetup) setup.tankCapacity else customTank.toDoubleOrNull() ?: setup.tankCapacity
    val raw by remember(rainfall, roofArea, tankCapacity, setup.unit, setup.runoffCoefficient) {
        derivedStateOf {
            Calculations.calculateRawWaterCollected(
                roofArea = roofArea,
                unit = setup.unit,
                rainfallMm = rainfall.toDouble(),
                runoffCoeff = setup.runoffCoefficient
            )
        }
    }
    val collected by remember(raw, roofArea, tankCapacity, setup.unit, setup.runoffCoefficient) {
        derivedStateOf {
            Calculations.calculateWaterCollected(
                roofArea = roofArea,
                unit = setup.unit,
                rainfallMm = rainfall.toDouble(),
                runoffCoeff = setup.runoffCoefficient,
                tankCapacity = tankCapacity
            )
        }
    }
    val overflow = (raw - tankCapacity).coerceAtLeast(0.0)
    val remaining = (tankCapacity - collected).coerceAtLeast(0.0)
    val tankPct = Calculations.calculateTankPercentage(collected, tankCapacity)

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("Rainfall Potential Calculator", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Estimate collection for any rainfall scenario", color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionCard {
            Text("${rainfall.toInt()} mm", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Slider(value = rainfall, onValueChange = { rainfall = it }, valueRange = 0f..200f)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Light", style = MaterialTheme.typography.labelSmall)
                Text("Moderate", style = MaterialTheme.typography.labelSmall)
                Text("Heavy", style = MaterialTheme.typography.labelSmall)
                Text("Extreme", style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { useMySetup = true }, enabled = !useMySetup) { Text("Use my setup") }
                Button(onClick = { useMySetup = false }, enabled = useMySetup) { Text("Custom") }
            }
            AnimatedVisibility(!useMySetup) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(customArea, { customArea = it }, label = { Text("Roof area") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(customTank, { customTank = it }, label = { Text("Tank capacity") }, modifier = Modifier.fillMaxWidth())
                }
            }
            Text("Your roof: ${setup.runoffLabel} (${setup.runoffCoefficient})")
        }

        SectionCard {
            Text("${collected.toInt()} L", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("estimated collection", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("Impact", "${"%.1f".format(Calculations.calculateImpactScore(collected))} days", Modifier.weight(1f))
                MiniStat("Tank", "${(tankPct * 100).toInt()}% full", Modifier.weight(1f))
                MiniStat("Overflow", if (overflow > 0) "${overflow.toInt()} L" else "${remaining.toInt()} L left", Modifier.weight(1f))
            }
            LinearProgressIndicator(progress = { tankPct }, modifier = Modifier.fillMaxWidth())
        }

        SectionCard {
            Text("That is equivalent to:", fontWeight = FontWeight.Bold)
            Text("${(collected / 6).toInt()} toilet flushes")
            Text("${(collected / 70).toInt()} loads of laundry")
            Text("${(collected / 8).toInt()} minutes of shower")
        }

        Button(onClick = { onLogToday(rainfall.toDouble()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Log this as today's entry")
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold)
    }
}
