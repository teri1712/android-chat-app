package com.decade.practice.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.decade.practice.model.presentation.AuthenticationState
import com.decade.practice.theme.ApplicationTheme
import com.decade.practice.viewmodel.LoginViewModel
import com.decade.practice.viewmodel.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.util.Date

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {
      override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            runBlocking { }
            enableEdgeToEdge()
            setContent {
                  ApplicationTheme {
                        LoginScreen()
                  }
            }
      }
}

private fun Activity.launchThreadActivity() {
      val intent = Intent(this, ThreadActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
      startActivity(intent)
      finish()
}

@Serializable
object LoginRoute

@Serializable
object SignUpRoute

@Composable
fun LoginScreen() {
      val navController = rememberNavController()
      NavHost(
            navController = navController,
            startDestination = LoginRoute,
            enterTransition = {
                  slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(700))
            },
            exitTransition = {
                  slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(700))
            }
      ) {
            composable<LoginRoute> {
                  LoginScreen(navController)
            }
            composable<SignUpRoute> {
                  SignUpScreen(navController)
            }
      }
}

@Composable
fun LoginScreen(navController: NavController) {
      val vm: LoginViewModel = hiltViewModel()
      val state by vm.loginState.collectAsState()

      val inProgress by remember {
            derivedStateOf {
                  state == AuthenticationState.InProgress
            }
      }
      val error by remember {
            derivedStateOf {
                  if (state is AuthenticationState.Error)
                        return@derivedStateOf state.message
                  ""
            }
      }

      if (state == AuthenticationState.Success) {
            val activity = LocalContext.current as LoginActivity
            activity.launchThreadActivity()
      }
      com.decade.practice.composable.LoginScreen(
            navController,
            error,
            inProgress,
            onSubmit = { username, password ->
                  vm.signIn(username, password)
            },
            onGoogleLogin = { accessToken ->
                  vm.signIn(accessToken)
            }
      )
}

@Composable
fun SignUpScreen(navController: NavHostController) {
      val vm: SignUpViewModel = hiltViewModel()
      val state by vm.signUpState.collectAsState()
      val error by remember {
            derivedStateOf {
                  if (state is AuthenticationState.Error)
                        return@derivedStateOf state.message
                  ""
            }
      }
      val inProgress by remember {
            derivedStateOf {
                  state == AuthenticationState.InProgress
            }
      }
      if (state == AuthenticationState.Success) {
            val activity = LocalContext.current as LoginActivity
            activity.launchThreadActivity()
      }
      com.decade.practice.composable.SignUpScreen(
            error,
            inProgress,
            navController
      )
      { username, password, fullname, gender, dob, uri ->
            vm.signUp(username, password, fullname, gender, Date(dob), uri)
      }
}


