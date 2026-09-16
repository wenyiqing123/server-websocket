package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.annotation.LoginNotRequired;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.config.AlipayConfig;
import cn.wyq.serverwebsocket.mapper.RechargeOrderMapper;
import cn.wyq.serverwebsocket.pojo.entity.RechargeOrder;
import cn.wyq.serverwebsocket.pojo.vo.AlipayPaymentStatusVO;
import cn.wyq.serverwebsocket.service.AccountService;
import cn.wyq.serverwebsocket.service.RechargeService;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.utils.BaseContext;
import cn.wyq.serverwebsocket.utils.CacheClient;
import cn.wyq.serverwebsocket.utils.SystemConfigUtils;
import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * AlipayController：Web 层控制器，负责接收并处理 HTTP 请求。
 */
@Slf4j
@RestController
public class AlipayController {

    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private RechargeService rechargeService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AlipayConfig alipayConfig;
    @Autowired
    private RechargeOrderMapper orderMapper;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private SysConfigService sysConfigService;
    /** 支付宝异步回调地址，只能由部署环境配置。 */
    @Value("${alipay.notify-url:}")
    private String notifyUrl;

    @PostMapping("/alipay/cancel/{outTradeNo}")
    public Result cancelOrder(@PathVariable String outTradeNo) {
        Integer userId = BaseContext.getCurrentId().intValue();
        RechargeOrder order = rechargeService.findOrderForUser(outTradeNo, userId);
        if (order == null) {
            return Result.error(404, "充值订单不存在");
        }
        if (!Integer.valueOf(0).equals(order.getStatus())) {
            return Result.error("订单已处理，不能重复取消");
        }

        // 直接在数据库中将订单状态改为 2 (已关闭/超时)
        // 注意：只更新状态为 0 (待支付) 的订单，防止误操作
        int updated = orderMapper.updateOrderStatusToClosed(outTradeNo);
        if (updated > 0) {
            // 订单状态和管理端财务列表都可能已被缓存，关闭订单后必须立即失效。
            cacheClient.evict("finance:recharge-status", outTradeNo);
            cacheClient.evictByPrefix("finance");
            return Result.success("订单已主动关闭");
        }
        return Result.error("订单取消失败或订单状态已改变");
    }

