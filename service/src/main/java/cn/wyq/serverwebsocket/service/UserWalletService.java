package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.pojo.dto.AdjustBalanceDTO;
import cn.wyq.serverwebsocket.pojo.dto.WalletQueryDTO;
import cn.wyq.serverwebsocket.pojo.vo.UserWalletVO;

import java.util.List;

public interface UserWalletService {
    PageResult<List<UserWalletVO>> getWalletPage(WalletQueryDTO queryDTO);
    void manualAdjustBalance(AdjustBalanceDTO dto);
}