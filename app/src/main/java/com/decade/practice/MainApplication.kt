package com.decade.practice

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import androidx.room.Room.databaseBuilder
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.work.Configuration
import com.decade.practice.components.JobDelay
import com.decade.practice.components.NetworkJobDelay
import com.decade.practice.components.SHARED_PREFERENCES_NAME
import com.decade.practice.db.AccountDatabase
import com.decade.practice.event.ApplicationEventPublisher
import com.decade.practice.repository.AccountRepository
import com.decade.practice.session.AccountManager
import com.decade.practice.session.Session
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

const val SERVER: String = "http://192.168.1.6:8080/"

@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        val debugMode = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debugMode) {
            getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit()
            databaseList().forEach { dbName ->
                deleteDatabase(dbName)
            }
        }
    }

    @Inject
    lateinit var executorService: ExecutorService

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setExecutor(executorService)
            .build()

}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MainEntryPoint {

    fun accountManager(): AccountManager
    fun applicationEventPublisher(): ApplicationEventPublisher
    fun retrofit(): Retrofit
    fun sqlLiteFactory(): SupportSQLiteOpenHelper.Factory

}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ApplicationModule {

    @Binds
    abstract fun context(application: Application): Context

    @Binds
    abstract fun executor(executorService: ExecutorService): Executor

    @Binds
    abstract fun jobCache(jobCache: NetworkJobDelay): JobDelay

    companion object {

        @Provides
        @Singleton
        fun executorService(): ExecutorService {
            return ThreadPoolExecutor(
                3,
                max(3.0, (Runtime.getRuntime().availableProcessors() - 1).toDouble()).toInt(),
                10, TimeUnit.SECONDS,
                LinkedBlockingQueue()
            )
        }

        @Singleton
        @Provides
        fun gson() = Gson()

        @Provides
        @Singleton
        fun httpClient(executor: ExecutorService, context: Context): OkHttpClient {
            return OkHttpClient.Builder().dispatcher(
                Dispatcher(executor)
            )
                .cache(Cache(File(context.cacheDir, "http"), 50L * 1024 * 1024))
                .build()
        }

        @Provides
        @Singleton
        fun defaultRetrofit(httpClient: OkHttpClient): Retrofit {
            return Retrofit.Builder()
                .baseUrl(SERVER)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build()
        }

    }
}

fun Context.createDatabase(account: String): AccountDatabase {
    val openFactory = EntryPoints.get(this.applicationContext, MainEntryPoint::class.java).sqlLiteFactory()
    return databaseBuilder(
        this.applicationContext,
        AccountDatabase::class.java, "database_$account"
    ).openHelperFactory(openFactory).build()
}

fun Context.applicationEventPublisher(): ApplicationEventPublisher {
    return EntryPoints.get(this.applicationContext, MainEntryPoint::class.java).applicationEventPublisher()
}

fun Context.retrofit(): Retrofit {
    return EntryPoints.get(this.applicationContext, MainEntryPoint::class.java).retrofit()
}

fun Context.accountRepository(): AccountRepository {
    return EntryPoints.get(this.applicationContext, MainEntryPoint::class.java).accountManager()
}

fun Context.currentSession(): Session {
    return EntryPoints.get(this.applicationContext, MainEntryPoint::class.java).accountManager().currentSession!!
}

fun Context.cacheFile(uri: Uri): File {
    val cacheDir = File(cacheDir, "temp")
    if (!cacheDir.exists()) {
        cacheDir.mkdir()
    }

    val tempFile = File(cacheDir, UUID.randomUUID().toString())

    contentResolver.openInputStream(uri)?.use { fis ->
        FileOutputStream(tempFile).use { fos ->
            fis.copyTo(fos)
        }
    } ?: throw IOException("Can't read the Uri: $uri")

    return tempFile
}
