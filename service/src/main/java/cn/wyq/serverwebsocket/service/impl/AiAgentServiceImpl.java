package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.constant.ModelConstant;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.AiAgentMapper;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.dto.AiAgentCreateDTO;
import cn.wyq.serverwebsocket.pojo.dto.AiBuiltinAgentUpdateDTO;
import cn.wyq.serverwebsocket.pojo.dto.AiAgentUpdateDTO;
import cn.wyq.serverwebsocket.pojo.entity.AiAgent;
import cn.wyq.serverwebsocket.pojo.entity.BuiltinAiAgent;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.service.AiAgentService;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.utils.CacheClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/** 自定义智能体业务实现，集中处理校验、所有权和管理员权限。 */
@Service
@RequiredArgsConstructor
public class AiAgentServiceImpl implements AiAgentService {
    private static final int MAX_AGENT_COUNT = 30;
    private static final int MAX_PROMPT_LENGTH = 10000;
    private static final int MAX_WELCOME_LENGTH = 1000;
    private static final BigDecimal MIN_TEMPERATURE = BigDecimal.ZERO;
    private static final BigDecimal MAX_TEMPERATURE = BigDecimal.ONE;
    private static final String REMOTE_CHAT_MODEL = "qwen3.8-max-0902";
    private static final List<String> ALLOWED_MODELS = List.of(REMOTE_CHAT_MODEL);
    private static final String BUILTIN_KEY = "girlfriend";
    private static final String DEFAULT_BUILTIN_NAME = "女友助手";
    private static final String DEFAULT_BUILTIN_WELCOME = "我会保留原有女友模式的聊天记忆，也可以从这里管理你的专属智能体。";
    private static final String DEFAULT_BUILTIN_THEME = "#ff6b81";
    private static final BigDecimal DEFAULT_BUILTIN_TEMPERATURE = new BigDecimal("0.70");
    private static final int DEFAULT_BUILTIN_MAX_TOKENS = 2048;
    private static final int BUILTIN_STATUS_ENABLED = 1;

    private final AiAgentMapper aiAgentMapper;
    private final UserMapper userMapper;
    private final CacheClient cacheClient;
    private final SysConfigService sysConfigService;

    @Override
    public BuiltinAiAgent getBuiltinGirlfriend() {
        return fromConfig(sysConfigService.getConfigMap());
    }

