package com.decade.practice.viewlayer

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.decade.practice.activity.LoginActivity
import com.decade.practice.session.AccountRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class LoginViewTest {

      @Inject
      lateinit var accountRepository: AccountRepository

      @get:Rule
      val hiltRule = HiltAndroidRule(this)


      @Before
      fun setUp() = runTest {
            withContext(Dispatchers.Main) {
                  hiltRule.inject()
            }
      }

      @After
      fun tearDown() = runTest {
            withContext(Dispatchers.Main) {
                  accountRepository.logOut()
            }
      }

      @get:Rule
      val composeRule = createAndroidComposeRule<LoginActivity>()

      @Test
      fun testLoginView() {
            composeRule.onNodeWithTag("username")
                  .performTextInput("first")

            composeRule.onNodeWithTag("password")
                  .performTextInput("first")

            composeRule.onNodeWithTag("submit")
                  .performClick()

            val current = System.currentTimeMillis()
            composeRule.waitUntil(timeoutMillis = 6000) {
                  System.currentTimeMillis() - current >= 5000
            }
      }
}