package com.decade.practice.authentication

import com.decade.practice.event.ApplicationEvent
import com.decade.practice.model.User
import com.decade.practice.session.Session

abstract class AuthenticationEvent(val account: User?) : ApplicationEvent() {
    abstract val isAuthenticated: Boolean
}

class UnAuthorizedEvent(account: User, val code: Int) : AuthenticationEvent(account) {
    override val isAuthenticated: Boolean = false
}


class LoginEvent(val session: Session) : AuthenticationEvent(session.account) {
    override val isAuthenticated: Boolean = true
}

class LogoutEvent(val session: Session) : AuthenticationEvent(session.account) {
    override val isAuthenticated: Boolean = false
}
