package com.decade.practice.datalayer

import com.decade.practice.net.ChatSubscription
import com.decade.practice.net.api.eventCall
import com.decade.practice.repository.AccountRepository
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
class TypeTest {

    @Inject
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var retrofit: Retrofit

    lateinit var sub: ChatSubscription

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() = runTest {
        withContext(Dispatchers.Main) {
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

    @Test
    fun testTyping() {
        Thread.sleep(2000)
        runTest {
            withContext(Dispatchers.Main) {
                val session = accountRepository.currentSession!!
                sub = session.onlineClient.subscribeChat(testConversation("first", "second").chat)
            }
        }
        Thread.sleep(2000)

        runTest {
            withContext(Dispatchers.Main) {
                sub.ping()
            }
        }

        Thread.sleep(5000)

    }

}

@HiltAndroidTest
class ReceivingMessageTest {

    @Inject
    lateinit var accountRepository: AccountRepository
    private val secondToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6InNlY29uZCIsInBhc3N3b3JkX3ZlcnNpb24iOjAsImlkIjoiYTlmMGU2MWEtMTM3ZC0zNmFhLTlkYjUtMzQ2NWUwODAxNjEyIiwibmFtZSI6InNlY29uZCIsImlhdCI6MTc0NDEwNDk4NSwiZXhwIjoxNzQ0NzA5Nzg1fQ.ynWWkTwVpeTjWIeL0PFyeDL8xzKhC48__GRmn4blaAw"

    @Inject
    lateinit var retrofit: Retrofit

    lateinit var sub: ChatSubscription

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() = runTest {
        withContext(Dispatchers.Main) {
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


    @Test
    fun testTyping() {
        runTest {
            withContext(Dispatchers.Main) {
                val session = accountRepository.currentSession!!
                Assert.assertEquals("third", session.conversationRepository.list()[0].partner.username)
            }
        }

        Thread.sleep(2000)
        runTest {
            withContext(Dispatchers.Main) {
                val conversation = testConversation("first", "second")
                retrofit.eventCall().sendText("Bearer " + secondToken, testText(conversation, "wtfff"))
            }
        }

        Thread.sleep(2000)

    }

}