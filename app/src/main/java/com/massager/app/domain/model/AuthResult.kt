package com.massager.app.domain.model

sealed interface AuthResult {
    data class LoginSuccess(val user: User) : AuthResult
    data class RegisterSuccess(val user: User) : AuthResult
    data class Error(val message: String) : AuthResult
    data object LoggedOut : AuthResult
}
