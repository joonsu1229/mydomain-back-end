package com.domainreg.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Properties;

/**
 * SMTP 설정을 관리자 페이지(DB, {@link AppSettingsService})에서 읽어 이메일을 발송한다.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final StringRedisTemplate redis;
    private final AppSettingsService settings;
    private final String webBaseUrl;

    public EmailService(StringRedisTemplate redis, AppSettingsService settings,
                        @Value("${app.web-base-url:https://mydomain.rog.kr}") String webBaseUrl) {
        this.redis = redis;
        this.settings = settings;
        this.webBaseUrl = webBaseUrl;
    }

    public void sendPasswordResetEmail(String email, String name, String token) {
        String resetUrl = webBaseUrl + "/#/reset-password?token=" + token;
        String subject = "[마이도메인] 비밀번호 재설정 안내";

        String body = String.format("""
            <div style="max-width:480px;margin:0 auto;font-family:sans-serif;">
              <h2 style="color:#1a1a1a;">안녕하세요, %s님</h2>
              <p style="color:#555;line-height:1.6;">
                비밀번호 재설정 요청이 있었습니다.<br/>
                아래 버튼을 클릭하여 새 비밀번호를 설정하세요:
              </p>
              <a href="%s"
                 style="display:inline-block;background:#1a1a1a;color:#fff;padding:12px 32px;
                        border-radius:8px;text-decoration:none;font-weight:bold;margin:16px 0;">
                비밀번호 재설정하기
              </a>
              <p style="color:#999;font-size:12px;">
                이 링크는 15분 동안 유효합니다.<br/>
                비밀번호 재설정을 요청하지 않으셨다면 이 이메일을 무시하세요.
              </p>
            </div>
            """, name, resetUrl);

        // Store reset token in Redis (15 min TTL)
        redis.opsForValue().set("reset_token:" + token, email, Duration.ofMinutes(15));

        try {
            JavaMailSenderImpl sender = buildMailSender();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(body, true);
            helper.setFrom(fromAddress());
            sender.send(message);
            log.info("Password reset email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", email, e.getMessage());
            log.info("Password reset link (fallback): {}", resetUrl);
        }
    }

    public void sendVerificationEmail(String email, String name, String token) {
        String verifyUrl = webBaseUrl + "/#/verify?token=" + token;
        String subject = "[마이도메인] 이메일 인증을 완료해주세요";

        String body = String.format("""
            <div style="max-width:480px;margin:0 auto;font-family:sans-serif;">
              <h2 style="color:#1a1a1a;">안녕하세요, %s님! 👋</h2>
              <p style="color:#555;line-height:1.6;">
                마이도메인 회원가입을 위한 <strong>이메일 인증</strong>입니다.<br/>
                아래 버튼을 클릭하면 인증이 완료됩니다:
              </p>
              <a href="%s"
                 style="display:inline-block;background:#1a1a1a;color:#fff;padding:12px 32px;
                        border-radius:8px;text-decoration:none;font-weight:bold;margin:16px 0;">
                이메일 인증하기
              </a>
              <p style="color:#999;font-size:12px;">
                이 링크는 24시간 동안 유효합니다.
              </p>
            </div>
            """, name, verifyUrl);

        // Store verification token in Redis for dev retrieval (24h TTL)
        redis.opsForValue().set("verify_token:" + token, email, Duration.ofHours(24));

        try {
            JavaMailSenderImpl sender = buildMailSender();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML
            helper.setFrom(fromAddress());

            sender.send(message);
            log.info("Verification email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", email, e.getMessage());
            // Fallback: log the link so user can still verify via Redis
            log.info("Verification link (fallback): {}", verifyUrl);
        }
    }

    private JavaMailSenderImpl buildMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.getOrDefault("smtp.host", "smtp.naver.com"));
        String port = settings.getOrDefault("smtp.port", "587");
        try {
            sender.setPort(Integer.parseInt(port.trim()));
        } catch (NumberFormatException e) {
            sender.setPort(587);
        }
        sender.setUsername(settings.getOrDefault("smtp.username", ""));
        sender.setPassword(settings.getOrDefault("smtp.password", ""));
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        return sender;
    }

    private String fromAddress() {
        String from = settings.getOrDefault("smtp.from", "");
        return from.isBlank() ? "no-reply@mydomain.rog.kr" : from;
    }
}