    /**
     * 1. 前端请求生成二维码
     */
    @GetMapping("/alipay/precreate")
    public Result precreate(@RequestParam String amount, @RequestParam String outTradeNo) throws Exception {
        if (!StringUtils.hasText(notifyUrl)) {
            return Result.error("未配置支付宝异步回调地址");
        }
        Integer userId = BaseContext.getCurrentId().intValue();

        // 1. 先在本地数据库创建待支付订单
        rechargeService.createOrder(outTradeNo, userId, new BigDecimal(amount));

        // 2. 构造支付宝请求
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();

        // 🌟 优化：使用变量读取回调地址
        request.setNotifyUrl(notifyUrl);

        // 🌟 优化：使用 JSONObject 构建参数，避免字符串拼接容易出错的双引号转义问题
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", amount);
        bizContent.put("subject", "聊天系统余额充值");
        // 支付宝侧超时与 RabbitMQ 分段消息使用同一配置，避免外部订单先过期而本地仍可支付。
        bizContent.put("timeout_express", rechargeTimeoutExpress());
        request.setBizContent(bizContent.toJSONString());

        AlipayTradePrecreateResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            return Result.success(response.getQrCode());
        } else {
            return Result.error("调用失败: " + response.getSubMsg());
        }
    }

    /**
     * 2. 支付宝异步回调通知接口
     */
    @LoginNotRequired
    @PostMapping("/alipay/notify")
    public String notify(HttpServletRequest request) {
        log.info("============== 收到支付宝异步通知 ==============");
        try {
            // 1. 获取支付宝 POST 过来反馈信息
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String name : requestParams.keySet()) {
                String[] values = requestParams.get(name);
                String valueStr = String.join(",", values);
                params.put(name, valueStr);
            }

            // 2. 调用 SDK 验证签名
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );

            if (signVerified) {
                // 🌟 🚨 核心安全优化：校验 app_id 是否是你自己的！！！
                String appId = params.get("app_id");
                if (!alipayConfig.getAppId().equals(appId)) {
                    log.error("警告！收到未知 AppId 的回调，可能存在伪造请求！收到的AppId: {}", appId);
                    return "failure";
                }

                // 3. 验证通过，判断交易状态
                String tradeStatus = params.get("trade_status");
                // 🌟 支付宝除了 TRADE_SUCCESS，某些不可退款交易还会发送 TRADE_FINISHED
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    String outTradeNo = params.get("out_trade_no");
                    String alipayTradeNo = params.get("trade_no");
                    String totalAmount = params.get("total_amount");

                    log.info("验证成功！订单号: {}, 金额: {}", outTradeNo, totalAmount);

                    // 🌟 提示：请确保 handleAlipayNotify 内部做了以下三件事：
                    // 1. 查询数据库判断订单是否已经是“已支付”状态，如果是则直接 return (防重复通知)
                    // 2. 判断 totalAmount 是否等于 数据库里这笔订单的金额 (防金额篡改)
                    // 3. 开启事务：修改订单状态 + 增加用户余额 + 记录资金流水
                    rechargeService.handleAlipayNotify(outTradeNo, alipayTradeNo, new BigDecimal(totalAmount));
                }
                return "success";
            } else {
                log.error("支付宝回调验签失败，存在黑客伪造风险！");
                return "failure";
            }
        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return "failure";
        }
    }

    /**
     * 3. 供前端轮询查询订单支付状态
     */
    @GetMapping("/alipay/status")
    public Result checkOrderStatus(@RequestParam String outTradeNo) {
        Integer userId = BaseContext.getCurrentId().intValue();
        RechargeOrder order = rechargeService.findOrderForUser(outTradeNo, userId);
        if (order == null) {
            return Result.error(404, "充值订单不存在");
        }

        if (Integer.valueOf(1).equals(order.getStatus())) {
            // 订单状态只有在充值事务提交后才会变为 1，此处再直接从数据库读取余额，
            // 将已经提交的余额随支付状态一起返回，避免前端紧接着读取到 Redis 旧值。
            BigDecimal balance = accountService.getCurrentUserBalanceFresh(order.getUserId().longValue());
            return Result.success(paymentStatus(order, "PAID", true, true, balance));
        }
        if (Integer.valueOf(2).equals(order.getStatus())) {
            return Result.success(paymentStatus(order, "CLOSED", false, false, null));
        }

        // 本地回调可能还未到达，主动查询支付宝可以及时识别“已扫码、待付款”状态。
        String tradeStatus = queryAlipayTradeStatus(outTradeNo);
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            return Result.success(paymentStatus(order, "CONFIRMING", false, true, null));
        }
        if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
            return Result.success(paymentStatus(order, "SCANNED", false, true, null));
        }
        if ("TRADE_CLOSED".equals(tradeStatus)) {
            return Result.success(paymentStatus(order, "CLOSED", false, false, null));
        }
        return Result.success(paymentStatus(order, "WAITING_SCAN", false, false, null));
    }

    /** 读取充值订单关闭窗口，按支付宝支持的分钟格式生成超时参数。 */
    private String rechargeTimeoutExpress() {
        int minutes = SystemConfigUtils.intValue(sysConfigService.getConfigMap(),
                "recharge_close_minutes", 1, 1, 24 * 60);
        return minutes + "m";
    }

    /** 查询支付宝侧交易状态；网关暂时不可用时由本地订单状态继续兜底。 */
    private String queryAlipayTradeStatus(String outTradeNo) {
        try {
            AlipayTradeQueryRequest queryRequest = new AlipayTradeQueryRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", outTradeNo);
            queryRequest.setBizContent(bizContent.toJSONString());
            AlipayTradeQueryResponse response = alipayClient.execute(queryRequest);
            return response.isSuccess() ? response.getTradeStatus() : null;
        } catch (Exception e) {
            log.warn("查询支付宝订单状态失败，继续等待异步通知，订单号={}", outTradeNo, e);
            return null;
        }
    }

    private AlipayPaymentStatusVO paymentStatus(RechargeOrder order, String status,
                                                  boolean paid, boolean scanned,
                                                  BigDecimal balance) {
        return AlipayPaymentStatusVO.builder()
                .outTradeNo(order.getOutTradeNo())
                .status(status)
                .paid(paid)
                .scanned(scanned)
                .amount(order.getAmount())
                .balance(balance)
                .build();
    }
}
