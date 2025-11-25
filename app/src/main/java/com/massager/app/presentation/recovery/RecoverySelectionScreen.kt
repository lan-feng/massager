package com.massager.app.presentation.recovery

// 文件说明：账号恢复方式选择界面。
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.massager.app.domain.model.RecoveryMassagerOption

@Composable
fun RecoverySelectionScreen(
    options: List<RecoveryMassagerOption>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Recovery Plans",
            style = MaterialTheme.typography.titleLarge
        )
        if (options.isEmpty()) {
            Text(
                text = "Upcoming recovery flows will appear here.",
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            options.forEach { option ->
                Text(
                    text = option.title,
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = option.description,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
