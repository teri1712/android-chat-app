package com.decade.practice.net

import com.decade.practice.session.AccountComponent
import com.decade.practice.session.AccountLifecycle
import com.decade.practice.session.AccountScope
import com.decade.practice.authentication.AuthenticationException
import com.decade.practice.authentication.NoCredentialException
import com.decade.practice.authentication.UnAuthorizedEvent
import com.decade.practice.db.AccountDatabase
import com.decade.practice.db.dao.AccountDao
import com.decade.practice.event.ApplicationEventPublisher
import com.decade.practice.model.Credential
import com.decade.practice.model.User
import com.decade.practice.net.api.authenticationCall
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.IntoSet
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Route
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException
import java.net.HttpURLConnection
import javax.inject.Inject
import kotlin.concurrent.Volatile


private const val HEADER = "Authorization"
private const val BEARER = "Bearer "

@AccountScope
class AccountHttpContext @Inject constructor(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val account: User,
    httpClient: OkHttpClient,
    database: AccountDatabase,
    defaultRetrofit: Retrofit,
) : HttpContext, AccountLifecycle {

    @field:Volatile
    private var credential: Credential? = null
        get() {
            if (field == null) {
                synchronized(this@AccountHttpContext) {
                    if (field == null) {
                        field = accountDao.getBlocking().credential
                    }
                }
            }
            return field
        }

    private val accountDao: AccountDao = database.accountDao()

    override val client: OkHttpClient
    override val retrofit: Retrofit

    init {
        client = httpClient.newBuilder()

            .authenticator(RefreshAuthenticator())
            .addInterceptor(JwtInterceptor())
            .build()
        retrofit = defaultRetrofit.newBuilder().client(client)
            .build()
    }

    private fun onRefreshSuccess(credential: Credential) {
        accountDao.save(credential.accessToken, credential.expiresIn)
        this.credential = credential
    }

    @Synchronized
    private fun onRefreshFailed() {
        credential = null
        applicationEventPublisher.publish(UnAuthorizedEvent(account, 401))
    }

    override suspend fun onLogout() {
        synchronized(this) {
            credential = null
        }
    }

    @Synchronized
    @Throws(IOException::class)
    private fun execRefresh(): Credential {
        val refreshToken = credential?.refreshToken ?: throw NoCredentialException()
        try {
            val response = retrofit.authenticationCall().refresh(refreshToken)
            return response.also {
                onRefreshSuccess(it)
            }
        } catch (e: HttpException) {
            if (e.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw AuthenticationException()
            }
            throw e
        }
    }


    inner class JwtInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val _credential = credential ?: throw NoCredentialException()

            return chain.proceed(
                chain.request()
                    .newBuilder()
                    .addHeader(HEADER, BEARER + _credential.accessToken)
                    .build()
            )
        }
    }

    // RetryAndFollowUpInterceptor internally detect a HTTP_UNAUTHORIZED,
    // then invoke the Authenticator to retry the request,
    // implement refreshing logic here, if refreshing failed,
    // report to the Authentication Handler
    inner class RefreshAuthenticator : Authenticator {
        @Throws(IOException::class)
        override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
            val request = response.request
            try {
                execRefresh()
                return request.newBuilder()
                    .header(HEADER, BEARER + credential!!.accessToken)
                    .build()
            } catch (exception: Exception) {
                if (exception is AuthenticationException) {
                    onRefreshFailed()
                }
                throw exception
            }
        }
    }

}

@Module
@InstallIn(AccountComponent::class)
internal abstract class HttpContextModule {
    @Binds
    abstract fun httpContext(accountHttpContext: AccountHttpContext): HttpContext

    @Binds
    @IntoSet
    abstract fun accountLifecycle(accountHttpContext: AccountHttpContext): AccountLifecycle

}