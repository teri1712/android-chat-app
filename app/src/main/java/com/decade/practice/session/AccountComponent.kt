package com.decade.practice.session

import com.decade.practice.database.AccountDatabase
import com.decade.practice.model.domain.User
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.hilt.DefineComponent
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.MainScope


@AccountScope
@DefineComponent(parent = SingletonComponent::class)
interface AccountComponent

@DefineComponent.Builder
interface AccountComponentBuilder {

      fun account(@BindsInstance account: User): AccountComponentBuilder
      fun database(@BindsInstance database: AccountDatabase): AccountComponentBuilder

      fun build(): AccountComponent
}

fun AccountComponentBuilder.createSession(account: User, database: AccountDatabase) = account(account).database(database).build().getSession()


@EntryPoint
@InstallIn(AccountComponent::class)
internal interface AccountEntryPoint {
      fun session(): AccountSession
}

@Module
@InstallIn(AccountComponent::class)
internal object AccountModule {

      @AccountScope
      @Provides
      fun coroutineScope() = MainScope()
}


fun AccountComponent.getSession(): AccountSession {
      return EntryPoints.get(this, AccountEntryPoint::class.java).session()
}
