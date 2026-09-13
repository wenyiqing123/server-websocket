package cn.wyq.serverwebsocket.utils;

import java.util.Map;

/**
 * 系统配置值读取工具，负责把数据库中的字符串配置安全地转换为业务类型。
 *
 * <p>配置由管理员维护，不能假设数据库中的值永远合法；解析失败或超出范围时
 * 统一返回调用方提供的默认值，避免异常配置影响主业务链路。</p>
 */
public final class SystemConfigUtils {

    private SystemConfigUtils() {
        // 工具类不允许实例化。
    }

    /** 读取非空字符串配置。 */
    public static String stringValue(Map<String, String> values, String key, String defaultValue) {
        if (values == null || key == null) {
            return defaultValue;
        }
        String value = values.get(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    /** 读取整数配置，并校验闭区间范围。 */
    public static int intValue(Map<String, String> values, String key,
                               int defaultValue, int minValue, int maxValue) {
        if (minValue > maxValue || values == null || key == null) {
            return defaultValue;
        }
        String value = values.get(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= minValue && parsed <= maxValue ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 读取长整数配置，并校验闭区间范围。 */
    public static long longValue(Map<String, String> values, String key,
                                 long defaultValue, long minValue, long maxValue) {
        if (minValue > maxValue || values == null || key == null) {
            return defaultValue;
        }
        String value = values.get(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed >= minValue && parsed <= maxValue ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 读取小数配置，并校验闭区间范围。 */
    public static double doubleValue(Map<String, String> values, String key,
                                     double defaultValue, double minValue, double maxValue) {
        if (minValue > maxValue || values == null || key == null) {
            return defaultValue;
        }
        String value = values.get(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            return Double.isFinite(parsed) && parsed >= minValue && parsed <= maxValue
                    ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 读取布尔配置，兼容 true/false 与 1/0 两种存储形式。 */
    public static boolean booleanValue(Map<String, String> values, String key, boolean defaultValue) {
        if (values == null || key == null) {
            return defaultValue;
        }
        String value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(value.trim()) || "0".equals(value.trim())) {
            return false;
        }
        return defaultValue;
    }
}
