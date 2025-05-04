package com.decade.practice.viewlayer


import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.decade.practice.SHARED_PREFERENCES_NAME
import com.decade.practice.datalayer.testConversation
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
import retrofit2.Retrofit
import javax.inject.Inject

@HiltAndroidTest
class NotificationTest {

      @Inject
      lateinit var accountRepository: AccountRepository

      @Inject
      lateinit var retrofit: Retrofit

      @get:Rule
      val hiltRule = HiltAndroidRule(this)

      private val secondToken =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6InNlY29uZCIsInBhc3N3b3JkX3ZlcnNpb24iOjAsImlkIjoiYTlmMGU2MWEtMTM3ZC0zNmFhLTlkYjUtMzQ2NWUwODAxNjEyIiwibmFtZSI6InNlY29uZCIsImlhdCI6MTc0Mzg3NzczNywiZXhwIjoxNzQ0NDgyNTM3fQ.QKeCusPvZGF1g9FSqUoC6YvMwMtcD_X8Q1QiITyQfzk"

      @Before
      fun setUp() = runTest {
            withContext(Dispatchers.Main) {
                  val context = InstrumentationRegistry.getInstrumentation().targetContext
                  context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                        .clear().commit()
                  context.databaseList().forEach { dbName ->
                        context.deleteDatabase(dbName)
                  }

                  hiltRule.inject()
                  accountRepository.logIn("first", "first")
            }
      }

      @After
      fun tearDown() = runTest {
            withContext(Dispatchers.Main) {
                  accountRepository.logOut()
            }
      }

      fun secondSendText() {
            val conversation = testConversation("first", "second")
//        runTest {
//            withContext(Dispatchers.Main) {
//                val session = accountRepository.currentSession!!
//                Assert.assertEquals("third", session.conversationRepository.list()[0].partner.username)
//                retrofit.eventCall().sendText("Bearer " + secondToken, mockText(conversation, "Xin chào mọi người"))
//            }
//        }
            Thread.sleep(60000)
      }

      @Test
      fun testSecondSendText() {
            secondSendText()
      }
}

