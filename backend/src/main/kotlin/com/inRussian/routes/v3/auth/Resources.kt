package com.inRussian.routes.v3.auth

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("/auth")
class AuthResource {
    @Serializable
    @Resource("student/register")
    class StudentRegister(val parent: AuthResource = AuthResource())

    @Serializable
    @Resource("staff/register")
    class StaffRegister(val parent: AuthResource = AuthResource())

    @Serializable
    @Resource("login")
    class Login(val parent: AuthResource = AuthResource())

    @Serializable
    @Resource("refresh")
    class Refresh(val parent: AuthResource = AuthResource())

    @Serializable
    @Resource("logout")
    class Logout(val parent: AuthResource = AuthResource())

    @Serializable
    @Resource("me")
    class Me(val parent: AuthResource = AuthResource())

    @Serializable
    @Resource("admin/create-initial")
    class CreateInitialAdmin(val parent: AuthResource = AuthResource())
}
