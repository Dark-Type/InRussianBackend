package com.inRussian.services.mailer

import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import java.util.Properties

class GmailMailer(
    private val host: String = "smtp.gmail.com",
    private val port: Int = 587,
    private val username: String,
    private val appPassword: String,
    private val from: String? = null,
    private val useTls: Boolean = true
) : Mailer {

    private val sender: String = from ?: username

    private val session: Session by lazy {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", useTls.toString())
            put("mail.smtp.host", host)
            put("mail.smtp.port", port.toString())
        }
        Session.getInstance(props, object : jakarta.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, appPassword)
            }
        })
    }

    override fun send(to: String, subject: String, text: String, html: String?) {
        try {
            val message = MimeMessage(session).apply {
                val senderEmail = (getSender() as? InternetAddress)?.address
                setFrom(senderEmail)
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                setSubject(subject)
                if (html.isNullOrBlank()) {
                    setText(text)
                } else {
                    val multipart = MimeMultipart("alternative")
                    MimeBodyPart().apply {
                        setText(text, "utf-8")
                        multipart.addBodyPart(this)
                    }
                    MimeBodyPart().apply {
                        setContent(html, "text/html; charset=utf-8")
                        multipart.addBodyPart(this)
                    }
                    setContent(multipart)
                }
            }
            Transport.send(message)
        } catch (e: MessagingException) {
            throw RuntimeException("Failed to send email", e)
        }
    }
}