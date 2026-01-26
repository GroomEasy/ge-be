package com.ceos.menual.domain.email.service;

import com.ceos.menual.domain.email.exception.EmailErrorCode;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully via SMTP. to={}, subject={}", to, subject);

        } catch (Exception e) {
            log.error("Failed to send email via SMTP. to={}, error={}", to, e.getMessage(), e);
            throw new GlobalException(EmailErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
