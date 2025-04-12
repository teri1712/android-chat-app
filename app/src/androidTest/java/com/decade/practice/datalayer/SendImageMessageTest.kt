package com.decade.practice.datalayer

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.decade.practice.components.SHARED_PREFERENCES_NAME
import com.decade.practice.repository.AccountRepository
import com.decade.practice.view.composable.InputBar
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.time.Duration


@HiltAndroidTest
@FixMethodOrder
class SendImageMessageTest {

    @Inject
    lateinit var accountRepository: AccountRepository

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Volatile
    private lateinit var uri: Uri

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

    @Test
    fun sendImageMessage() {
        val conversation = testConversation("Luffy", "Nami")

        runTest {
            withContext(Dispatchers.Main) {
                val session = accountRepository.currentSession!!

                val list = session.conversationRepository.list()
                Assert.assertEquals(list.size, 3)
                Assert.assertEquals(list[0].partner.username, "Chopper")
                Assert.assertEquals(list[1].partner.username, "Nami")
                Assert.assertEquals(list[1].chat, conversation.chat)
                Assert.assertEquals(list[2].partner.username, "admin")
                session.messageService.send(conversation, uri)
            }
        }

        Thread.sleep(5000)

        runTest(timeout = Duration.INFINITE) {
            val session = accountRepository.currentSession!!
            val chatRepository = session.conversationRepository
            val list = chatRepository.list()
            Assert.assertEquals(list.size, 3)
            Assert.assertEquals(list[0].partner.username, "Nami")
            Assert.assertEquals(list[1].partner.username, "Chopper")
            Assert.assertEquals(list[2].partner.username, "admin")

            val messageRepo = session.messageRepoFactory.create(conversation)
            val messageList = messageRepo.list(Long.MAX_VALUE)
            Assert.assertNotNull(messageList[0].imageEvent)
            Assert.assertNull(messageList[1].imageEvent)
            Assert.assertEquals(2, messageList.size)
            Assert.assertTrue(messageList[0].imageEvent?.uri?.contains("filename=") ?: false)

            val eventRepo = session.eventRepository

            val eventList = eventRepo.list(Pair(conversation, Long.MAX_VALUE))
            Assert.assertEquals(2, eventList.size)
            Assert.assertNotNull(eventList[0].imageEvent)
            Assert.assertTrue(eventList[0].imageEvent?.uri?.contains("filename=") ?: false)
            Assert.assertEquals(4, eventList[0].eventVersion)
        }
    }


}

