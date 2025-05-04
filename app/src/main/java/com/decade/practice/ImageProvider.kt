package com.decade.practice

import android.content.Context
import coil.Coil
import coil.ImageLoader
import com.decade.practice.authentication.AuthenticationEvent
import com.decade.practice.authentication.LoginEvent
import com.decade.practice.authentication.LogoutEvent
import com.decade.practice.endpoints.HttpContext
import com.decade.practice.endpoints.imageCall
import com.decade.practice.event.ApplicationEvent
import com.decade.practice.event.ApplicationEventListener
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton


interface ImageProvider {
      suspend fun load(filename: String): InputStream
      val loader: ImageLoader
}

@Singleton
class ImageInitializer @Inject constructor(
      private val context: Context,
      private val defaultClient: OkHttpClient,
      private val defaultRetrofit: Retrofit
) : ApplicationEventListener, ImageProvider {

      private val cache = File(context.cacheDir, "images")
      private val defaultLoader = ImageLoader.Builder(context)
            .okHttpClient { defaultClient }
            .build()
      private var imageLoader = defaultLoader
      private var retrofit: Retrofit = defaultRetrofit

      init {
            if (!cache.exists() && !cache.mkdir()) {
                  throw RuntimeException("Can't create cache directory for Picasso")
            }
      }

      override suspend fun load(filename: String): InputStream = retrofit.imageCall().download(filename).byteStream()

      override val loader: ImageLoader
            get() = imageLoader

      override fun supportsEventType(eventType: Class<out ApplicationEvent>): Boolean {
            return AuthenticationEvent::class.java.isAssignableFrom(eventType)
      }

      override suspend fun onApplicationEvent(applicationEvent: ApplicationEvent) {
            if (applicationEvent is LoginEvent) {
                  val httpContext: HttpContext = applicationEvent.session.httpContext
                  val httpClient = httpContext.client.newBuilder()
                        .cache(Cache(cache, (100 * 1024 * 1024).toLong()))
                        .build()
                  imageLoader = ImageLoader.Builder(context)
                        .okHttpClient { httpClient }
                        .build()
                  retrofit = httpContext.retrofit
            } else if (applicationEvent is LogoutEvent) {
                  imageLoader = defaultLoader
                  retrofit = defaultRetrofit
            }
            Coil.setImageLoader(imageLoader)
      }

}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PicassoModule {

      @Binds
      abstract fun picassoProvider(picasso: ImageInitializer): ImageProvider

      @Binds
      @IntoSet
      abstract fun applicationEventListener(picasso: ImageInitializer): ApplicationEventListener

}
