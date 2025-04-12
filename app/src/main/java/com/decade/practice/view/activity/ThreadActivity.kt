package com.decade.practice.view.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.decade.practice.R
import com.decade.practice.accountRepository
import com.decade.practice.currentSession
import com.decade.practice.model.Conversation
import com.decade.practice.repository.AccountRepository
import com.decade.practice.view.composable.ChatInfoBar
import com.decade.practice.view.composable.ConversationList
import com.decade.practice.view.composable.InputBar
import com.decade.practice.view.composable.LocalConversation
import com.decade.practice.view.composable.LocalUser
import com.decade.practice.view.composable.MessageList
import com.decade.practice.view.composable.OnlineList
import com.decade.practice.view.composable.SearchField
import com.decade.practice.view.composable.Setting
import com.decade.practice.view.theme.ApplicationTheme
import com.decade.practice.view.viewmodel.MessageViewModel
import com.decade.practice.view.viewmodel.MessageViewModelFactory
import com.decade.practice.view.viewmodel.OnlineViewModel
import com.decade.practice.view.viewmodel.OnlineViewModelFactory
import com.decade.practice.view.viewmodel.ThreadViewModel
import com.decade.practice.view.viewmodel.ThreadViewModelFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

const val CONVERSATION_INTENT_DATA = "CONVERSATION"

@AndroidEntryPoint
class ThreadActivity : ComponentActivity() {

    @Inject
    lateinit var accountRepository: AccountRepository
    private var ready = false
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splashScreen.setKeepOnScreenCondition {
            return@setKeepOnScreenCondition accountRepository.hasSession
                    && accountRepository.currentSession?.ready == true && ready
        }
        if (!accountRepository.hasSession) {
            lifecycleScope.launch {
                val debugMode = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (!debugMode)
                    delay(3000)
                launchLoginActivity()
                ready = true
            }
        } else {
            setContent {
                navController = rememberNavController()
                ApplicationTheme {
                    CompositionLocalProvider(LocalUser provides accountRepository.currentSession!!.account) {
                        MainScreen(navController!!)
                    }
                }
            }
            navigate(intent)
        }
    }

    private fun navigate(intent: Intent?) {
        val conversation = intent?.getStringExtra(CONVERSATION_INTENT_DATA) ?: return
        navigate(conversation)
    }

    private fun navigate(conversation: String) {
        try {
            navigate(Json.decodeFromString<Conversation>(conversation))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun navigate(conversation: Conversation) {
        val route = ConversationRoute(conversation)
        try {
            val current = navController?.currentBackStackEntry?.toRoute<ConversationRoute>()
            if (current == route)
                return
        } catch (e: Exception) {
            e.printStackTrace()
        }
        navController?.navigate(route)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigate(intent)
    }
}

private fun Activity.launchLoginActivity() {
    val intent = Intent(this, LoginActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    startActivity(intent)
    finish()
}

@Serializable
object HomeRoute
data class HomeRouteItem(val route: Any, val icon: ImageVector, val name: String)

@Serializable
object ThreadRoute

@Serializable
object SettingRoute


@Serializable
data class ConversationRoute(
    val conversation: String,
) {
    constructor(conversation: Conversation) : this(Json.encodeToString(conversation))
}

@Composable
fun MainScreen(navController: NavHostController = rememberNavController()) {

    NavHost(
        navController = navController, startDestination = HomeRoute,
        enterTransition = {
            slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(700))
        },
        exitTransition = {
            slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(700))
        }, modifier = Modifier.systemBarsPadding()
    ) {
        composable<HomeRoute> {
            HomeScreen(navController)
        }
        composable<ConversationRoute> {
            val route: ConversationRoute = it.toRoute()
            val conversation = Json.decodeFromString<Conversation>(route.conversation)
            CompositionLocalProvider(LocalConversation provides conversation) {
                ConversationScreen(navController)
            }
        }
    }
}

@Composable
fun HomeScreen(parentNavController: NavController) {
    val navController = rememberNavController()
    val routes = listOf(
        HomeRouteItem(
            ThreadRoute, ImageVector.vectorResource(id = R.drawable.comments), "Chats"
        ),
        HomeRouteItem(SettingRoute, ImageVector.vectorResource(id = R.drawable.settings), "Settings")
    )
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .padding(top = 15.dp)
                    .height(65.dp)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                routes.forEach { topLevelRoute ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                topLevelRoute.icon,
                                contentDescription = topLevelRoute.name,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text(topLevelRoute.name) },
                        selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(topLevelRoute.route::class)
                        } == true,
                        onClick = {
                            navController.navigate(topLevelRoute.route)
                        },
                        colors = NavigationBarItemDefaults.colors().copy(
                            selectedIndicatorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ThreadRoute,
            modifier = Modifier
                .padding(innerPadding)
        ) {
            composable<ThreadRoute> {
                ThreadScreen(parentNavController)
            }
            composable<SettingRoute> {
                SettingScreen(parentNavController)
            }
        }
    }
}

