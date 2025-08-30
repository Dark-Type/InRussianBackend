package com.inRussian.services.mailer

import at.favre.lib.crypto.bcrypt.BCrypt

interface PasswordHasher {
    fun hash(plain: String): String
    fun verify(plain: String, hash: String): Boolean
}

class BcryptPasswordHasher(private val cost: Int = 12) : PasswordHasher {
    override fun hash(plain: String): String =
        BCrypt.withDefaults().hashToString(cost, plain.toCharArray())

    override fun verify(plain: String, hash: String): Boolean =
        BCrypt.verifyer().verify(plain.toCharArray(), hash).verified
}