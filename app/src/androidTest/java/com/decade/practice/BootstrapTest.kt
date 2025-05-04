package com.decade.practice

import androidx.test.core.app.ActivityScenario
import com.decade.practice.activity.ThreadActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class BootstrapTest {

      @get:Rule
      val hiltRule = HiltAndroidRule(this)

      lateinit var scenario: ActivityScenario<ThreadActivity>


      @Before
      fun setUp() {
            scenario = ActivityScenario.launch(ThreadActivity::class.java)
      }

      @Test
      fun launchActivityForManualInteraction() {
            Thread.sleep(60 * 1000)
      }

      @After
      fun tearDown() {
            scenario.close()
      }

}