    @Override
    public BuiltinAiAgent getBuiltinGirlfriendForAdmin(UserEntity operator) {
        assertAdmin(operator);
        return getBuiltinGirlfriend();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BuiltinAiAgent updateBuiltinGirlfriend(UserEntity operator, AiBuiltinAgentUpdateDTO dto) {
        assertAdmin(operator);
        if (dto == null) {
            throw new ServiceException("智能体参数不能为空", 400);
        }
        validateBuiltin(dto);
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        putIfPresent(values, "ai_girlfriend_name", dto.getName());
        putIfPresent(values, "ai_girlfriend_avatar", dto.getAvatarUrl());
        putIfPresent(values, "ai_girlfriend_welcome", dto.getWelcomeMessage());
        putIfPresent(values, "ai_girlfriend_system_prompt", dto.getSystemPrompt());
        putIfPresent(values, "ai_girlfriend_model", dto.getModel());
        putIfPresent(values, "ai_girlfriend_temperature", dto.getTemperature() == null ? null : dto.getTemperature().toPlainString());
        putIfPresent(values, "ai_girlfriend_max_tokens", dto.getMaxTokens() == null ? null : String.valueOf(dto.getMaxTokens()));
        putIfPresent(values, "ai_girlfriend_theme_color", dto.getThemeColor());
        putIfPresent(values, "ai_girlfriend_status", dto.getStatus() == null ? null : String.valueOf(dto.getStatus()));

        // 兼容旧版女友页面，旧键与新键保持同步，避免两个入口看到不同配置。
        if (dto.getAvatarUrl() != null) putIfPresent(values, "ai_avatar", dto.getAvatarUrl());
        if (dto.getSystemPrompt() != null) putIfPresent(values, "ai_system_prompt", dto.getSystemPrompt());
        if (dto.getTemperature() != null) putIfPresent(values, "ai_temperature", dto.getTemperature().toPlainString());
        if (dto.getThemeColor() != null) putIfPresent(values, "ai_theme_color", dto.getThemeColor());
        if (!values.isEmpty()) {
            sysConfigService.updateValues(values);
        }
        return getBuiltinGirlfriend();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BuiltinAiAgent resetBuiltinGirlfriend(UserEntity operator) {
        assertAdmin(operator);
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("ai_girlfriend_name", DEFAULT_BUILTIN_NAME);
        values.put("ai_girlfriend_avatar", "");
        values.put("ai_girlfriend_welcome", DEFAULT_BUILTIN_WELCOME);
        values.put("ai_girlfriend_system_prompt", ModelConstant.GirlFriend_SYSTEM_PROMPT);
        values.put("ai_girlfriend_model", REMOTE_CHAT_MODEL);
        values.put("ai_girlfriend_temperature", DEFAULT_BUILTIN_TEMPERATURE.toPlainString());
        values.put("ai_girlfriend_max_tokens", String.valueOf(DEFAULT_BUILTIN_MAX_TOKENS));
        values.put("ai_girlfriend_theme_color", DEFAULT_BUILTIN_THEME);
        values.put("ai_girlfriend_status", String.valueOf(BUILTIN_STATUS_ENABLED));
        values.put("ai_system_prompt", ModelConstant.GirlFriend_SYSTEM_PROMPT);
        values.put("ai_temperature", DEFAULT_BUILTIN_TEMPERATURE.toPlainString());
        values.put("ai_avatar", "");
        values.put("ai_theme_color", DEFAULT_BUILTIN_THEME);
        sysConfigService.updateValues(values);
        return getBuiltinGirlfriend();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBuiltinGirlfriendStatus(UserEntity operator, Integer status) {
        assertAdmin(operator);
        if (!List.of(0, 1).contains(status)) {
            throw new ServiceException("智能体状态不正确", 400);
        }
        sysConfigService.updateValues(java.util.Map.of(
                "ai_girlfriend_status", String.valueOf(status)));
    }

    @Override
    public BuiltinAiAgent requireUsableBuiltinGirlfriend() {
        BuiltinAiAgent agent = getBuiltinGirlfriend();
        if (!Integer.valueOf(1).equals(agent.getStatus())) {
            throw new ServiceException("AI 女友智能体已被禁用", 403);
        }
        return agent;
    }

    private BuiltinAiAgent fromConfig(java.util.Map<String, String> config) {
        String prompt = firstText(config, "ai_girlfriend_system_prompt", "ai_system_prompt");
        String avatar = firstText(config, "ai_girlfriend_avatar", "ai_avatar");
        String theme = firstText(config, "ai_girlfriend_theme_color", "ai_theme_color");
        String temperature = firstText(config, "ai_girlfriend_temperature", "ai_temperature");
        String maxTokens = firstText(config, "ai_girlfriend_max_tokens", null);
        return BuiltinAiAgent.builder()
                .key(BUILTIN_KEY)
                .name(firstText(config, "ai_girlfriend_name", null, DEFAULT_BUILTIN_NAME))
                .avatarUrl(avatar)
                .welcomeMessage(firstText(config, "ai_girlfriend_welcome", null, DEFAULT_BUILTIN_WELCOME))
                .systemPrompt(StringUtils.hasText(prompt) ? prompt : ModelConstant.GirlFriend_SYSTEM_PROMPT)
                .model(resolveRemoteModel(firstText(config, "ai_girlfriend_model", null, REMOTE_CHAT_MODEL)))
                .temperature(parseTemperature(temperature))
                .maxTokens(parseMaxTokens(maxTokens))
                .themeColor(StringUtils.hasText(theme) ? theme : DEFAULT_BUILTIN_THEME)
                .status(parseStatus(firstText(config, "ai_girlfriend_status", null)))
                .builtin(true)
                .build();
    }

    private String firstText(java.util.Map<String, String> config, String primary, String fallbackKey) {
        return firstText(config, primary, fallbackKey, null);
    }

    private String firstText(java.util.Map<String, String> config, String primary, String fallbackKey, String defaultValue) {
        String value = config.get(primary);
        if (!StringUtils.hasText(value) && fallbackKey != null) value = config.get(fallbackKey);
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    /** 历史配置可能残留其它云端模型，运行时统一收敛到当前允许的模型。 */
    private String resolveRemoteModel(String configured) {
        return ALLOWED_MODELS.contains(configured == null ? "" : configured.trim())
                ? configured.trim() : REMOTE_CHAT_MODEL;
    }

    private BigDecimal parseTemperature(String value) {
        try {
            return StringUtils.hasText(value) ? new BigDecimal(value) : DEFAULT_BUILTIN_TEMPERATURE;
        } catch (NumberFormatException e) {
            return DEFAULT_BUILTIN_TEMPERATURE;
        }
    }

    private int parseMaxTokens(String value) {
        try {
            int parsed = Integer.parseInt(value == null ? "" : value);
            return parsed >= 64 && parsed <= 8192 ? parsed : DEFAULT_BUILTIN_MAX_TOKENS;
        } catch (NumberFormatException e) {
            return DEFAULT_BUILTIN_MAX_TOKENS;
        }
    }

    private int parseStatus(String value) {
        return "0".equals(value) ? 0 : 1;
    }

    private void putIfPresent(java.util.Map<String, String> values, String key, String value) {
        if (value != null) values.put(key, value);
    }

    private void validateBuiltin(AiBuiltinAgentUpdateDTO dto) {
        if (dto.getName() != null && (dto.getName().trim().isEmpty() || dto.getName().trim().length() > 50)) {
            throw new ServiceException("智能体名称长度必须为 1-50 个字符", 400);
        }
        if (dto.getSystemPrompt() != null && dto.getSystemPrompt().length() > MAX_PROMPT_LENGTH) {
            throw new ServiceException("系统提示词不能超过 " + MAX_PROMPT_LENGTH + " 个字符", 400);
        }
        if (dto.getWelcomeMessage() != null && dto.getWelcomeMessage().length() > MAX_WELCOME_LENGTH) {
            throw new ServiceException("欢迎语不能超过 " + MAX_WELCOME_LENGTH + " 个字符", 400);
        }
        if (dto.getAvatarUrl() != null && dto.getAvatarUrl().length() > 500) {
            throw new ServiceException("头像地址过长", 400);
        }
        if (dto.getTemperature() != null && (dto.getTemperature().compareTo(MIN_TEMPERATURE) < 0
                || dto.getTemperature().compareTo(MAX_TEMPERATURE) > 0)) {
            throw new ServiceException("temperature 必须在 0 到 1 之间", 400);
        }
        if (dto.getMaxTokens() != null && (dto.getMaxTokens() < 64 || dto.getMaxTokens() > 8192)) {
            throw new ServiceException("maxTokens 必须在 64 到 8192 之间", 400);
        }
        if (dto.getModel() != null && !ALLOWED_MODELS.contains(dto.getModel().trim())) {
            throw new ServiceException("不支持该模型", 400);
        }
        if (dto.getThemeColor() != null && !dto.getThemeColor().matches("^#[0-9a-fA-F]{6}$")) {
            throw new ServiceException("主题色必须是六位十六进制颜色", 400);
        }
        if (dto.getStatus() != null && !List.of(0, 1).contains(dto.getStatus())) {
            throw new ServiceException("智能体状态不正确", 400);
        }
    }

    @Override
    public List<AiAgent> listMine(Integer ownerUserId) {
        requireUserId(ownerUserId);
        return aiAgentMapper.selectByOwner(ownerUserId);
    }

    @Override
    public AiAgent getMine(Long id, Integer ownerUserId) {
        requireUserId(ownerUserId);
        AiAgent agent = aiAgentMapper.selectOwned(id, ownerUserId);
        if (agent == null) {
            throw new ServiceException("智能体不存在或无权访问", 404);
        }
        return agent;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAgent create(Integer ownerUserId, AiAgentCreateDTO dto) {
        requireUserId(ownerUserId);
        if (dto == null) {
            throw new ServiceException("智能体参数不能为空", 400);
        }
        if (userMapper.lockById(ownerUserId) == null) {
            throw new ServiceException("用户不存在", 404);
        }
        if (aiAgentMapper.countByOwner(ownerUserId) >= MAX_AGENT_COUNT) {
            throw new ServiceException("每个用户最多创建 " + MAX_AGENT_COUNT + " 个智能体", 400);
        }
        AiAgent agent = new AiAgent();
        BeanUtils.copyProperties(dto, agent);
        agent.setOwnerUserId(ownerUserId);
        normalizeAndValidate(agent, true);
        if (!StringUtils.hasText(agent.getSystemPrompt())) {
            agent.setSystemPrompt("你是一个友善、可靠的 AI 助手，请结合上下文直接回答用户问题。");
        }
        if (!StringUtils.hasText(agent.getModel())) {
            agent.setModel(REMOTE_CHAT_MODEL);
        }
        if (agent.getTemperature() == null) {
            agent.setTemperature(new BigDecimal("0.70"));
        }
        if (agent.getMaxTokens() == null) {
            agent.setMaxTokens(2048);
        }
        agent.setStatus(1);
        agent.setIsBuiltin(0);
        agent.setPromptVersion(1);
        agent.setVisibility("private");
        aiAgentMapper.insert(agent);
        evictCaches(ownerUserId, agent.getId());
        return aiAgentMapper.selectOwned(agent.getId(), ownerUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAgent updateOwned(Long id, Integer ownerUserId, AiAgentUpdateDTO dto) {
        getMine(id, ownerUserId);
        requireUpdateDto(dto);
        AiAgent update = new AiAgent();
        BeanUtils.copyProperties(dto, update);
        update.setId(id);
        update.setOwnerUserId(ownerUserId);
        normalizeAndValidate(update, false);
        if (dto.getStatus() != null) {
            throw new ServiceException("用户不能修改智能体启用状态", 403);
        }
        if (isEmptyUpdate(update)) {
            throw new ServiceException("没有可更新的内容", 400);
        }
        aiAgentMapper.updateOwned(update);
        evictCaches(ownerUserId, id);
        return getMine(id, ownerUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwned(Long id, Integer ownerUserId) {
        getMine(id, ownerUserId);
        if (aiAgentMapper.softDeleteOwned(id, ownerUserId) == 0) {
            throw new ServiceException("智能体删除失败", 400);
        }
        evictCaches(ownerUserId, id);
    }

    @Override
    public AiAgent requireUsable(Long id, Integer ownerUserId) {
        AiAgent agent = getMine(id, ownerUserId);
        if (!Integer.valueOf(1).equals(agent.getStatus())) {
            throw new ServiceException("该智能体已被禁用", 403);
        }
        // 兼容尚未执行数据库迁移的历史记录，确保运行时不会调用旧的云端模型。
        agent.setModel(REMOTE_CHAT_MODEL);
        return agent;
    }

    @Override
    public AiAgent requireOwnedForSession(Long id, Integer ownerUserId) {
        requireUserId(ownerUserId);
        if (id == null || id <= 0) {
            throw new ServiceException("智能体编号不能为空", 400);
        }
        AiAgent agent = aiAgentMapper.selectOwnedIncludingDeleted(id, ownerUserId);
        if (agent == null) {
            throw new ServiceException("智能体不存在或无权访问", 404);
        }
        return agent;
    }

    @Override
    public List<AiAgent> listForAdmin(UserEntity operator, String ownerUserName, String name, Integer status) {
        assertAdmin(operator);
        return aiAgentMapper.selectForAdmin(ownerUserName, name, status);
    }

    @Override
    public AiAgent getForAdmin(UserEntity operator, Long id) {
        assertAdmin(operator);
        AiAgent agent = aiAgentMapper.selectById(id);
        if (agent == null) {
            throw new ServiceException("智能体不存在", 404);
        }
        return agent;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAgent updateForAdmin(UserEntity operator, Long id, AiAgentUpdateDTO dto) {
        assertAdmin(operator);
        AiAgent existing = getForAdmin(operator, id);
        requireUpdateDto(dto);
        AiAgent update = new AiAgent();
        BeanUtils.copyProperties(dto, update);
        update.setId(id);
        normalizeAndValidate(update, false);
        if (dto.getStatus() != null && !List.of(0, 1).contains(dto.getStatus())) {
            throw new ServiceException("智能体状态不正确", 400);
        }
        if (isEmptyUpdate(update)) {
            throw new ServiceException("没有可更新的内容", 400);
        }
        if (aiAgentMapper.updateByAdmin(update) == 0) {
            throw new ServiceException("智能体更新失败", 400);
        }
        evictCaches(existing.getOwnerUserId(), id);
        return getForAdmin(operator, id);
    }

    @Override
    public void updateStatus(UserEntity operator, Long id, Integer status) {
        assertAdmin(operator);
        if (!List.of(0, 1).contains(status)) {
            throw new ServiceException("智能体状态不正确", 400);
        }
        AiAgent existing = getForAdmin(operator, id);
        if (aiAgentMapper.updateStatus(id, status) == 0) {
            throw new ServiceException("状态更新失败", 400);
        }
        evictCaches(existing.getOwnerUserId(), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteForAdmin(UserEntity operator, Long id) {
        assertAdmin(operator);
        AiAgent existing = getForAdmin(operator, id);
        if (aiAgentMapper.softDeleteByAdmin(id) == 0) {
            throw new ServiceException("智能体删除失败", 400);
        }
        evictCaches(existing.getOwnerUserId(), id);
    }

    @Override
    public void markUsed(Long id) {
        if (id != null) {
            aiAgentMapper.incrementUseCount(id);
        }
    }

    private void normalizeAndValidate(AiAgent agent, boolean required) {
        if (agent.getName() != null) {
            agent.setName(agent.getName().trim());
        }
        if (required && !StringUtils.hasText(agent.getName())) {
            throw new ServiceException("智能体名称不能为空", 400);
        }
        if (agent.getName() != null && (agent.getName().length() < 1 || agent.getName().length() > 50)) {
            throw new ServiceException("智能体名称长度必须为 1-50 个字符", 400);
        }
        if (agent.getSystemPrompt() != null && agent.getSystemPrompt().length() > MAX_PROMPT_LENGTH) {
            throw new ServiceException("系统提示词不能超过 " + MAX_PROMPT_LENGTH + " 个字符", 400);
        }
        if (agent.getWelcomeMessage() != null && agent.getWelcomeMessage().length() > MAX_WELCOME_LENGTH) {
            throw new ServiceException("欢迎语不能超过 " + MAX_WELCOME_LENGTH + " 个字符", 400);
        }
        if (agent.getTemperature() != null && (agent.getTemperature().compareTo(MIN_TEMPERATURE) < 0
                || agent.getTemperature().compareTo(MAX_TEMPERATURE) > 0)) {
            throw new ServiceException("temperature 必须在 0 到 1 之间", 400);
        }
        if (agent.getMaxTokens() != null && (agent.getMaxTokens() < 64 || agent.getMaxTokens() > 8192)) {
            throw new ServiceException("maxTokens 必须在 64 到 8192 之间", 400);
        }
        if (agent.getModel() != null) {
            agent.setModel(agent.getModel().trim());
            if (!ALLOWED_MODELS.contains(agent.getModel())) {
                throw new ServiceException("不支持该模型", 400);
            }
        }
        if (agent.getVisibility() != null && !List.of("private", "public").contains(agent.getVisibility())) {
            throw new ServiceException("可见范围只能是 private 或 public", 400);
        }
        if (agent.getAvatarUrl() != null && agent.getAvatarUrl().length() > 500) {
            throw new ServiceException("头像地址过长", 400);
        }
    }

    private void requireUpdateDto(AiAgentUpdateDTO dto) {
        if (dto == null) {
            throw new ServiceException("智能体参数不能为空", 400);
        }
    }

    private boolean isEmptyUpdate(AiAgent agent) {
        return agent.getName() == null && agent.getSystemPrompt() == null && agent.getAvatarUrl() == null
                && agent.getWelcomeMessage() == null && agent.getModel() == null && agent.getTemperature() == null
                && agent.getMaxTokens() == null && agent.getVisibility() == null && agent.getStatus() == null;
    }

    private void assertAdmin(UserEntity operator) {
        if (operator == null || operator.getId() <= 0) {
            throw new ServiceException("请先登录", 401);
        }
        var user = userMapper.getCurrentUserById(operator.getId());
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())
                || (user.getRole() != 0 && user.getRole() != 1)) {
            throw new ServiceException("仅管理员可以管理智能体", 403);
        }
    }

    private void requireUserId(Integer ownerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new ServiceException("用户身份无效", 401);
        }
    }

    private void evictCaches(Integer ownerUserId, Long id) {
        cacheClient.evict("ai:agents:user", ownerUserId);
        cacheClient.evict("ai:agents:detail", id);
    }
}
