package cn.wyq.serverwebsocket.utils;

import cn.wyq.serverwebsocket.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
public class MailUtil {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 🌟 破局关键：自己注入自己（配合 @Lazy 延迟加载防止循环依赖），用于调用自身的 @Async 方法
    @Autowired
    @Lazy
    private MailUtil self;

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

        // 4. 真正耗时的发邮件动作，通过代理对象交给子线程异步去干！极速响应前端。
        self.executeSendMailAsync(toEmail, code);
    }

    /**
     * 内部实际发送邮件的方法（完全异步，不阻塞主线程）
     */
    @Async
    public void executeSendMailAsync(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【你的聊天系统】账户验证码");
            message.setText("尊敬的用户您好，您的验证码是：" + code + "，有效期为 5 分钟。请勿将此验证码泄露给他人。");

            mailSender.send(message);
        } catch (Exception e) {
            // 异步线程中的异常只能打印日志记录，不能抛给用户
            System.err.println("邮件发送失败给：" + toEmail + "，原因：" + e.getMessage());
        }
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