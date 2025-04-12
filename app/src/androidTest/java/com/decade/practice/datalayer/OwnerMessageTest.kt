package com.decade.practice.datalayer

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
import javax.inject.Inject

@HiltAndroidTest
class OwnerMessageTest {

    @Inject
    lateinit var accountRepository: AccountRepository

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


    fun sendTextToThird() {
        val conversation = testConversation("first", "third")
        runTest {
            withContext(Dispatchers.Main) {
                val session = accountRepository.currentSession!!
                Assert.assertEquals(session.conversationRepository.list()[0].partner.username, "third")
                session.messageService.send(conversation, "ekkk")
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

    fun sendTextToSecond() {
        val conversation = testConversation("first", "second")
        runTest {
            withContext(Dispatchers.Main) {
                val session = accountRepository.currentSession!!
                Assert.assertEquals("third", session.conversationRepository.list()[0].partner.username)
                session.messageService.send(conversation, "wtfff")
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

    fun sendSecondTextToThird() {
        val conversation = testConversation("first", "third")
        runTest {
            withContext(Dispatchers.Main) {
                val session = accountRepository.currentSession!!
                Assert.assertEquals(session.conversationRepository.list()[0].partner.username, "second")
                session.messageService.send(conversation, "ekkk")
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

    fun sendSeenToThird() {
        val conversation = testConversation("first", "third")
        runTest {
            withContext(Dispatchers.Main) {
                val session = accountRepository.currentSession!!
                Assert.assertEquals(session.conversationRepository.list()[0].partner.username, "third")
                session.messageService.seen(conversation)
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
        sendTextToThird()
        sendTextToSecond()
        sendSecondTextToThird()
        sendSeenToThird()
    }
}

