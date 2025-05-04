package com.decade.practice.datalayer


import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.decade.practice.SHARED_PREFERENCES_NAME
import com.decade.practice.endpoints.eventCall
import com.decade.practice.session.AccountRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import javax.inject.Inject

@HiltAndroidTest
class ConversationTest {

      @Inject
      lateinit var accountRepository: AccountRepository

      @Inject
      lateinit var retrofit: Retrofit

      @get:Rule
      val hiltRule = HiltAndroidRule(this)

      private val namiToken =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6Ik5hbWkiLCJwYXNzd29yZF92ZXJzaW9uIjowLCJpZCI6IjE0OGEyMGJjLTA0OWYtM2Y1NC04NTIxLTYxMjkzNGY5MTE5OCIsIm5hbWUiOiJOYW1pIiwiaWF0IjoxNzQ0Mzk2NDc4LCJleHAiOjE3NDUwMDEyNzh9.pKbl12MlWozZnK1j4UlJxqEcynxeXuLsRSLB3fxkk8c"

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
                  accountRepository.logIn("Luffy", "Luffy")
            }
      }


      @After
      fun tearDown() = runTest {
            withContext(Dispatchers.Main) {
                  accountRepository.logOut()
            }
      }


      @Test
      fun test() {
            val conversation = testConversation("Luffy", "Nami")
            val session = accountRepository.currentSession!!


            runTest {
                  withContext(Dispatchers.Main) {
                        Assert.assertEquals(session.dialogRepository.list().size, 3)
                        Assert.assertEquals(session.dialogRepository.list()[1].conversation.partner.username, "Nami")
                        Assert.assertEquals(1, session.dialogRepository.get(conversation).messages.size)
                  }
            }

            runTest {
                  withContext(Dispatchers.Main) {
                        retrofit.eventCall().sendText("Bearer " + namiToken, testText(conversation, "ekkk"))
                  }
            }
            Thread.sleep(2000)

            runTest {
                  withContext(Dispatchers.Main) {
                        session.messageService.send(conversation, "wtf")
                  }
            }
            Thread.sleep(2000)
            runTest {
                  withContext(Dispatchers.Main) {
                        retrofit.eventCall().sendSeen("Bearer " + namiToken, testSeen(conversation))
                  }
            }

            Thread.sleep(2000)

            runTest {
                  withContext(Dispatchers.Main) {
                        Assert.assertTrue(session.dialogRepository.get(conversation).messages[0].seen)
                        Assert.assertEquals(3, session.dialogRepository.get(conversation).messages.size)
                  }
            }

      }
}

