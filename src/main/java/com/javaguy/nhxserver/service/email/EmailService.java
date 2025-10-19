package com.javaguy.nhxserver.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender emailSender;
    private final TemplateEngine templateEngine;

    @Value("${nhx.app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetEmail(String to, String token) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        Context context = new Context();
        context.setVariable("resetUrl", baseUrl + "/api/v1/auth/reset-password?token=" + token);

        String htmlContent = templateEngine.process("password-reset-email", context);

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Password Reset Request");
        helper.setText(htmlContent, true);

        emailSender.send(message);
    }
}
