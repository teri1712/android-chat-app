package com.decade.practice.datalayer

import com.decade.practice.endpoints.eventCall
import com.decade.practice.model.domain.Chat
import com.decade.practice.model.domain.ChatEvent
import com.decade.practice.model.domain.ChatIdentifier
import com.decade.practice.model.domain.Conversation
import com.decade.practice.model.domain.ImageSpec
import com.decade.practice.model.domain.SeenEvent
import com.decade.practice.model.domain.TextEvent
import com.decade.practice.model.domain.User
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
import java.util.UUID
import javax.inject.Inject

fun testConversation(first: String, second: String): Conversation {
      val owner = User(UUID.nameUUIDFromBytes(first.toByteArray()).toString(), first, avatar = ImageSpec("", "", 100, 100))
      val partner = User(UUID.nameUUIDFromBytes(second.toByteArray()).toString(), second, avatar = ImageSpec("", "", 100, 100))
      val chat = Chat(ChatIdentifier.from(owner, partner), owner)
      return Conversation(chat, partner, owner)
}


fun testSeen(conversation: Conversation): ChatEvent {
      return ChatEvent(
            chatIdentifier = conversation.identifier,
            sender = conversation.partner.id,
            seenEvent = SeenEvent(System.currentTimeMillis())
      )
}


fun testText(conversation: Conversation, text: String): ChatEvent {
      return ChatEvent(
            chatIdentifier = conversation.identifier,
            sender = conversation.partner.id,
            textEvent = TextEvent(text)
      )
}

@HiltAndroidTest
class MessageTest {

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

      @Test
      fun testThirdMessageCount() = runTest {
            withContext(Dispatchers.Main) {
                  val conversation = testConversation("first", "third")

                  val session = accountRepository.currentSession!!
                  val messageRepo = session.messageRepoFactory.create(conversation)
                  val messageList = messageRepo.list()
                  Assert.assertEquals(2, messageList.size)
                  Assert.assertEquals(0, messageRepo.list(messageList.last().receiveTime).size)

            }
      }


      @Test
      fun testSecondMessageCount() = runTest {
            withContext(Dispatchers.Main) {
                  val conversation = testConversation("first", "second")

                  val session = accountRepository.currentSession!!
                  val messageRepo = session.messageRepoFactory.create(conversation)
                  Assert.assertEquals(messageRepo.list().size, 1)
            }
      }
}


@HiltAndroidTest
class MessageCountTest {

      @Inject
      lateinit var accountRepository: AccountRepository

      @Inject
      lateinit var retrofit: Retrofit

      @get:Rule
      val hiltRule = HiltAndroidRule(this)

      val secondToken =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6InNlY29uZCIsInBhc3N3b3JkX3ZlcnNpb24iOjAsImlkIjoiYTlmMGU2MWEtMTM3ZC0zNmFhLTlkYjUtMzQ2NWUwODAxNjEyIiwibmFtZSI6InNlY29uZCIsImlhdCI6MTc0NDA1NjUwOCwiZXhwIjoxNzQ0NjYxMzA4fQ.bApbO5A1pru_JvXtDGctmeCu1BQbQEw6bhqLFPAkofA"

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
      fun testCountThirdMessages() = runTest {
            withContext(Dispatchers.Main) {
                  val session = accountRepository.currentSession!!
                  val chatRepository = session.dialogRepository
                  chatRepository.list()

                  val conversation = testConversation("first", "third")
                  Assert.assertEquals(2, session.dialogRepository.get(conversation).messages.size)

            }
      }

      @Test
      fun testCountSecondMessagesAfterSend() {

            val conversation = testConversation("first", "second")
            runTest {
                  withContext(Dispatchers.Main) {
                        val session = accountRepository.currentSession!!
                        Assert.assertEquals("third", session.dialogRepository.list()[0].conversation.partner.username)
                        retrofit.eventCall().sendText("Bearer $secondToken", testText(conversation, "hello nami"))
                  }
            }
            Thread.sleep(1500)
            runTest {
                  withContext(Dispatchers.Main) {
                        val session = accountRepository.currentSession!!
                        val chatRepository = session.dialogRepository
                        val list = chatRepository.list()

                        Assert.assertEquals("second", list[0].conversation.partner.username)
                        val conversation = testConversation("first", "second")
                        Assert.assertEquals(2, session.dialogRepository.get(conversation).messages.size)
                        Assert.assertNotNull(session.dialogRepository.get(conversation).messages[0].textEvent)
                        Assert.assertNotNull("hello nami", session.dialogRepository.get(conversation).messages[0].textEvent?.content)

                  }
            }

      }
}


@HiltAndroidTest
class SeenMessageTest {

      private val secondToken =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6InNlY29uZCIsInBhc3N3b3JkX3ZlcnNpb24iOjAsImlkIjoiYTlmMGU2MWEtMTM3ZC0zNmFhLTlkYjUtMzQ2NWUwODAxNjEyIiwibmFtZSI6InNlY29uZCIsImlhdCI6MTc0NDEwNDk4NSwiZXhwIjoxNzQ0NzA5Nzg1fQ.ynWWkTwVpeTjWIeL0PFyeDL8xzKhC48__GRmn4blaAw"

