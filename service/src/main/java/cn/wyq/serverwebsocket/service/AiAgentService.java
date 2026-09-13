package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.pojo.dto.AiAgentCreateDTO;
import cn.wyq.serverwebsocket.pojo.dto.AiBuiltinAgentUpdateDTO;
import cn.wyq.serverwebsocket.pojo.dto.AiAgentUpdateDTO;
import cn.wyq.serverwebsocket.pojo.entity.AiAgent;
import cn.wyq.serverwebsocket.pojo.entity.BuiltinAiAgent;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;

import java.util.List;

/** 自定义智能体业务服务。 */
public interface AiAgentService {
    /** 获取系统内置女友智能体配置，普通登录用户可读取。 */
    BuiltinAiAgent getBuiltinGirlfriend();

    /** 管理员查看完整的系统内置女友配置。 */
    BuiltinAiAgent getBuiltinGirlfriendForAdmin(UserEntity operator);

    /** 管理员修改系统内置女友智能体配置。 */
    BuiltinAiAgent updateBuiltinGirlfriend(UserEntity operator, AiBuiltinAgentUpdateDTO dto);

    /** 管理员恢复系统内置女友智能体默认配置。 */
    BuiltinAiAgent resetBuiltinGirlfriend(UserEntity operator);

    /** 管理员单独切换系统内置女友智能体状态。 */
    void updateBuiltinGirlfriendStatus(UserEntity operator, Integer status);

    /** 校验内置女友是否可用于新会话或模型请求。 */
    BuiltinAiAgent requireUsableBuiltinGirlfriend();

    List<AiAgent> listMine(Integer ownerUserId);

    AiAgent getMine(Long id, Integer ownerUserId);

    AiAgent create(Integer ownerUserId, AiAgentCreateDTO dto);

    AiAgent updateOwned(Long id, Integer ownerUserId, AiAgentUpdateDTO dto);

    void deleteOwned(Long id, Integer ownerUserId);

    AiAgent requireUsable(Long id, Integer ownerUserId);

    /** 校验智能体归属，允许软删除智能体用于查看和删除既有会话。 */
    AiAgent requireOwnedForSession(Long id, Integer ownerUserId);

    List<AiAgent> listForAdmin(UserEntity operator, String ownerUserName, String name, Integer status);

    AiAgent getForAdmin(UserEntity operator, Long id);

    AiAgent updateForAdmin(UserEntity operator, Long id, AiAgentUpdateDTO dto);

    void updateStatus(UserEntity operator, Long id, Integer status);

    void deleteForAdmin(UserEntity operator, Long id);

    void markUsed(Long id);
}
