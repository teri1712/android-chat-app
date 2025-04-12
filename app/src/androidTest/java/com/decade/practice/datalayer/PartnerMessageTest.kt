package com.decade.practice.datalayer


import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.decade.practice.components.SHARED_PREFERENCES_NAME
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
class PartnerMessageTest {

    @Inject
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var retrofit: Retrofit

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val thirdToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6InRoaXJkIiwicGFzc3dvcmRfdmVyc2lvbiI6MCwiaWQiOiJkZDVjOGJmNS0xNTU4LTNmY2ItYTUwMC03MDcxOTA4ZTk1MjQiLCJuYW1lIjoidGhpcmQiLCJpYXQiOjE3NDM4Nzc3ODgsImV4cCI6MTc0NDQ4MjU4OH0.UZ_2-YuPoN360sW28dpE06-pvBGiv8mj0wiZqm35bsw"
    private val secondToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6InNlY29uZCIsInBhc3N3b3JkX3ZlcnNpb24iOjAsImlkIjoiYTlmMGU2MWEtMTM3ZC0zNmFhLTlkYjUtMzQ2NWUwODAxNjEyIiwibmFtZSI6InNlY29uZCIsImlhdCI6MTc0NDEwNDk4NSwiZXhwIjoxNzQ0NzA5Nzg1fQ.ynWWkTwVpeTjWIeL0PFyeDL8xzKhC48__GRmn4blaAw"

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


    fun thirdSendText() {
        val conversation = testConversation("first", "third")
        runTest {
            withContext(Dispatchers.Main) {
                val session = accountRepository.currentSession!!
                Assert.assertEquals(session.conversationRepository.list()[0].partner.username, "third")
                retrofit.eventCall().sendText("Bearer " + thirdToken, testText(conversation, "ekkk"))
            }
        }
        Thread.sleep(1500)

        runTest {
            val session = accountRepository.currentSession!!
            val chatRepository = session.conversationRepository
            val list = chatRepository.list()
            Assert.assertEquals(list.size, 3)
            Assert.assertEquals(list[0].partner.username, "third")
            Assert.assertEquals(list[1].partner.username, "second")
            Assert.assertEquals(list[2].partner.username, "admin")

            val messageRepo = session.messageRepoFactory.create(conversation)
            val messageList = messageRepo.list()
            Assert.assertEquals(3, messageList.size)
            Assert.assertNotNull(messageList[0].textEvent)
            Assert.assertEquals(messageList[0].textEvent?.content, "ekkk")
            val eventRepo = session.eventRepository

            val eventList = eventRepo.list()
            Assert.assertEquals(3, eventList.size)
            Assert.assertNotNull(eventList[0].textEvent)
            Assert.assertEquals(eventList[0].textEvent?.content, "ekkk")
            Assert.assertEquals(eventList[0].eventVersion, 4)
        }
    }

    fun secondSendText() {
        val conversation = testConversation("first", "second")
        runTest {
            withContext(Dispatchers.Main) {
                val session = accountRepository.currentSession!!
                Assert.assertEquals("third", session.conversationRepository.list()[0].partner.username)
                retrofit.eventCall().sendText("Bearer " + secondToken, testText(conversation, "wtfff"))
            }
        }
        Thread.sleep(1500)

        runTest {
            val session = accountRepository.currentSession!!
            val chatRepository = session.conversationRepository
            val list = chatRepository.list()
            Assert.assertEquals(list.size, 3)
            Assert.assertEquals(list[0].partner.username, "second")
            Assert.assertEquals(list[1].partner.username, "third")
            Assert.assertEquals(list[2].partner.username, "admin")

            val messageRepo = session.messageRepoFactory.create(conversation)
            val messageList = messageRepo.list()
            Assert.assertEquals(2, messageList.size)
            Assert.assertNotNull(messageList[0].textEvent)
            Assert.assertEquals(messageList[0].textEvent?.content, "wtfff")
            val eventRepo = session.eventRepository

            val eventList = eventRepo.list()
            Assert.assertEquals(2, eventList.size)
            Assert.assertNotNull(eventList[0].textEvent)
            Assert.assertEquals(eventList[0].textEvent?.content, "wtfff")
            Assert.assertEquals(eventList[0].eventVersion, 5)
        }
    }

    fun thirdResendText() {
        val conversation = testConversation("first", "third")
        runTest {
            withContext(Dispatchers.Main) {
                val session = accountRepository.currentSession!!
                Assert.assertEquals(session.conversationRepository.list()[0].partner.username, "second")
                retrofit.eventCall().sendText("Bearer " + thirdToken, testText(conversation, "ekkk"))
            }
        }
        Thread.sleep(1500)

        runTest {
            val session = accountRepository.currentSession!!
            val chatRepository = session.conversationRepository
            val list = chatRepository.list()
            Assert.assertEquals(list.size, 3)
            Assert.assertEquals(list[0].partner.username, "third")
            Assert.assertEquals(list[1].partner.username, "second")
            Assert.assertEquals(list[2].partner.username, "admin")

            val messageRepo = session.messageRepoFactory.create(conversation)
            val messageList = messageRepo.list()
            Assert.assertEquals(4, messageList.size)
            Assert.assertNotNull(messageList[0].textEvent)
            Assert.assertEquals(messageList[0].textEvent?.content, "ekkk")
            val eventRepo = session.eventRepository

            val eventList = eventRepo.list()
            Assert.assertEquals(4, eventList.size)
            Assert.assertNotNull(eventList[0].textEvent)
            Assert.assertEquals(eventList[0].textEvent?.content, "ekkk")
            Assert.assertEquals(eventList[0].eventVersion, 6)
        }
    }

    fun thirdSendSeen() {
        val conversation = testConversation("first", "third")
        runTest {
            withContext(Dispatchers.Main) {
                val session = accountRepository.currentSession!!
                Assert.assertEquals(session.conversationRepository.list()[0].partner.username, "third")
                retrofit.eventCall().sendSeen("Bearer " + thirdToken, testSeen(conversation))
            }
        }
        Thread.sleep(1500)

        runTest {
            val session = accountRepository.currentSession!!
            val chatRepository = session.conversationRepository
            var list = chatRepository.list()
            Assert.assertEquals(list.size, 3)
            Assert.assertEquals(list[0].partner.username, "third")
            Assert.assertEquals(list[1].partner.username, "second")
            Assert.assertEquals(list[2].partner.username, "admin")

            val messageRepo = session.messageRepoFactory.create(conversation)
            var messageList = messageRepo.list()
            Assert.assertEquals(4, messageList.size)
            Assert.assertNotNull(messageList[0].textEvent)
            Assert.assertEquals(messageList[0].textEvent?.content, "ekkk")
            val eventRepo = session.eventRepository

            val eventList = eventRepo.list()
            Assert.assertEquals(5, eventList.size)
            Assert.assertNotNull(eventList[0].seenEvent)
            Assert.assertNotNull(eventList[0].seenEvent?.at)
            Assert.assertEquals(eventList[0].eventVersion, 7)

            Assert.assertNotNull(eventList[1].textEvent)
            Assert.assertEquals("ekkk", eventList[1].textEvent?.content)
            Assert.assertEquals(eventList[1].eventVersion, 6)

            list = chatRepository.list()
            Assert.assertEquals(list.size, 3)
            Assert.assertEquals(list[0].partner.username, "third")
            Assert.assertEquals(list[1].partner.username, "second")
            Assert.assertEquals(list[2].partner.username, "admin")
        }
    }

    @Test
    fun testSendTextBoth() {
        thirdSendText()
        secondSendText()
        thirdResendText()
        thirdSendSeen()
    }
}

