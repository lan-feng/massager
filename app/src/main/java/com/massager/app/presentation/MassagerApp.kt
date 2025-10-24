package com.massager.app.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.massager.app.presentation.navigation.MassagerNavHost
import com.massager.app.presentation.theme.MassagerTheme

@Composable
fun MassagerApp() {
    MassagerTheme {
        Surface {
            MassagerNavHost()
        }
    }
}
