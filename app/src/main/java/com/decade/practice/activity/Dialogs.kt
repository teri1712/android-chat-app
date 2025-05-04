package com.decade.practice.activity

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.decade.practice.R
import com.decade.practice.model.domain.ImageSpec
import com.decade.practice.service.DownloadService
import com.decade.practice.service.FILENAME
import com.decade.practice.theme.ApplicationTheme


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


@Composable
fun ImageDialog(image: ImageSpec, onDismissRequest: () -> Unit) = Dialog(
      onDismissRequest = onDismissRequest,
      properties = DialogProperties(
            usePlatformDefaultWidth = false,
      )

) {
      val uri = remember { image.uri.toUri() }
      val context = LocalContext.current
      val options = remember { listOf("Save") }
      var expanded by remember { mutableStateOf(false) }
      Column(modifier = Modifier.fillMaxSize()) {
            Row(
                  modifier = Modifier
                        .fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
            ) {

                  IconButton(onClick = { onDismissRequest() }) {
                        Icon(
                              imageVector = ImageVector.vectorResource(id = R.drawable.back),
                              contentDescription = "Back",
                              modifier = Modifier.size(25.dp),
                              tint = MaterialTheme.colorScheme.primary
                        )
                  }
                  Box {
                        IconButton(onClick = { expanded = !expanded }) {
                              Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "More options",
                                    modifier = Modifier.size(25.dp),
                                    tint = MaterialTheme.colorScheme.primary
                              )
                        }
                        DropdownMenu(
                              expanded = expanded,
                              onDismissRequest = { expanded = false }
                        ) {
                              options.forEach { option ->
                                    DropdownMenuItem(
                                          text = { Text(option) },
                                          onClick = {
                                                val intent = Intent(context, DownloadService::class.java)
                                                intent.putExtra(FILENAME, image.filename)
                                                context.startService(intent)
                                          }
                                    )
                              }
                        }
                  }
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  AsyncImage(
                        model = uri,
                        contentDescription = "Image",
                        contentScale = ContentScale.Fit,
                  )
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
