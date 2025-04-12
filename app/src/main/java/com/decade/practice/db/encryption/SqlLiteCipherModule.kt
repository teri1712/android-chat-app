package com.decade.practice.db.encryption

import android.content.SharedPreferences
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.util.UUID
import javax.inject.Singleton

private const val PASSPHRASE = "SQL_LITE_CIPHER_PASSPHRASE"

@Module
@InstallIn(SingletonComponent::class)
object SqlLiteCipherModule {

    @Provides
    @Singleton
    fun sqlLiteFactory(encryptedSP: SharedPreferences): SupportSQLiteOpenHelper.Factory {
        var passphrase = encryptedSP.getString(PASSPHRASE, null)

        if (passphrase == null) {
            passphrase = UUID.randomUUID().toString()
            encryptedSP.edit().putString(PASSPHRASE, passphrase).apply()
        }

        val bytes: ByteArray = SQLiteDatabase.getBytes(passphrase.toCharArray())

        return SupportFactory(bytes)
    }
}