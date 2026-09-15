package cn.wyq.serverwebsocket.utils;

import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.messaging.MailMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * MailUtil：通用工具类，封装可复用的辅助方法。
 */
@Component
public class MailUtil {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // 🌟 必须区分两个不同的 Key 前缀！
    private static final String CODE_PREFIX = "VERIFY_CODE:";   // 用于存真实验证码
    private static final String COOLDOWN_PREFIX = "MAIL_COOLDOWN:"; // 用于存 60 秒冷却锁

    /**
     * 对外暴露的发送方法（这是同步方法，运行在主线程）
     */
    public void sendVerificationCode(String toEmail) {

        // 1. 同步校验：防恶刷。抛出的异常可以直接被 Controller 的全局异常处理器捕获，返回给前端！
        String cooldownKey = COOLDOWN_PREFIX + toEmail;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new ServiceException("操作太频繁，请 60 秒后再试！", 400);
        }

        // 2. 生成 6 位随机纯数字验证码
        String code = String.valueOf(new Random().nextInt(899999) + 100000);

        // 3. 存入 Redis（验证码和冷却锁各自独立，互不干扰）
        redisTemplate.opsForValue().set(CODE_PREFIX + toEmail, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(cooldownKey, "locked", 60, TimeUnit.SECONDS);

        // 4. 邮件进入登录模块事件，由 Web 模块投递到 RabbitMQ 后异步发送。
        eventPublisher.publishEvent(MailMessage.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .toEmail(toEmail)
                .subject("【你的聊天系统】账户验证码")
                .content("尊敬的用户您好，您的验证码是：" + code + "，有效期为 5 分钟。请勿将此验证码泄露给他人。")
                .templateType("VERIFICATION_CODE")
                .build());
    }

    /**
     * 内部实际发送邮件的方法（完全异步，不阻塞主线程）
     */
    @Async
    public void executeSendMailAsync(String toEmail, String code) {
        sendMailAsync(toEmail, "【你的聊天系统】账户验证码",
                "尊敬的用户您好，您的验证码是：" + code + "，有效期为 5 分钟。请勿将此验证码泄露给他人。");
    }

    /** RabbitMQ 不可用时的异步兜底发信。 */
    @Async
    public void sendMailAsync(String toEmail, String subject, String content) {
        sendMailNow(toEmail, subject, content);
    }

    /** RabbitMQ 消费者调用的同步发信方法。 */
    public void sendMailNow(String toEmail, String subject, String content) {
        try {
            sendMailNowOrThrow(toEmail, subject, content);
        } catch (Exception e) {
            // 异步线程中的异常只能打印日志记录，不能抛给用户
            System.err.println("邮件发送失败给：" + toEmail + "，原因：" + e.getMessage());
        }
    }

    /**
     * RabbitMQ 消费者使用的严格发信方法。
     *
     * <p>发送失败必须抛出异常，交给 RabbitMQ listener 的重回队列策略处理，
     * 不能像普通异步兜底方法一样吞掉异常。</p>
     */
    public void sendMailNowOrThrow(String toEmail, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    /**
     * 校验验证码的方法
     */
    public boolean verifyCode(String email, String inputCode) {
        String storedCode = redisTemplate.opsForValue().get(CODE_PREFIX + email);

        if (storedCode != null && storedCode.equals(inputCode)) {
            redisTemplate.delete(CODE_PREFIX + email);
            return true;
        }
        return false;
    }
}
