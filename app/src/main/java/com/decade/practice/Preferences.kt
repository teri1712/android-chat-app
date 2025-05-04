package com.decade.practice

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.decade.practice.model.domain.User
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton


const val SHARED_PREFERENCES_NAME = "APPLICATION_SHARED_PREFERENCES"
private const val ACCOUNT_KEY = "APPLICATION_CURRENT_ACCOUNT"

@Singleton
class PreferencesStore @Inject constructor(
      private val sharedPreferences: SharedPreferences,
      private val gson: Gson
) {
      var currentAccount: User?
            get() {
                  val userString = sharedPreferences.getString(ACCOUNT_KEY, null) ?: return null
                  return gson.fromJson(userString, User::class.java)
            }
            set(value) {
                  if (value != null) {
                        sharedPreferences.edit {
                              putString(ACCOUNT_KEY, gson.toJson(value))
                        }
                  } else {
                        sharedPreferences.edit {
                              remove(ACCOUNT_KEY)
                        }
                  }
            }
}

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

      @Provides
      @Singleton
      fun sharedPreferences(context: Application): SharedPreferences {
            val masterKey: MasterKey
            try {
                  masterKey = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                  return EncryptedSharedPreferences.create(
                        context,
                        SHARED_PREFERENCES_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                  )
            } catch (e: GeneralSecurityException) {
                  e.printStackTrace()
                  throw RuntimeException(e)
            } catch (e: IOException) {
                  e.printStackTrace()
                  throw RuntimeException(e)
            }
      }
}