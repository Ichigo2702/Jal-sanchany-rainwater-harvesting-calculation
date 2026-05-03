package com.jalsanchay.tracker.widget

import android.content.Context
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.glance.Button
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.compose.ui.unit.dp
import com.jalsanchay.tracker.MainActivity

class JalSanchayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            JalSanchayWidgetContent(context)
        }
    }
}

@Composable
fun JalSanchayWidgetContent(context: Context) {
    val deepLinkKey = ActionParameters.Key<String>("DEEP_LINK_SCREEN")
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
            .padding(16.dp)
    ) {
        Text("Jal-Sanchay")
        Text("Open app for latest saved days")
        Text("Tank and rain forecast update after sync")
        Spacer(GlanceModifier.height(8.dp))
        Button(
            "Log Rainfall",
            onClick = actionStartActivity<MainActivity>(
                actionParametersOf(deepLinkKey to "entry")
            )
        )
    }
}
