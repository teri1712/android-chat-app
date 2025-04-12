package com.decade.practice.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.decade.practice.session.AccountComponent
import com.decade.practice.session.AccountLifecycle
import com.decade.practice.session.AccountScope
import com.decade.practice.message.INBOUND_CHANNEL
import com.decade.practice.message.MessageQueue
import com.decade.practice.model.Chat
import com.decade.practice.model.ChatEvent
import com.decade.practice.model.TypeEvent
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.IntoSet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompCommand
import ua.naiksoftware.stomp.dto.StompHeader
import ua.naiksoftware.stomp.dto.StompMessage
import ua.naiksoftware.stomp.pathmatcher.SubscriptionPathMatcher
import javax.inject.Inject
import javax.inject.Named

@Module
@InstallIn(AccountComponent::class)
abstract class OnlineClientModule {
    @Binds
    @IntoSet
    abstract fun accountLifeCycle(onlineClient: OnlineClient): AccountLifecycle
}

interface ChatSubscription {
    val chat: Chat
    val eventFlow: StateFlow<TypeEvent>
    fun unSubscribe()
    fun ping()
}

@AccountScope
class OnlineClient @Inject constructor(
    context: Context,
    private val httpContext: HttpContext,
    @Named(INBOUND_CHANNEL)
    private val messageQueue: MessageQueue,
    private val gson: Gson
) : NetworkCallback(), AccountLifecycle {
    private var connManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var chatSubscription: ChatSubscriptionImpl? = null

    private var stompDisposable: CompositeDisposable? = null
    private var stompClient: StompClient? = null
    private val isOnline: Boolean
        get() = stompClient != null

    private var netAvailable: Boolean = true

    override fun onAvailable(network: Network) {
        val cap = connManager.getNetworkCapabilities(network)
        if (cap != null && cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            netAvailable = true
            if (!isOnline)
                connect()
        }
    }

    override fun onLost(network: Network) {
        netAvailable = false
    }

    override suspend fun onResume() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connManager.registerNetworkCallback(networkRequest, this)
    }

    override suspend fun onLogout() {
        connManager.unregisterNetworkCallback(this)
        disconnect()
    }

    private fun handleConnLost() {
        chatSubscription?.disposable = null
        stompClient = null
        stompDisposable = null
        if (netAvailable)
            connect()
    }

    private fun connect() {
        val stompClient = Stomp.over(
            Stomp.ConnectionProvider.OKHTTP,
            "ws://192.168.1.6:8080/handshake",
            null,
            httpContext.client
        )
        stompClient.setPathMatcher(SubscriptionPathMatcher(stompClient))
        val stompDisposable = CompositeDisposable()
        stompClient.withClientHeartbeat(5000)
            .withServerHeartbeat(5000)
            .connect()

        val connection = stompClient.lifecycle()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { lifecycleEvent: LifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> {
                    }

                    LifecycleEvent.Type.CLOSED, LifecycleEvent.Type.ERROR,
                    LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> handleConnLost()

                }
            }
        val eventQueue = stompClient.topic("/user/queue")
            .map { message: StompMessage -> gson.fromJson(message.payload, ChatEvent::class.java) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ chatEvent: ChatEvent ->
                messageQueue.enqueue(chatEvent)
            }, { throwable: Throwable -> this.handleError(throwable) })

        stompDisposable.add(connection)
        stompDisposable.add(eventQueue)

        this.stompClient = stompClient
        this.stompDisposable = stompDisposable

        chatSubscription?.connect()

    }

    private fun disconnect() {
        stompClient?.disconnect()
        stompDisposable?.dispose()
        chatSubscription?.unSubscribe()
        stompClient = null
        stompDisposable = null
        chatSubscription = null
    }

    private fun handleError(throwable: Throwable) {
        disconnect()
        throwable.printStackTrace()
    }

    fun subscribeChat(chat: Chat): ChatSubscription {
        chatSubscription?.unSubscribe()
        return ChatSubscriptionImpl(chat)
            .apply {
                chatSubscription = this
                connect()
            }
    }

    private inner class ChatSubscriptionImpl(override val chat: Chat) : ChatSubscription {
        var disposable: Disposable? = null
        val _eventFlow = MutableStateFlow(TypeEvent(chat))

        override val eventFlow: StateFlow<TypeEvent> = _eventFlow.asStateFlow()

        override fun unSubscribe() {
            disposable?.dispose()
            chatSubscription = null
        }

        override fun ping() {
            val message = StompMessage(
                StompCommand.SEND,
                listOf(StompHeader(StompHeader.DESTINATION, "/typing"), StompHeader("chat_identifier", chat.identifier.toString())),
                "Hello"
            )
            stompClient?.send(message)?.subscribe()
        }

        fun connect() {
            if (netAvailable && isOnline)
                disposable = stompClient!!.topic("/typing", listOf(StompHeader("chat_identifier", chat.identifier.toString())))
                    .map { message: StompMessage -> gson.fromJson(message.payload, TypeEvent::class.java) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ typeEvent: TypeEvent ->
                        if (chat.partner == typeEvent.from)
                            _eventFlow.value = typeEvent
                    }, { })
        }

    }
}





