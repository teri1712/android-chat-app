package com.decade.practice.view.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.decade.practice.R
import com.decade.practice.view.theme.ApplicationTheme


@Composable
fun ErrorDialog(error: String, onDismissRequest: () -> Unit) = AlertDialog(
    icon = {
        Icon(
            ImageVector.vectorResource(R.drawable.error),
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error
        )
    },
    title = {
        Text(text = "SIGN UP FAILED")
    },
    text = {
        Text(text = error)
    },
    onDismissRequest = {
        onDismissRequest()
    },
    confirmButton = {
        TextButton(
            onClick = {
                onDismissRequest()
            }
        ) {
            Text("Ok")
        }
    },
    dismissButton = {
        TextButton(
            onClick = {
                onDismissRequest()
            }
        ) {
            Text("Dismiss")
        }
    }
)

@Composable
fun InProgressDialog(message: String) {
    Dialog(
        onDismissRequest = { }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        25.dp
                    )
                )
                .padding(vertical = 20.dp, horizontal = 50.dp)

        ) {
            CircularProgressIndicator(
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(20.dp)
                    .padding(vertical = 5.dp)
            )
            Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)

        }
    }
}


@Preview(showBackground = true, device = "id:pixel")
@Composable
@ExperimentalMaterial3Api
private fun InProgressDialogPreview() {
    ApplicationTheme {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            InProgressDialog("Logging out")
        }
    }
}
