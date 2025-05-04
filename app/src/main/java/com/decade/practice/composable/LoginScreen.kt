package com.decade.practice.composable

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.decade.practice.R
import com.decade.practice.activity.SignUpRoute
import com.decade.practice.theme.ApplicationTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException


@Composable
fun LoginScreen(
      navController: NavController,
      error: String,
      inProgress: Boolean,
      onSubmit: (username: String, password: String) -> Unit,
      onGoogleLogin: (accessToken: String) -> Unit
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
      var password by rememberSaveable {
            mutableStateOf("")
      }

      Column(
            modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 50.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
      ) {
            Text(
                  text = "LOGIN",
                  modifier = Modifier.padding(vertical = 20.dp),
                  style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold
                  )
            )

            if (inProgress) {
                  CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        modifier = Modifier
                              .size(28.dp)
                  )
            }
            if (error.isNotEmpty()) {
                  Box(
                        modifier = Modifier
                              .align(Alignment.Start)
                              .padding(vertical = 10.dp)
                  ) {
                        ErrorValidation(error)
                  }
            }

            UsernameTextField(username = username, error = usernameError, onChange = { username = it })
            PasswordTextField(password = password, error = passwordError, onChange = { password = it })

            RoundedButton(
                  textContent = "Login",
                  modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .testTag("submit"),
                  onClick = {
                        onSubmit(username, password)
                  },
                  enabled = !inProgress
            )
            Text(
                  text = "Don't have an account?", style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,

                        color = MaterialTheme.colorScheme.primary
                  ),
                  modifier = Modifier
                        .padding(top = 10.dp)
                        .clickable {
                              navController.navigate(SignUpRoute)
                        }
            )
            LoginGoogle(onGoogleLogin)
      }
}

@Composable
private fun LoginGoogle(onGoogleLogin: (accessToken: String) -> Unit = { }) {
      val context = LocalContext.current
      val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                  .requestIdToken(context.getString(R.string.google_client_id))
                  .requestEmail()
                  .build()


      val googleSignInClient =
            GoogleSignIn.getClient(context, gso)

      val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
      ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                  val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                  try {
                        val account = task.getResult(ApiException::class.java)
                        Toast.makeText(context, account.email, Toast.LENGTH_SHORT).show()
                        val accessToken = account.idToken!!
                        onGoogleLogin(accessToken)
                  } catch (e: ApiException) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                  }
            } else {
                  Toast.makeText(context, result.resultCode, Toast.LENGTH_SHORT).show()
            }
      }

      Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                  .padding(top = 10.dp)
                  .clickable {
                        val signInIntent = googleSignInClient.signInIntent
                        launcher.launch(signInIntent)
                  }) {
            Icon(
                  ImageVector.vectorResource(id = R.drawable.google),
                  contentDescription = "Login with google",
                  modifier = Modifier.size(20.dp),
                  tint = Color.Unspecified
            )

            Text(text = "Or Sign In With Google", style = MaterialTheme.typography.bodyMedium)
      }
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
            placeholder = {
                  Text(text = "Username")
            },
            isError = error,

            leadingIcon = {
                  Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.username),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                  )
            },
            modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 5.dp)
                  .testTag("username")
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
            isError = error,
            placeholder = {
                  Text(text = "Password")
            },
            leadingIcon = {
                  Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.password),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                  )
            },
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
                  .testTag("password")

      )
}

@Preview(showBackground = true, device = "id:pixel")
@Composable
fun LoginPreview() {
      ApplicationTheme {
            val navController = rememberNavController()
            LoginScreen(navController, "Username exists", true, { username, password -> }, {})
      }
}