@Composable
fun SettingScreen(navController: NavController) = Column(
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.spacedBy(20.dp),
    modifier = Modifier.padding(horizontal = 15.dp)
) {
    val context = LocalContext.current
    val accountRepository = remember { context.accountRepository() }
    val composeScope = rememberCoroutineScope()
    var loggingOut by remember { mutableStateOf(false) }
    if (loggingOut)
        InProgressDialog("Logging out")
    Text(
        text = "settings",
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold
        ),
    )
    Setting(onLogoutClick = {
        loggingOut = true
        composeScope.launch {
            accountRepository.logOut()
            delay(1500)
            (context as Activity).launchLoginActivity()
        }
    })
}

@Composable
fun ThreadScreen(navController: NavController) = Column(
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.padding(horizontal = 15.dp)
) {
    Text(
        text = "thread",
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold
        ),
    )
    OnlineScreen(navController)
    SearchField(value = "", onChange = {
        //TODO
    })
    val context = LocalContext.current
    val session = remember { context.currentSession() }
    val viewModel: ThreadViewModel = hiltViewModel(
        creationCallback = { factory: ThreadViewModelFactory ->
            factory.create(session.conversationRepository, session.persistentContext)
        }
    )
    val conversationList by viewModel.conversationFlow.collectAsStateWithLifecycle()
    val loading by viewModel.loadingFlow.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier
            .weight(1f)
    ) {
        ConversationList(navController, conversationList, loading) { viewModel.expand() }
    }
}

@Composable
fun OnlineScreen(navController: NavController) {

    val context = LocalContext.current
    val session = remember { context.currentSession() }
    val viewModel: OnlineViewModel = hiltViewModel(
        creationCallback = { factory: OnlineViewModelFactory ->
            factory.create(session.onlineRepository)
        }
    )
    viewModel.refresh()
    LaunchedEffect(Unit) {
        while (true) {
            delay(60 * 1000)
            viewModel.refresh()
        }
    }
    val onlineList by viewModel.onlineFlow.collectAsStateWithLifecycle()
    OnlineList(navController = navController, onlineList = onlineList)

}


@Composable
fun ConversationScreen(navController: NavController) = Column(
    modifier = Modifier
        .fillMaxSize()
) {
    val conversation = LocalConversation.current

    val context = LocalContext.current
    val session = context.currentSession()
    val messageService = session.messageService

    val viewModel: MessageViewModel = hiltViewModel(
        creationCallback = { factory: MessageViewModelFactory ->
            factory.create(conversation, session.messageRepoFactory, session.persistentContext)
        }
    )

    val listState: LazyListState = rememberLazyListState()
    val messageList by viewModel.itemFlow.collectAsState()
    val loading by viewModel.loadingFlow.collectAsState()
    val online by viewModel.onlineFlow.collectAsState()

    LaunchedEffect(messageList) {
        if (messageList.isNotEmpty()) {
            val first = messageList.first()
            if (conversation.partner.id == first.sender && first !== viewModel.ownerSeen)
                messageService.seen(conversation)
        }
    }

    val subscription = remember {
        session.onlineClient.subscribeChat(conversation.chat)
    }
    val typeEvent by subscription.eventFlow.collectAsStateWithLifecycle()
    var partnerTyping by remember { mutableStateOf(false) }
    var typing by remember { mutableLongStateOf(Long.MIN_VALUE) }

    val composeScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val typeTimeoutJob = composeScope.launch {
            while (true) {
                delay(500)
                partnerTyping = (System.currentTimeMillis() - typeEvent.time) < 2000
                if (System.currentTimeMillis() <= typing + 1000) {
                    subscription.ping()
                }
            }
        }
        onDispose {
            typeTimeoutJob.cancel()
            subscription.unSubscribe()
        }
    }
    ChatInfoBar(navController, online)
    MessageList(
        modifier = Modifier.weight(1f),
        listState = listState,
        messageList = messageList,
        typing = partnerTyping,
        loading = loading,
        endListAction = {
            viewModel.expand()
        }
    )
    InputBar(
        onSubmitText = { text: String ->
            messageService.send(conversation, text)
            composeScope.launch {
                listState.scrollToItem(0)
            }
        },
        onSubmitImage = { uri: Uri ->
            messageService.send(conversation, uri)
            composeScope.launch {
                listState.scrollToItem(0)
            }
        },
        onSubmitIcon = { resourceId ->
            messageService.send(conversation, resourceId)
            composeScope.launch {
                listState.scrollToItem(0)
            }
        },
        onTextChanged = { text ->
            if (text.isNotEmpty()) {
                typing = System.currentTimeMillis()
            }
        }
    )

}


//@Preview(showBackground = true, device = "id:pixel")
@Composable
fun MainScreenPreview() {
    ApplicationTheme {
        MainScreen()
    }
}

//@Preview(showBackground = true, device = "id:pixel")
@Composable
fun ConversationScreenPreview() {
    ApplicationTheme {
//        ConversationScreen(mockConversation())
    }
}