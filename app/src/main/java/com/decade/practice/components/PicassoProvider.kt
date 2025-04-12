package com.decade.practice.components

import android.content.Context
import com.decade.practice.authentication.AuthenticationEvent
import com.decade.practice.authentication.LoginEvent
import com.decade.practice.authentication.LogoutEvent
import com.decade.practice.event.ApplicationEvent
import com.decade.practice.event.ApplicationEventListener
import com.decade.practice.net.HttpContext
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Cache
import java.io.File
import java.util.concurrent.ExecutorService
import javax.inject.Inject
import javax.inject.Singleton


interface PicassoProvider {
    fun get(): Picasso
}

@Singleton
class PicassoInitializer @Inject constructor(
    private val context: Context,
    private val executor: ExecutorService
) : ApplicationEventListener, PicassoProvider {

    private val cache = File(context.cacheDir, "images")
    private lateinit var picasso: Picasso

    init {
        if (!cache.exists() && !cache.mkdir()) {
            throw RuntimeException("Can't create cache directory for Picasso")
        }
    }

    override fun get(): Picasso = picasso

    override fun supportsEventType(eventType: Class<out ApplicationEvent>): Boolean {
        return AuthenticationEvent::class.java.isAssignableFrom(eventType)
    }

    override suspend fun onApplicationEvent(applicationEvent: ApplicationEvent) {
        if (applicationEvent is LoginEvent) {
            val httpContext: HttpContext = applicationEvent.session.httpContext
            val sharedPool = httpContext.client.newBuilder()
                .cache(Cache(cache, (100 * 1024 * 1024).toLong()))
                .build()
            picasso = Picasso.Builder(context)
                .executor(executor)
                .downloader(OkHttp3Downloader(sharedPool))
                .build()
        } else if (applicationEvent is LogoutEvent) {
            picasso = Picasso.get()
        }
    }

}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PicassoModule {

    @Binds
    abstract fun picassoProvider(picasso: PicassoInitializer): PicassoProvider

    @Binds
    @IntoSet
    abstract fun applicationEventListener(picasso: PicassoInitializer): ApplicationEventListener

}
