package com.decade.practice.composable

/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
      value: Long = System.currentTimeMillis(),
      label: String? = null,
      onSelect: (Long) -> Unit = {}
) {
      var show by rememberSaveable {
            mutableStateOf(false)
      }
      val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value
      )
      TextField(
            value = convertMillisToDate(value),
            onValueChange = { },
            label = {
                  if (label != null)
                        Text(label)
            },
            readOnly = true,
            trailingIcon = {
                  IconButton(onClick = { show = !show }) {
                        Icon(
                              imageVector = Icons.Default.DateRange,
                              contentDescription = "Select date",
                              modifier = Modifier.size(20.dp)
                        )
                  }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
      )

      if (show) {
            DatePickerDialog(
                  onDismissRequest = {
                        show = false
                  },
                  confirmButton = {
                        TextButton(onClick = {
                              onSelect(datePickerState.selectedDateMillis!!)
                              show = false
                        }) {
                              Text("OK")
                        }
                  },
                  dismissButton = {
                        TextButton(onClick = {
                              show = false
                        }) {
                              Text("Cancel")
                        }
                  }
            ) {
                  DatePicker(state = datePickerState)
            }
      }
}

private fun convertMillisToDate(millis: Long): String {
      val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
      return formatter.format(Date(millis))
}