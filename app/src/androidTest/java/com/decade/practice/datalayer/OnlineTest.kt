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
class OnlineTest {

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
    fun testOnline() = runTest {
        withContext(Dispatchers.Main) {
            val onlineRepo = accountRepository.currentSession!!.onlineRepository
            val list = onlineRepo.list()
            Assert.assertTrue(list.isNotEmpty())
            list.forEach {
                Assert.assertNotNull(it.user)
                Assert.assertNotNull(it.user.username)
                Assert.assertNotNull(it.user.id)
                Assert.assertNotNull(it.user.name)
                Assert.assertNotNull(it.user.role)
                Assert.assertNotNull(it.user.gender)
                Assert.assertNotNull(it.user.avatar)
                Assert.assertNotNull(it.user.avatar.width)
                Assert.assertNotNull(it.user.avatar.height)
            }
        }
    }

}