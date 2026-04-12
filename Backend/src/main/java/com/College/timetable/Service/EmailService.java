package com.College.timetable.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.from-name:SamaySetu Admin}")
    private String fromName;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

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
            // Log error for debugging
            logger.error("Failed to send verification email via MimeMessage to {}: {}", toEmail, e.getMessage());
            try {
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
            } catch (Exception fallbackError) {
                logger.error("Failed to send verification email via SimpleMailMessage to {}: {}", toEmail, fallbackError.getMessage());
            }
        }
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        // Point to frontend reset password page instead of backend endpoint
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("🔐 SamaySetu - Password Reset Request");
            
            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
                        <h1 style="color: white; margin: 0; font-size: 28px;">🔐 Password Reset Request</h1>
                        <p style="color: #f0f0f0; margin: 10px 0 0 0; font-size: 16px;">SamaySetu Timetable Management System</p>
                    </div>
                    
                    <div style="background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; border-radius: 0 0 10px 10px;">
                        <p style="font-size: 16px; margin-bottom: 20px;">Dear Teacher,</p>
                        
                        <p style="font-size: 16px; margin-bottom: 20px;">
                            We received a request to reset your password for your SamaySetu account. 
                            If you made this request, please click the button below to create a new password.
                        </p>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" 
                               style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
                                      color: white; 
                                      padding: 15px 30px; 
                                      text-decoration: none; 
                                      border-radius: 25px; 
                                      font-weight: bold; 
                                      font-size: 16px; 
                                      display: inline-block;
                                      box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);">
                                🔑 Reset My Password
                            </a>
                        </div>
                        
                        <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #ffc107;">
                            <p style="margin: 0; font-size: 14px; color: #856404;">
                                <strong>⚠️ Important Security Information:</strong><br>
                                • This link will expire in <strong>1 hour</strong> for your security<br>
                                • If you didn't request this reset, please ignore this email<br>
                                • Your password will remain unchanged until you create a new one
                            </p>
                        </div>
                        
                        <p style="font-size: 14px; color: #666; margin-bottom: 20px;">
                            If the button doesn't work, you can copy and paste this link into your browser:<br>
                            <a href="%s" style="color: #667eea; word-break: break-all;">%s</a>
                        </p>
                        
                        <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 25px 0;">
                        
                        <div style="text-align: center; color: #666; font-size: 14px;">
                            <p style="margin: 5px 0;"><strong>SamaySetu Team</strong></p>
                            <p style="margin: 5px 0;">MIT Academy of Engineering</p>
                            <p style="margin: 5px 0;">Alandi(D), Pune - 412105</p>
                            <p style="margin: 15px 0 5px 0; font-size: 12px; color: #999;">
                                This is an automated message. Please do not reply to this email.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(resetUrl, resetUrl, resetUrl);
            
            helper.setText(htmlContent, true); // true indicates HTML content
            
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            // Log error for debugging
            logger.error("Failed to send password reset email via MimeMessage to {}: {}", toEmail, e.getMessage());
            try {
                // Fallback to simple message
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(toEmail);
                message.setSubject("🔐 SamaySetu - Password Reset Request");
                message.setText("Dear Teacher,\n\n" +
                        "We received a request to reset your password for your SamaySetu account.\n\n" +
                        "Please click the link below to reset your password:\n" +
                        resetUrl + "\n\n" +
                        "⚠️ IMPORTANT:\n" +
                        "• This link will expire in 1 hour for your security\n" +
                        "• If you didn't request this reset, please ignore this email\n" +
                        "• Your password will remain unchanged until you create a new one\n\n" +
                        "If you have any issues, please contact the administrator.\n\n" +
                        "Best regards,\n" +
                        "SamaySetu Team\n" +
                        "MIT Academy of Engineering, Alandi(D), Pune\n\n" +
                        "This is an automated message. Please do not reply to this email.");
                mailSender.send(message);
            } catch (Exception fallbackError) {
                logger.error("Failed to send password reset email via SimpleMailMessage to {}: {}", toEmail, fallbackError.getMessage());
            }
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
                    "Your account is pending admin approval. Please wait for approval.\n\n" +
                    "Once approved, You can log in to SamaySetu Timetable Management System.\n\n" +
                    "Best regards,\n" +
                    "SamaySetu Team\n" +
                    "MIT Academy of Engineering, Alandi(D), Pune");

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            // Log error for debugging
            logger.error("Failed to send welcome email via MimeMessage to {}: {}", toEmail, e.getMessage());
            try {
                // Fallback to simple message
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(toEmail);
                message.setSubject("Welcome to SamaySetu!");
                message.setText("Dear " + name + ",\n\n" +
                        "Your email has been successfully verified!\n\n" +
                        "Your account is pending admin approval. Please wait for approval.\n\n" +
                        "Once approved, You can log in to SamaySetu Timetable Management System.\n\n" +
                        "Best regards,\n" +
                        "SamaySetu Team");
                mailSender.send(message);
            } catch (Exception fallbackError) {
                logger.error("Failed to send welcome email via SimpleMailMessage to {}: {}", toEmail, fallbackError.getMessage());
            }
        }
    }
    
    public void sendApprovalEmail(String toEmail, String name) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("SamaySetu - Account Approved!");
            helper.setText("Dear " + name + ",\n\n" +
                    "Great news! Your SamaySetu account has been approved by the administrator.\n\n" +
                    "You can now login and access the timetable management system:\n" +
                    frontendUrl + "/login\n\n" +
                    "Welcome to SamaySetu!\n\n" +
                    "Best regards,\n" +
                    "SamaySetu Team\n" +
                    "MIT Academy of Engineering, Alandi(D), Pune");

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            logger.error("Failed to send approval email to {}: {}", toEmail, e.getMessage());
        }
    }
    
    public void sendRejectionEmail(String toEmail, String name, String reason) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("SamaySetu - Account Application Status");
            helper.setText("Dear " + name + ",\n\n" +
                    "Thank you for your interest in SamaySetu Timetable Management System.\n\n" +
                    "Unfortunately, your account application has not been approved at this time.\n\n" +
                    "Reason: " + reason + "\n\n" +
                    "If you believe this is an error or have questions, please contact the administrator.\n\n" +
                    "Best regards,\n" +
                    "SamaySetu Team\n" +
                    "MIT Academy of Engineering, Alandi(D), Pune");

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            logger.error("Failed to send rejection email to {}: {}", toEmail, e.getMessage());
        }
    }
}
