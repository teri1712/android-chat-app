package com.decade.practice

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.decade.practice.activity.CONVERSATION_INTENT_DATA
import com.decade.practice.activity.ThreadActivity
import com.decade.practice.event.ApplicationEvent
import com.decade.practice.event.ApplicationEventListener
import com.decade.practice.model.domain.ChatEvent
import com.decade.practice.model.domain.Conversation
import com.decade.practice.model.domain.isMessage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton


private const val CHANNEL_ID = "decade_channel_id"
private const val CHANNEL_NAME = "Decade Messaging"
private const val CHANNEL_DESCRIPTION = "This channel used for posting incoming messages"
private const val NOTIFICATION_ID = 666


@Singleton
class NotificationService @Inject constructor(
      private val applicationContext: Application,
      private val imageProvider: ImageProvider,
) : ApplicationEventListener {

      private var isOnForeGround: Boolean = false

      private val mainScope = MainScope()
      private fun Bitmap.crop(): Bitmap {
            val w = width
            val h = height
            val cropped = createBitmap(w, h)
            val canvas = Canvas(cropped)
            val path = Path()
            path.addCircle(w / 2.0f, h / 2.0f, w / 2.0f, Path.Direction.CW)
            canvas.clipPath(path)
            canvas.drawBitmap(this, 0.0f, 0.0f, null)
            return cropped
      }

      init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                  val importance = NotificationManager.IMPORTANCE_HIGH
                  val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                        description = CHANNEL_DESCRIPTION
                  }
                  // Register the channel with the system.
                  val notificationManager: NotificationManager =
                        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                  notificationManager.createNotificationChannel(channel)
            }
            applicationContext.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                  }

                  override fun onActivityStarted(activity: Activity) {
                        if (activity is ThreadActivity)
                              isOnForeGround = true
                  }

                  override fun onActivityResumed(activity: Activity) {
                  }

                  override fun onActivityPaused(activity: Activity) {
                  }

                  override fun onActivityStopped(activity: Activity) {
                        if (activity is ThreadActivity)
                              isOnForeGround = false
                  }

                  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                  }

                  override fun onActivityDestroyed(activity: Activity) {
                  }
            })
      }

      private fun tapAction(conversation: Conversation): PendingIntent {
            val intent = Intent(applicationContext, ThreadActivity::class.java).apply {
                  putExtra(CONVERSATION_INTENT_DATA, Json.encodeToString(conversation))
            }
            return PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
      }

      override fun supportsEventType(eventType: Class<out ApplicationEvent>): Boolean {
            return eventType.isAssignableFrom(ChatEvent::class.java)
      }

      override suspend fun onApplicationEvent(applicationEvent: ApplicationEvent) {
            val event = applicationEvent as ChatEvent
            if (isOnForeGround || !event.isMessage() || event.owner.id == event.sender)
                  return
            mainScope.launch {
                  val sender = event.partner
                  val uri = sender.avatar.uri.toUri()
                  val imageRequest = ImageRequest.Builder(applicationContext)
                        .data(uri)
                        .build()

                  val message = event.textEvent?.content ?: (sender.name + "has sent you an image")
                  val imageLoader = imageProvider.loader
                  val avatar = withContext(Dispatchers.IO) {
                        val result = imageLoader.execute(imageRequest)
                        if (result !is SuccessResult)
                              return@withContext null

                        val drawable = result.drawable
                        if (drawable is BitmapDrawable)
                              return@withContext drawable.bitmap
                        val width = drawable.intrinsicWidth
                        val height = drawable.intrinsicHeight

                        val bitmap = createBitmap(width, height)
                        val canvas = Canvas(bitmap)

                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)

                        return@withContext bitmap

                  }
                  val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                        .setSmallIcon(R.drawable.message_icon)
                        .setLargeIcon(avatar?.crop())
                        .setContentTitle(sender.name)
                        .setContentText(message)
                        .setContentIntent(tapAction(event.conversation))
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setAutoCancel(true)

                  with(NotificationManagerCompat.from(applicationContext)) {
                        if (ActivityCompat.checkSelfPermission(
                                    applicationContext,
                                    Manifest.permission.POST_NOTIFICATIONS
                              ) != PackageManager.PERMISSION_GRANTED
                        ) {
                              return@with
                        }
                        // notificationId is a unique int for each notification that you must define.
                        notify(NOTIFICATION_ID, builder.build())
                  }
            }
      }

}


@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationModule {

      @Binds
      @IntoSet
      abstract fun applicationEventListener(notificationService: NotificationService): ApplicationEventListener

}
