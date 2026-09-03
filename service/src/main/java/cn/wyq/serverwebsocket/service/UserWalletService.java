package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.pojo.dto.AdjustBalanceDTO;
import cn.wyq.serverwebsocket.pojo.dto.WalletQueryDTO;
import cn.wyq.serverwebsocket.pojo.vo.UserWalletVO;

import java.util.List;

/**
 * UserWalletService：业务服务接口，定义对应领域的业务操作契约。
 */
public interface UserWalletService {
    PageResult<List<UserWalletVO>> getWalletPage(WalletQueryDTO queryDTO);
    void manualAdjustBalance(AdjustBalanceDTO dto);
}