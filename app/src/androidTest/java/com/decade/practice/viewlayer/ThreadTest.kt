package com.decade.practice.viewlayer

import androidx.test.core.app.ActivityScenario
import com.decade.practice.repository.AccountRepository
import com.decade.practice.view.activity.ThreadActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class ThreadTest {

    @Inject
    lateinit var accountRepository: AccountRepository

    @get:Rule
    val hiltRule = HiltAndroidRule(this)


    @Before
    fun setUp() = runBlocking(Dispatchers.Main) {
        hiltRule.inject()
        accountRepository.logIn("Luffy", "Luffy")
        return@runBlocking
    }

    @After
    fun tearDown() = runBlocking(Dispatchers.Main) {
        accountRepository.logOut()
    }

    @Test
    fun testThreadView() {
        val scenario = ActivityScenario.launch(ThreadActivity::class.java)
        Thread.sleep(15 * 60 * 1000)
        scenario.close()
    }
}