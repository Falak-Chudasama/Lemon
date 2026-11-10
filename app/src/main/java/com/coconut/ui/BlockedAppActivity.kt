package com.coconut.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coconut.R

class BlockedAppActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val ACTION_BLOCKED_APP = "com.coconut.action.BLOCKED_APP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL)
            ?: intent.getStringExtra("app_name")
            ?: "This application"

        setContent {
            LemonTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = LemonTheme.shapes.dialog,
                        color = LemonTheme.colors.cardBackground,
                        border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                        shadowElevation = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_lemon_mark),
                                contentDescription = "Lemon Logo",
                                modifier = Modifier.size(56.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Application Snapped",
                                style = LemonTheme.typography.displaySmall.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center,
                                color = LemonTheme.colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "$appLabel is currently restricted by Lemon.",
                                style = LemonTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                textAlign = TextAlign.Center,
                                color = LemonTheme.colors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(26.dp))

                            Button(
                                onClick = {
                                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                        addCategory(Intent.CATEGORY_HOME)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    startActivity(homeIntent)
                                    finish()
                                },
                                shape = LemonTheme.shapes.button,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LemonTheme.colors.primaryAccent,
                                    contentColor = LemonTheme.colors.onPrimaryAccent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Return Home",
                                    style = LemonTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
