package com.decade.practice.composable

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.decade.practice.R
import com.decade.practice.activity.ErrorDialog
import com.decade.practice.activity.InProgressDialog
import com.decade.practice.theme.ApplicationTheme

@Composable
private fun AvatarMask() {
      Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                  val maxRadius = kotlin.math.min(size.width, size.height)
                  val radius = maxRadius / 2 - 50f

                  drawContext.canvas.withSaveLayer(bounds = Rect(Offset.Zero, size), paint = Paint().apply {
                        isAntiAlias = true
                  }) {

                        drawRoundRect(color = Color(0, 0, 0, 20), cornerRadius = CornerRadius(0f, 0f))
                        drawCircle(
                              color = Color.Transparent,
                              center = center,
                              radius = radius,
                              blendMode = BlendMode.Clear
                        )
                  }
            }
      }
}

@Composable
private fun AvatarPicker(size: Dp, uri: Uri?, onPicked: (Uri) -> Unit) = Box(
      modifier = Modifier.size(size),
      contentAlignment = Alignment.Center
) {
      val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                  onPicked(uri)
            }
      }
      AsyncImage(
            model = uri ?: R.drawable.avatar_placeholder,
            contentDescription = "avatar preview",
            modifier = Modifier.fillMaxSize()
      )

      AvatarMask()
      IconButton(onClick = {
            launcher.launch("image/*")
      }) {
            Icon(
                  imageVector = ImageVector.vectorResource(id = R.drawable.camera),
                  contentDescription = "Camera",
                  modifier = Modifier.size(30.dp),
                  tint = Color(0, 0, 0, 50)
            )
      }
}

@Composable
private fun AvatarScreen(
      signingUp: Boolean,
      onSubmit: (uri: Uri?) -> Unit
) = Column(
      modifier =
            Modifier
                  .fillMaxSize()
                  .padding(horizontal = 20.dp)
                  .padding(bottom = 100.dp, top = 20.dp),
      horizontalAlignment = Alignment.Start
) {

      var uri by rememberSaveable { mutableStateOf<Uri?>(null) }
      Text(
            text = "SETTING UP",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.displayMedium.copy(
                  fontWeight = FontWeight.Bold
            )
      )
      Text(
            text = "YOUR AVATAR",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.displayMedium.copy(
                  fontWeight = FontWeight.Bold
            )
      )
      if (signingUp) {
            InProgressDialog("Signing Up")
      }
      Box(
            modifier = Modifier
                  .weight(1f)
                  .padding(top = 10.dp)
                  .fillMaxWidth(),
            contentAlignment = Alignment.Center
      ) {
            val configuration = LocalConfiguration.current
            val avatarSize = configuration.screenWidthDp.dp - 50.dp
            AvatarPicker(size = avatarSize, uri) {
                  uri = it
            }
      }
      RoundedButton(
            textContent = "Submit",
            modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 20.dp),
            onClick = {
                  onSubmit(uri)
            },
            enabled = !signingUp
      )
}


@Composable
fun InformationScreen(
      error: String,
      signingUp: Boolean,
      onNext: (username: String, password: String, fullname: String, gender: String, dob: Long) -> Unit
) = Box(
      modifier = Modifier
            .padding(horizontal = 30.dp)
            .fillMaxSize(),
      contentAlignment = Alignment.Center
) {

      val usernameError by remember {
            derivedStateOf {
                  error.contains("Username", ignoreCase = true)
            }
      }
      val passwordError by remember {
            derivedStateOf {
                  error.contains("Password", ignoreCase = true)
            }
      }

      var username by rememberSaveable {
            mutableStateOf("")
      }

      var fullname by rememberSaveable {
            mutableStateOf("")
      }
      var password by rememberSaveable {
            mutableStateOf("")
      }
      var dob by rememberSaveable {
            mutableLongStateOf(System.currentTimeMillis())
      }

      var gender by rememberSaveable {
            mutableStateOf("Male")
      }
//    ThumbNail()
      Column(
            modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 50.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
      ) {
            Text(
                  text = "SIGN UP",
                  modifier = Modifier.padding(vertical = 20.dp),
                  style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold
                  )
            )
            if (error.isNotEmpty()) {
                  Box(
                        modifier = Modifier
                              .align(Alignment.Start)
                  ) {
                        ErrorValidation(error)
                  }
            }
            UsernameTextField(username = username, error = usernameError, onChange = { username = it })
            PasswordTextField(password = password, error = passwordError, onChange = { password = it })
            FullnameTextField(fullname = fullname, onChange = { fullname = it })
            DatePickerModal(value = dob, label = "DOB", onSelect = { dob = it })
            TextField(
                  value = gender,
                  onValueChange = { },
                  label = {
                        Text("Gender")
                  },
                  readOnly = true,
                  trailingIcon = {
                        GenderPicker {
                              gender = it
                        }
                  },
                  modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
            )
            RoundedButton(
                  textContent = "Next",
                  modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                  onClick = {
                        onNext(username, fullname, password, gender, dob)
                  },
                  enabled = !signingUp
            )

      }
}

