package com.inRussian.services.mailer

interface Mailer {
    fun send(to: String, subject: String, text: String, html: String? = null)
}