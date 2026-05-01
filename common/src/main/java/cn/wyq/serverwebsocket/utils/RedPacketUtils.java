package cn.wyq.serverwebsocket.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RedPacketUtils {

    /**
     * 二倍均值法拆分红包
     *
     * @param totalAmount 红包总金额 (元)
     * @param totalCount  红包总个数
     * @return 拆分后的金额列表
     */
    public static List<BigDecimal> splitRedPacket(BigDecimal totalAmount, int totalCount) {
        List<BigDecimal> resultList = new ArrayList<>();

        // 1. 拦截非法参数：把“元”转成“分”，判断钱够不够分（每人至少要能分到1分钱）
        int totalFen = totalAmount.multiply(new BigDecimal("100")).intValue();
        if (totalFen < totalCount) {
            throw new IllegalArgumentException("金额不足！保证每人至少能抢到0.01元");
        }

        int remainingFen = totalFen;     // 剩余金额（分）
        int remainingCount = totalCount; // 剩余人数

        // 引入你之前学过的高并发随机数生成器
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 2. 循环拆分，留出最后一个人
        for (int i = 0; i < totalCount - 1; i++) {
            // 核心公式：当前均值的2倍作为随机上限
            int maxFen = (remainingFen / remainingCount) * 2;

            // 安全防线：必须保证剩下的每个人都至少有1分钱！
            // (假设还剩2人，总共只剩3分钱。此时即使 maxFen 算出来是 2，我们也不能随机到 2，否则最后一人就变 0 了)
            int safeMaxFen = remainingFen - (remainingCount - 1);
            maxFen = Math.min(maxFen, safeMaxFen);

            // 抽取金额：范围是 [1, maxFen)
            int currentFen = 1;
            if (maxFen > 1) {
                currentFen = random.nextInt(1, maxFen);
            }

            // 装入集合，更新剩余变量
            resultList.add(new BigDecimal(currentFen).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            remainingFen -= currentFen;
            remainingCount--;
        }

        // 3. 最后一个人“兜底”，拿走剩下的所有钱，保证账面绝对平
        resultList.add(new BigDecimal(remainingFen).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));

        return resultList;
    }

    // 简单测试一下
    public static void main(String[] args) {
        BigDecimal money = new BigDecimal("100.00"); // 100元
        int count = 10; // 10个人

        List<BigDecimal> list = splitRedPacket(money, count);

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < list.size(); i++) {
            System.out.println("第 " + (i + 1) + " 个人抢到: " + list.get(i) + " 元");
            sum = sum.add(list.get(i));
        }
        System.out.println("加总核对: " + sum + " 元");
    }
}