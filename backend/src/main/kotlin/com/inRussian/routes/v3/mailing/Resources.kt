package com.inRussian.routes.v3.mailing
import kotlinx.serialization.Serializable
import io.ktor.resources.Resource

@Serializable
@Resource("/password/recovery")
class PasswordRecoveryResource {
    @Serializable
    @Resource("request")
    class Request(val parent: PasswordRecoveryResource = PasswordRecoveryResource())

    @Serializable
    @Resource("check")
    class Check(val parent: PasswordRecoveryResource = PasswordRecoveryResource())

    @Serializable
    @Resource("reset")
    class Reset(val parent: PasswordRecoveryResource = PasswordRecoveryResource())
}

@Serializable
@Resource("/email")
class EmailVerificationResource {
    @Serializable
    @Resource("verify")
    class Verify(val parent: EmailVerificationResource = EmailVerificationResource())

    @Serializable
    @Resource("resend")
    class Resend(val parent: EmailVerificationResource = EmailVerificationResource())
}