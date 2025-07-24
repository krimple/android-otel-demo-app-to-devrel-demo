/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Modified from original at:
 * https://github.com/open-telemetry/opentelemetry-android/blob/main/demo-app/src/main/java/io/opentelemetry/android/demo/MainActivity.kt
 * 
 * Changes: Modified to use Honeycomb SDK and add additional telemetry and features.
 */

package io.opentelemetry.android.demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.opentelemetry.android.demo.about.AboutActivity
import io.opentelemetry.android.demo.theme.DemoAppTheme
import io.opentelemetry.android.demo.shop.ui.AstronomyShopActivity

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<DemoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Row(
                            Modifier.padding(all = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CenterText(
                                fontSize = 40.sp,
                                text =
                                    buildAnnotatedString {
                                        withStyle(style = SpanStyle(color = Color(0xFFF5A800))) {
                                            append("Open")
                                        }
                                        withStyle(style = SpanStyle(color = Color(0xFF425CC7))) {
                                            append("Telemetry")
                                        }
                                        withStyle(style = SpanStyle(color = Color.Black)) {
                                            append(" Android Demo")
                                        }
                                        toAnnotatedString()
                                    },
                            )
                        }
                        SessionId(viewModel.sessionIdState)
                        MainOtelButton(
                            painterResource(id = R.drawable.otel_icon),
                        )
                        val context = LocalContext.current
                        LauncherButton(text = "Go shopping", onClick = {
                            context.startActivity(Intent(this@MainActivity, AstronomyShopActivity::class.java))
                        })
                        LauncherButton(text = "Learn more", onClick = {
                            context.startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                        })

                    }
                }
                Log.d(TAG, "Main Activity started ")
            }
        }
    }
}
