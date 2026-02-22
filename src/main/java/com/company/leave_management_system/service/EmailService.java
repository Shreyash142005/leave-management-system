package com.company.leave_management_system.service;

import com.company.leave_management_system.entity.EmailLog;
import com.company.leave_management_system.entity.LeaveRequest;
import com.company.leave_management_system.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.mock:false}")
    private boolean mockEmail;

    // ==================== LEAVE REQUEST EMAILS ====================

    @Async
    public void sendLeaveAppliedEmail(LeaveRequest leave) {
        String subject = "Leave Application Submitted ✓";
        String body = buildLeaveAppliedBody(leave);
        sendHtmlEmail(leave.getEmployee().getEmail(), subject, body);
    }

    @Async
    public void sendLeaveApprovedEmail(LeaveRequest leave) {
        String subject = "Leave Request Approved ✅";
        String body = buildLeaveApprovedBody(leave);
        sendHtmlEmail(leave.getEmployee().getEmail(), subject, body);
    }

    @Async
    public void sendLeaveRejectedEmail(LeaveRequest leave) {
        String subject = "Leave Request Rejected ❌";
        String body = buildLeaveRejectedBody(leave);
        sendHtmlEmail(leave.getEmployee().getEmail(), subject, body);
    }

    @Async
    public void sendLeaveCancelledEmail(LeaveRequest leave) {
        String subject = "Leave Request Cancelled";
        String body = buildLeaveCancelledBody(leave);
        sendHtmlEmail(leave.getEmployee().getEmail(), subject, body);
    }

    // ==================== REGISTRATION & APPROVAL EMAILS ====================

    @Async
    public void sendWelcomeEmail(String to, String name, String username, String role) {
        String subject = "Welcome to Leave Management System";
        String body = buildWelcomeEmailBody(name, username, role);
        sendHtmlEmail(to, subject, body);
    }

    @Async
    public void sendManagerApprovalNotification(String to, String managerName, String managerEmail) {
        String subject = "New Manager Registration - Approval Required";
        String body = buildManagerApprovalEmailBody(managerName, managerEmail);
        sendHtmlEmail(to, subject, body);
    }

    @Async
    public void sendManagerApprovedEmail(String to, String managerName, String approvedBy) {
        String subject = "Manager Account Approved ✅";
        String body = buildManagerApprovedEmailBody(managerName, approvedBy);
        sendHtmlEmail(to, subject, body);
    }

    // ==================== EMAIL SENDING ====================

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            if (mockEmail) {
                log.info("📧 [MOCK MODE] Email would be sent:");
                log.info("   To: {}", to);
                log.info("   Subject: {}", subject);
                logEmail(to, subject, htmlBody, "MOCK", null);
            } else {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                // Set from address with friendly name
                helper.setFrom(fromEmail, "Leave Management System");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);

                mailSender.send(message);
                logEmail(to, subject, htmlBody, "SUCCESS", null);
                log.info("✅ Email sent successfully to: {}", to);
            }
        } catch (MessagingException e) {
            log.error("❌ Failed to send email to: {}", to, e);
            logEmail(to, subject, htmlBody, "FAILED", e.getMessage());
        } catch (Exception e) {
            log.error("❌ Failed to send email to: {}", to, e);
            logEmail(to, subject, htmlBody, "FAILED", e.getMessage());
        }
    }

    private void logEmail(String recipient, String subject, String body, String status, String errorMessage) {
        try {
            EmailLog emailLog = new EmailLog();
            emailLog.setRecipient(recipient);
            emailLog.setSubject(subject);
            emailLog.setBody(body);
            emailLog.setStatus(status);
            emailLog.setErrorMessage(errorMessage);
            emailLogRepository.save(emailLog);
        } catch (Exception e) {
            log.error("Failed to log email: {}", e.getMessage());
        }
    }

    // ==================== EMAIL TEMPLATES ====================

    private String buildLeaveAppliedBody(LeaveRequest leave) {
        String autoApprovalNote = leave.getStatus().name().equals("APPROVED")
                ? "<p style='background: #d4edda; color: #155724; padding: 15px; border-radius: 8px; border-left: 4px solid #28a745;'><strong>✅ Great news!</strong> Your leave has been automatically approved based on company policy.</p>"
                : "<p style='background: #fff3cd; color: #856404; padding: 15px; border-radius: 8px; border-left: 4px solid #ffc107;'><strong>⏳ Pending Approval:</strong> Your leave request is waiting for admin/manager approval.</p>";

        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                        ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                        ".header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                        ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                        ".info-box { background: white; padding: 20px; margin: 20px 0; border-left: 4px solid #667eea; border-radius: 5px; }" +
                        ".footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }" +
                        "h1 { margin: 0; font-size: 28px; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='container'>" +
                        "<div class='header'><h1>📝 Leave Application Submitted</h1></div>" +
                        "<div class='content'>" +
                        "<p>Dear %s,</p>" +
                        "<p>Your leave application has been submitted successfully.</p>" +
                        "<div class='info-box'>" +
                        "<strong>📅 Leave Details:</strong><br>" +
                        "<strong>Period:</strong> %s to %s<br>" +
                        "<strong>Duration:</strong> %.1f working days<br>" +
                        "<strong>Type:</strong> %s<br>" +
                        "<strong>Reason:</strong> %s<br>" +
                        "<strong>Status:</strong> %s<br>" +
                        "</div>" +
                        "%s" +
                        "<p>Best regards,<br><strong>Leave Management Team</strong></p>" +
                        "</div>" +
                        "<div class='footer'><p>This is an automated email. Please do not reply.</p></div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                leave.getEmployee().getName(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getWorkingDays(),
                leave.getDuration(),
                leave.getReason(),
                leave.getStatus(),
                autoApprovalNote
        );
    }

    private String buildLeaveApprovedBody(LeaveRequest leave) {
        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                        ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                        ".header { background: linear-gradient(135deg, #11998e 0%%, #38ef7d 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                        ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                        ".info-box { background: white; padding: 20px; margin: 20px 0; border-left: 4px solid #38ef7d; border-radius: 5px; }" +
                        ".footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }" +
                        "h1 { margin: 0; font-size: 28px; }" +
                        ".success { color: #38ef7d; font-size: 48px; text-align: center; margin: 10px 0; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='container'>" +
                        "<div class='header'>" +
                        "<div class='success'>✅</div>" +
                        "<h1>Leave Request Approved</h1>" +
                        "</div>" +
                        "<div class='content'>" +
                        "<p>Dear %s,</p>" +
                        "<p>Good news! Your leave request has been <strong>approved</strong>.</p>" +
                        "<div class='info-box'>" +
                        "<strong>📅 Leave Details:</strong><br>" +
                        "<strong>Period:</strong> %s to %s<br>" +
                        "<strong>Duration:</strong> %.1f working days<br>" +
                        "<strong>Approved by:</strong> %s<br>" +
                        "</div>" +
                        "<p>Enjoy your time off! 🌴</p>" +
                        "<p>Best regards,<br><strong>Leave Management Team</strong></p>" +
                        "</div>" +
                        "<div class='footer'><p>This is an automated email. Please do not reply.</p></div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                leave.getEmployee().getName(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getWorkingDays(),
                leave.getProcessedBy() != null ? leave.getProcessedBy().getUsername() : "System"
        );
    }

    private String buildLeaveRejectedBody(LeaveRequest leave) {
        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                        ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                        ".header { background: linear-gradient(135deg, #eb3349 0%%, #f45c43 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                        ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                        ".info-box { background: white; padding: 20px; margin: 20px 0; border-left: 4px solid #f45c43; border-radius: 5px; }" +
                        ".footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }" +
                        "h1 { margin: 0; font-size: 28px; }" +
                        ".reject { color: #f45c43; font-size: 48px; text-align: center; margin: 10px 0; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='container'>" +
                        "<div class='header'>" +
                        "<div class='reject'>❌</div>" +
                        "<h1>Leave Request Rejected</h1>" +
                        "</div>" +
                        "<div class='content'>" +
                        "<p>Dear %s,</p>" +
                        "<p>We regret to inform you that your leave request has been <strong>rejected</strong>.</p>" +
                        "<div class='info-box'>" +
                        "<strong>📅 Leave Details:</strong><br>" +
                        "<strong>Period:</strong> %s to %s<br>" +
                        "<strong>Duration:</strong> %.1f working days<br>" +
                        "<strong>Rejected by:</strong> %s<br>" +
                        "</div>" +
                        "<p>Your leave balance has been restored. For more information, please contact your manager or HR department.</p>" +
                        "<p>Best regards,<br><strong>Leave Management Team</strong></p>" +
                        "</div>" +
                        "<div class='footer'><p>This is an automated email. Please do not reply.</p></div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                leave.getEmployee().getName(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getWorkingDays(),
                leave.getProcessedBy() != null ? leave.getProcessedBy().getUsername() : "System"
        );
    }

    private String buildLeaveCancelledBody(LeaveRequest leave) {
        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                        ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                        ".header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                        ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                        ".info-box { background: white; padding: 20px; margin: 20px 0; border-left: 4px solid #667eea; border-radius: 5px; }" +
                        ".footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }" +
                        "h1 { margin: 0; font-size: 28px; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='container'>" +
                        "<div class='header'><h1>🔄 Leave Request Cancelled</h1></div>" +
                        "<div class='content'>" +
                        "<p>Dear %s,</p>" +
                        "<p>Your leave application has been <strong>cancelled</strong> as per your request.</p>" +
                        "<div class='info-box'>" +
                        "<strong>📅 Cancelled Leave Details:</strong><br>" +
                        "<strong>Period:</strong> %s to %s<br>" +
                        "<strong>Duration:</strong> %.1f working days<br>" +
                        "</div>" +
                        "<p>✅ The leave balance has been restored to your account.</p>" +
                        "<p>Best regards,<br><strong>Leave Management Team</strong></p>" +
                        "</div>" +
                        "<div class='footer'><p>This is an automated email. Please do not reply.</p></div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                leave.getEmployee().getName(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getWorkingDays()
        );
    }

    private String buildWelcomeEmailBody(String name, String username, String role) {
        String additionalInfo = role.equals("MANAGER")
                ? "<p><strong>⚠️ Important:</strong> Your manager account requires admin approval before you can approve/reject leave requests. You will receive another email once your account is approved.</p>"
                : "<p>You can now log in using your credentials at: <a href='http://localhost:8080'>http://localhost:8080</a></p>";

        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                        ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                        ".header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                        ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                        ".info-box { background: white; padding: 20px; margin: 20px 0; border-left: 4px solid #667eea; border-radius: 5px; }" +
                        ".footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }" +
                        "h1 { margin: 0; font-size: 28px; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='container'>" +
                        "<div class='header'><h1>🏢 Welcome to Leave Management System</h1></div>" +
                        "<div class='content'>" +
                        "<h2>Hello %s! 👋</h2>" +
                        "<p>Your account has been successfully created. You can now log in and start managing your leaves.</p>" +
                        "<div class='info-box'>" +
                        "<strong>📋 Your Account Details:</strong><br>" +
                        "<strong>Username:</strong> %s<br>" +
                        "<strong>Role:</strong> %s<br>" +
                        "</div>" +
                        "%s" +
                        "<p>If you have any questions, please contact the HR department.</p>" +
                        "<p>Best regards,<br><strong>Leave Management System</strong></p>" +
                        "</div>" +
                        "<div class='footer'><p>This is an automated email. Please do not reply.</p></div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                name,
                username,
                role,
                additionalInfo
        );
    }

    private String buildManagerApprovalEmailBody(String managerName, String managerEmail) {
        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                        ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                        ".header { background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                        ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                        ".info-box { background: white; padding: 20px; margin: 20px 0; border-left: 4px solid #f5576c; border-radius: 5px; }" +
                        ".footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }" +
                        "h1 { margin: 0; font-size: 28px; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='container'>" +
                        "<div class='header'><h1>⚠️ New Manager Registration</h1></div>" +
                        "<div class='content'>" +
                        "<p>Dear Admin,</p>" +
                        "<p>A new manager has registered and requires your approval.</p>" +
                        "<div class='info-box'>" +
                        "<strong>👤 Manager Details:</strong><br>" +
                        "<strong>Name:</strong> %s<br>" +
                        "<strong>Email:</strong> %s<br>" +
                        "</div>" +
                        "<p>Please log in to the admin dashboard to approve or reject this manager account.</p>" +
                        "<p>Best regards,<br><strong>Leave Management System</strong></p>" +
                        "</div>" +
                        "<div class='footer'><p>This is an automated email. Please do not reply.</p></div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                managerName,
                managerEmail
        );
    }

    private String buildManagerApprovedEmailBody(String managerName, String approvedBy) {
        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                        ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                        ".header { background: linear-gradient(135deg, #11998e 0%%, #38ef7d 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                        ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                        ".info-box { background: white; padding: 20px; margin: 20px 0; border-left: 4px solid #38ef7d; border-radius: 5px; }" +
                        ".footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }" +
                        "h1 { margin: 0; font-size: 28px; }" +
                        ".success { color: #38ef7d; font-size: 48px; text-align: center; margin: 10px 0; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='container'>" +
                        "<div class='header'>" +
                        "<div class='success'>✅</div>" +
                        "<h1>Manager Account Approved</h1>" +
                        "</div>" +
                        "<div class='content'>" +
                        "<p>Dear %s,</p>" +
                        "<p>Congratulations! Your manager account has been approved.</p>" +
                        "<div class='info-box'>" +
                        "<strong>✅ What you can do now:</strong><br>" +
                        "• Approve or reject employee leave requests<br>" +
                        "• View all leave requests in your department<br>" +
                        "• Manage holidays<br>" +
                        "• Access full manager dashboard<br>" +
                        "</div>" +
                        "<p><strong>Approved by:</strong> %s</p>" +
                        "<p>Best regards,<br><strong>Leave Management Team</strong></p>" +
                        "</div>" +
                        "<div class='footer'><p>This is an automated email. Please do not reply.</p></div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                managerName,
                approvedBy
        );
    }
}