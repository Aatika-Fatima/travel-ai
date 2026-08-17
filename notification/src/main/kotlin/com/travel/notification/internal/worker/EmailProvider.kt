package com.travel.notification.internal.worker

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

interface EmailProvider {
    fun send(to: String, subject: String, body: String)
}

@Component
class SmtpEmailProvider(private val mailSender: JavaMailSender) : EmailProvider {
    override fun send(to: String, subject: String, body: String) {
        val message = SimpleMailMessage()
        message.setTo(to)
        message.subject = subject
        message.text = body
        mailSender.send(message)  // routes through Mailhog:1025 locally per G10's spring.mail config
    }
}