@Composable
private fun FullnameTextField(
      fullname: String,
      onChange: (String) -> Unit = {}
) {
      TextField(
            value = fullname,
            onValueChange = onChange,
            label = {
                  Text(text = "Fullname")
            },
            modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 5.dp)
      )
}

@Composable
private fun UsernameTextField(
      username: String,
      error: Boolean,
      onChange: (String) -> Unit = {}
) {
      TextField(
            value = username,
            onValueChange = onChange,
            label = {
                  Text(text = "Username")
            },
            isError = error,
            singleLine = true,
            modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 5.dp)
      )
}

@Composable
private fun PasswordTextField(
      password: String,
      error: Boolean,
      onChange: (String) -> Unit = {}
) {
      var show by rememberSaveable {
            mutableStateOf(false)
      }
      TextField(
            value = password,
            onValueChange = onChange,
            label = {
                  Text(text = "Password")
            },
            isError = error,
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = if (show) KeyboardOptions.Default else KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                  IconButton(onClick = {
                        show = !show
                  }) {
                        val ic = if (show) R.drawable.show else R.drawable.hide
                        Icon(
                              ImageVector.vectorResource(id = ic),
                              contentDescription = "Clear text",
                              modifier = Modifier.size(20.dp)
                        )
                  }
            },
            modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 5.dp)
      )
}

@Composable
fun GenderPicker(onGenderChanged: (String) -> Unit) {
      var expanded by remember { mutableStateOf(false) }
      val options = listOf("Male", "Female", "Other")
      Box {
            IconButton(onClick = { expanded = !expanded }) {
                  Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "More options",
                        modifier = Modifier.size(20.dp)
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
                                    onGenderChanged(option)
                              }
                        )
                  }
            }
      }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
      error: String,
      signingUp: Boolean,
      parentNavController: NavHostController,
      onSubmit: (username: String, password: String, fullname: String, gender: String, dob: Long, uri: Uri?) -> Unit
) {
      var currentProgress by rememberSaveable {
            mutableStateOf(0.3f)
      }

      var _username by rememberSaveable {
            mutableStateOf("")
      }

      var _fullname by rememberSaveable {
            mutableStateOf("")
      }
      var _password by rememberSaveable {
            mutableStateOf("")
      }
      var _dob by rememberSaveable {
            mutableLongStateOf(System.currentTimeMillis())
      }

      var _gender by rememberSaveable {
            mutableStateOf("Male")
      }

      val navController = rememberNavController()
      Scaffold(
            topBar = {
                  CenterAlignedTopAppBar(
                        title = {
                              SignUpProgress(currentProgress)
                        },
                        navigationIcon = {
                              IconButton(onClick = {
                                    if (currentProgress != 0f)
                                          navController.popBackStack()
                                    else
                                          parentNavController.popBackStack()
                              }, enabled = !signingUp) {
                                    Icon(
                                          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                          contentDescription = "Move back"
                                    )
                              }
                        }, actions = {
                              Spacer(modifier = Modifier.size(40.dp))
                        }
                  )
            },
      ) { innerPadding ->
            NavHost(navController, startDestination = "information", Modifier.padding(innerPadding)) {
                  composable("information") {
                        currentProgress = 0f
                        InformationScreen(error, signingUp)
                        { username, fullname, password, gender, dob ->
                              _username = username
                              _fullname = fullname
                              _password = password
                              _gender = gender
                              _dob = dob
                              navController.navigate("avatar")
                        }
                  }
                  composable("avatar") {
                        currentProgress = 1f
                        AvatarScreen(signingUp) { uri ->
                              onSubmit(_username, _password, _fullname, _gender, _dob, uri)
                        }
                  }
            }
      }

      var alert by remember(error) { mutableStateOf(error.isNotEmpty()) }
      if (alert) {
            ErrorDialog(error, onDismissRequest = {
                  alert = false
            })
      }
}

@Preview(showBackground = true, device = "id:pixel")
@Composable
fun SignUpPreview() {
      ApplicationTheme {
            SignUpScreen(error = "", signingUp = false, parentNavController = rememberNavController()) { username, password, fullname, gender, dob, uri ->

            }
      }
}