package com.decade.practice.datalayer

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import com.decade.practice.authentication.AuthenticationException
import com.decade.practice.authentication.Authenticator
import com.decade.practice.repository.AccountRepository
import com.decade.practice.view.composable.InputBar
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration

@HiltAndroidTest
class LoginSessionTest {

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

    @Test
    fun testLoginSession() = runTest {
        withContext(Dispatchers.Main) {
            accountRepository.logIn("first", "first")
            Assert.assertTrue(accountRepository.hasSession)
            val session = accountRepository.currentSession!!
            val chatRepository = session.conversationRepository
            val list = chatRepository.list()
            Assert.assertEquals(list.size, 3)
            Assert.assertEquals(list[0].partner.username, "third")
            Assert.assertEquals(list[1].partner.username, "second")
            Assert.assertEquals(list[2].partner.username, "admin")
        }
    }
}

@HiltAndroidTest
class LoginTest {

    @Inject
    lateinit var authenticator: Authenticator

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() = runBlocking(Dispatchers.Main) {
        hiltRule.inject()
    }

    @After
    fun tearDown() = runBlocking(Dispatchers.Main) {

    }

    @Test
    fun testLoginFailed() {
        Assert.assertThrows(AuthenticationException::class.java) {
            runTest {
                authenticator.signIn("first", "second")
            }
        }
    }

    @Test
    fun testLogin() = runTest {
        val accountEntry = authenticator.signIn("first", "first")
        Assert.assertNotNull(accountEntry.account.user.role)
        Assert.assertNotNull(accountEntry.account.user.gender)
        Assert.assertNotNull(accountEntry.account.user.id)
        Assert.assertNotNull(accountEntry.account.user.avatar)
        Assert.assertNotNull(accountEntry.account.credential.accessToken)
        Assert.assertNotNull(accountEntry.account.credential.refreshToken)
        Assert.assertTrue(accountEntry.account.credential.expiresAt > System.currentTimeMillis())
        Assert.assertEquals(accountEntry.account.credential.expiresIn, 10L * 60 * 60 * 1000)
        Assert.assertEquals(accountEntry.chatSnapshots.size, 3)
        Assert.assertEquals(accountEntry.account.username, "first")
        Assert.assertEquals(accountEntry.account.syncContext.eventVersion, 3)
        Assert.assertEquals(1, accountEntry.chatSnapshots[1].eventList.size)
        Assert.assertEquals(2, accountEntry.chatSnapshots[0].eventList.size)
        Assert.assertEquals(accountEntry.chatSnapshots[0].conversation.partner.username, "third")
        Assert.assertEquals(accountEntry.chatSnapshots[1].conversation.partner.username, "second")


        accountEntry.chatSnapshots.forEach {
            val conversation = it.conversation

            Assert.assertNotNull(conversation.partner)
            Assert.assertNotNull(conversation.partner.username)
            Assert.assertNotNull(conversation.partner.id)
            Assert.assertNotNull(conversation.partner.avatar)
            Assert.assertNotNull(conversation.partner.role)
            Assert.assertNotNull(conversation.owner)
            Assert.assertNotNull(conversation.chat)
            Assert.assertNotNull(conversation.chat.identifier)
            Assert.assertNotNull(conversation.chat.partner)
            Assert.assertNotNull(conversation.chat.owner)
        }
    }
}

@HiltAndroidTest
class SignUpTest {

    @Inject
    lateinit var authenticator: Authenticator

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Volatile
    private lateinit var uri: Uri


    @Before
    fun setUp() = runBlocking(Dispatchers.Main) {
        hiltRule.inject()
    }

    @Before
    fun pickImage() = runTest(timeout = Duration.INFINITE) {
        var picked = false
        composeTestRule.setContent {
            InputBar(onSubmitImage = { it ->
                uri = it
                picked = true
            })
        }

        while (true) {
            if (!picked)
                delay(500)
            else
                break
        }
    }

    @After
    fun tearDown() = runBlocking(Dispatchers.Main) {

    }

    @Test
    fun testLogin() = runTest {
        val username = UUID.randomUUID().toString().substring(0, 15)
        val password = UUID.randomUUID().toString().substring(0, 15)

        authenticator.signUp(username, password, "decade", "bede", Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000), uri)
        val accountEntry = authenticator.signIn(username, password)

        Assert.assertNotNull(accountEntry.account.user.role)
        Assert.assertNotNull(accountEntry.account.user.gender)
        Assert.assertNotNull(accountEntry.account.user.id)
        Assert.assertNotNull(accountEntry.account.user.avatar)
        Assert.assertNotNull(accountEntry.account.credential.accessToken)
        Assert.assertNotNull(accountEntry.account.credential.refreshToken)
        Assert.assertTrue(accountEntry.account.credential.expiresAt > System.currentTimeMillis())
        Assert.assertEquals(accountEntry.account.credential.expiresIn, 10L * 60 * 60 * 1000)
        Assert.assertEquals(accountEntry.chatSnapshots.size, 1)
        Assert.assertEquals(accountEntry.account.username, username)
        Assert.assertEquals(accountEntry.account.syncContext.eventVersion, 0)
        Assert.assertEquals(1, accountEntry.chatSnapshots[0].eventList.size)
        Assert.assertEquals(accountEntry.chatSnapshots[0].conversation.partner.username, "admin")


        accountEntry.chatSnapshots.forEach {
            val conversation = it.conversation

            Assert.assertNotNull(conversation.partner)
            Assert.assertNotNull(conversation.partner.username)
            Assert.assertNotNull(conversation.partner.id)
            Assert.assertNotNull(conversation.partner.avatar)
            Assert.assertNotNull(conversation.partner.role)
            Assert.assertNotNull(conversation.owner)
            Assert.assertNotNull(conversation.chat)
            Assert.assertNotNull(conversation.chat.identifier)
            Assert.assertNotNull(conversation.chat.partner)
            Assert.assertNotNull(conversation.chat.owner)

        }
    }
}