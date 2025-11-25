package com.massager.app.domain.model

// 文件说明：统一表示认证流程的成功与失败结果。
sealed interface AuthResult {
    data class LoginSuccess(val user: User) : AuthResult
    data class RegisterSuccess(val user: User) : AuthResult
    data class Error(val message: String) : AuthResult
    data object LoggedOut : AuthResult
}
