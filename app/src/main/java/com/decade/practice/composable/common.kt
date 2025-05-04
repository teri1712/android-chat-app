package com.decade.practice.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.decade.practice.R
import com.decade.practice.theme.ApplicationTheme

@Composable
fun RoundedButton(
      modifier: Modifier = Modifier,
      textContent: String = "",
      onClick: () -> Unit = {},
      enabled: Boolean = true
) {
      Button(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            enabled = enabled,
//        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue, contentColor = Color.Black),
            modifier = modifier

      ) {
            Text(
                  text = textContent,
                  style = MaterialTheme.typography.bodyLarge,
                  modifier = Modifier.padding(3.dp)
            )
      }
}

@Composable
fun ErrorValidation(error: String) = Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp)
) {
      Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.error),
            contentDescription = "Error message",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(15.dp)
      )
      Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium.copy(
                  color = MaterialTheme.colorScheme.error,
            ),
      )
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit) = Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier
          .height(35.dp)
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))

) {
      Row(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
      ) {
            BasicTextField(
                  value = value, onValueChange = onChange,
                  decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                              Text(
                                    text = "Message", style = MaterialTheme.typography.bodyLarge
                              )
                        }
                        innerTextField()
                  },
            )
            Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = "Search",
            )
      }

}


@Preview(showBackground = true, device = "id:pixel")
@Composable
fun SearchFieldPreview() {
      ApplicationTheme {
            Box(
                  modifier = Modifier
                      .fillMaxSize()
                      .padding(20.dp)
            ) {
                  SearchField(value = "") {
                  }

            }

      }
}