      @Inject
      lateinit var accountRepository: AccountRepository

      @Inject
      lateinit var retrofit: Retrofit

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

      fun secondSendSeen() {
            val conversation = testConversation("first", "second")
            runTest {
                  withContext(Dispatchers.Main) {
                        retrofit.eventCall().sendSeen("Bearer $secondToken", testSeen(conversation))
                  }
            }
            Thread.sleep(2000)

            runTest {
                  val session = accountRepository.currentSession!!
                  val chatRepository = session.dialogRepository
                  var list = chatRepository.list()
                  Assert.assertEquals(list.size, 3)
                  Assert.assertEquals(list[0].conversation.partner.username, "third")
                  Assert.assertEquals(list[1].conversation.partner.username, "second")
                  Assert.assertEquals(list[2].conversation.partner.username, "admin")

                  val messageRepo = session.messageRepoFactory.create(conversation)
                  val messageList = messageRepo.list()
                  Assert.assertEquals(1, messageList.size)
                  Assert.assertNotNull(messageList[0].textEvent)
                  Assert.assertEquals(messageList[0].textEvent?.content, "Hello")

                  val eventRepo = session.eventRepoFactory.create(conversation)
                  val eventList = eventRepo.list()
                  Assert.assertEquals(2, eventList.size)
                  Assert.assertNotNull(eventList[0].seenEvent)
                  Assert.assertNotNull(eventList[0].seenEvent?.at)
                  Assert.assertEquals(eventList[0].eventVersion, 4)

                  Assert.assertNotNull(eventList[1].textEvent)
                  Assert.assertEquals("Hello", eventList[1].textEvent?.content)
                  Assert.assertEquals(eventList[1].eventVersion, 1)

                  list = chatRepository.list()
                  Assert.assertEquals(list.size, 3)
                  Assert.assertEquals(list[0].conversation.partner.username, "third")
                  Assert.assertEquals(list[1].conversation.partner.username, "second")
                  Assert.assertEquals(list[2].conversation.partner.username, "admin")

                  val persistentRepo = session.dialogRepository
                  Assert.assertEquals(persistentRepo.get(conversation).messages.size, 1)
                  Assert.assertTrue(persistentRepo.get(conversation).messages[0].seen)
                  Assert.assertEquals(UUID.nameUUIDFromBytes("first".toByteArray()).toString(), persistentRepo.get(conversation).messages[0].sender)

            }
      }

      fun ownerSendSeen() {
            val conversation = testConversation("first", "second")
            runTest {
                  withContext(Dispatchers.Main) {
                        val session = accountRepository.currentSession!!
                        val messageService = session.messageService
                        retrofit.eventCall().sendText("Bearer $secondToken", testText(conversation, "abc"))
                        messageService.seen(conversation)
                  }
            }
            Thread.sleep(3000)

            runTest {
                  val session = accountRepository.currentSession!!
                  val chatRepository = session.dialogRepository
                  var list = chatRepository.list()
                  Assert.assertEquals(list.size, 3)
                  Assert.assertEquals(list[0].conversation.partner.username, "second")
                  Assert.assertEquals(list[1].conversation.partner.username, "third")
                  Assert.assertEquals(list[2].conversation.partner.username, "admin")

                  val messageRepo = session.messageRepoFactory.create(conversation)
                  val messageList = messageRepo.list()
                  Assert.assertEquals(2, messageList.size)
                  Assert.assertNotNull(messageList[0].textEvent)
                  Assert.assertEquals(messageList[0].textEvent?.content, "abc")

                  val eventRepo = session.eventRepoFactory.create(conversation)
                  val eventList = eventRepo.list()
                  Assert.assertEquals(4, eventList.size)
                  Assert.assertNotNull(eventList[0].seenEvent)
                  Assert.assertNotNull(eventList[0].seenEvent?.at)
                  Assert.assertEquals(eventList[0].eventVersion, 6)

                  Assert.assertNotNull(eventList[1].textEvent)
                  Assert.assertEquals("abc", eventList[1].textEvent?.content)
                  Assert.assertEquals(eventList[1].eventVersion, 5)

                  list = chatRepository.list()
                  Assert.assertEquals(list.size, 3)
                  Assert.assertEquals(list[0].conversation.partner.username, "second")
                  Assert.assertEquals(list[1].conversation.partner.username, "third")
                  Assert.assertEquals(list[2].conversation.partner.username, "admin")

                  val persistentRepo = session.dialogRepository
                  Assert.assertEquals(persistentRepo.get(conversation).messages.size, 2)
                  Assert.assertTrue(persistentRepo.get(conversation).messages[0].seen)
                  Assert.assertEquals(UUID.nameUUIDFromBytes("second".toByteArray()).toString(), persistentRepo.get(conversation).messages[0].sender)
            }
      }

      @Test
      fun testThirdMessageCount() {
            secondSendSeen()
            ownerSendSeen()
      }

}
