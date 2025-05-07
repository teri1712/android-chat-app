package com.decade.practice.session.sync.work

import android.content.Context
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.decade.practice.cacheFile
import com.decade.practice.database.AccountDatabase
import com.decade.practice.database.dao.EventDao
import com.decade.practice.database.getAccount
import com.decade.practice.endpoints.eventCall
import com.decade.practice.model.domain.ChatEvent
import com.decade.practice.model.domain.ICON
import com.decade.practice.model.domain.IMAGE
import com.decade.practice.model.domain.SEEN
import com.decade.practice.model.domain.TEXT
import com.decade.practice.model.domain.User
import com.decade.practice.retrofit
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import java.io.IOException

const val WORK_GROUP_PREFIX = "MESSAGE_WORK_GROUP"
const val ACCOUNT_PARAM = "ACCOUNT_USERNAME"
const val NETWORK_EXCEPTION_FLAG = "NETWORK_EXCEPTION"
const val UNAUTHORIZED_EXCEPTION_CODE = "UNAUTHORIZED_EXCEPTION_CODE"

fun User.workTag(): String {
      return WORK_GROUP_PREFIX + "_" + username
}

class UploadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

      private val database: AccountDatabase = UploadWorker.database
      private val retrofit = context.retrofit()
      private val eventDao: EventDao = database.eventDao()
      private val gson = Gson()


      override suspend fun doWork(): Result {
            try {
                  val eventCall = retrofit.eventCall()
                  val credential = database.getAccount().credential
                  val tokenHeader = "Bearer " + credential.accessToken
                  var _event: ChatEvent = eventDao.findUnSent() ?: return Result.success()

                  withContext(Dispatchers.IO) {
                        when (_event.eventType) {
                              TEXT -> eventCall.sendText(tokenHeader, _event)
                              SEEN -> eventCall.sendSeen(tokenHeader, _event)
                              ICON -> eventCall.sendIcon(tokenHeader, _event)
                              IMAGE -> {
                                    // TODO: Handle Local URI/ Content Resolver
                                    val imageEvent = _event.imageEvent!!
                                    val file = applicationContext.cacheFile(imageEvent.uri.toUri())
                                    val requestFile = RequestBody.create(MultipartBody.FORM, file)
                                    val imagePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

                                    val json = gson.toJson(_event)
                                    val eventPart = RequestBody.create("application/json".toMediaType(), json)
                                    val result = eventCall.sendImage(tokenHeader, imagePart, eventPart)
                                    result
                              }

                              else -> {

                              }
                        }
                  }

            } catch (httpe: HttpException) {
                  httpe.printStackTrace()
                  if (httpe.code() == 401 || httpe.code() == 403) {
                        return Result.failure(
                              Data.Builder()
                                    .putInt(UNAUTHORIZED_EXCEPTION_CODE, httpe.code())
                                    .build()
                        )
                  }
                  throw httpe
            } catch (ioe: IOException) {
                  ioe.printStackTrace()
                  return Result.failure(
                        Data.Builder()
                              .putBoolean(NETWORK_EXCEPTION_FLAG, true)
                              .build()
                  )
            } catch (e: Exception) {
                  e.printStackTrace()
                  return Result.failure()
            }
            return Result.success()
      }

      companion object {

            @Volatile
            lateinit var database: AccountDatabase

      }
}

