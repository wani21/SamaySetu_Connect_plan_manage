package com.College.timetable.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.email.from-name:SamaySetu Admin}")
    private String fromName;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        String verificationUrl = baseUrl + "/auth/verify-email?token=" + token;
        
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("SamaySetu - Email Verification");
            helper.setText("Dear Teacher,\n\n" +
                    "Thank you for registering with SamaySetu Timetable Management System.\n\n" +
                    "Please verify your email address by clicking the link below:\n" +
                    verificationUrl + "\n\n" +
                    "This link will expire in 24 hours.\n\n" +
                    "If you did not register for this account, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "SamaySetu Team\n" +
                    "MIT Academy of Engineering, Alandi(D), Pune");
            
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            // Fallback to simple message if MimeMessage fails
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SamaySetu - Email Verification");
            message.setText("Dear Teacher,\n\n" +
                    "Thank you for registering with SamaySetu Timetable Management System.\n\n" +
                    "Please verify your email address by clicking the link below:\n" +
                    verificationUrl + "\n\n" +
                    "This link will expire in 24 hours.\n\n" +
                    "If you did not register for this account, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "SamaySetu Team");
            mailSender.send(message);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetUrl = baseUrl + "/auth/reset-password?token=" + token;
        
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("SamaySetu - Password Reset Request");
            helper.setText("Dear Teacher,\n\n" +
                    "We received a request to reset your password.\n\n" +
                    "Please click the link below to reset your password:\n" +
                    resetUrl + "\n\n" +
                    "This link will expire in 1 hour.\n\n" +
                    "If you did not request a password reset, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "SamaySetu Team\n" +
                    "MIT Academy of Engineering, Alandi(D), Pune");
            
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            // Fallback to simple message
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SamaySetu - Password Reset Request");
            message.setText("Dear Teacher,\n\n" +
                    "We received a request to reset your password.\n\n" +
                    "Please click the link below to reset your password:\n" +
                    resetUrl + "\n\n" +
                    "This link will expire in 1 hour.\n\n" +
                    "If you did not request a password reset, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "SamaySetu Team");
            mailSender.send(message);
        }
    }

    public void sendWelcomeEmail(String toEmail, String name) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to SamaySetu!");
            helper.setText("Dear " + name + ",\n\n" +
                    "Your email has been successfully verified!\n\n" +
                    "You can now log in to SamaySetu Timetable Management System.\n\n" +
                    "Best regards,\n" +
                    "SamaySetu Team\n" +
                    "MIT Academy of Engineering, Alandi(D), Pune");
            
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            // Fallback to simple message
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to SamaySetu!");
            message.setText("Dear " + name + ",\n\n" +
                    "Your email has been successfully verified!\n\n" +
                    "You can now log in to SamaySetu Timetable Management System.\n\n" +
                    "Best regards,\n" +
                    "SamaySetu Team");
            mailSender.send(message);
        }
    }
}
