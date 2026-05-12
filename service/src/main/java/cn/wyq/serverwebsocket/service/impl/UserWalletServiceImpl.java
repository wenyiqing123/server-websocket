package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.mapper.UserWalletMapper;
import cn.wyq.serverwebsocket.pojo.dto.AdjustBalanceDTO;
import cn.wyq.serverwebsocket.pojo.dto.WalletQueryDTO;
import cn.wyq.serverwebsocket.pojo.vo.UserWalletVO;
import cn.wyq.serverwebsocket.service.UserWalletService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class UserWalletServiceImpl implements UserWalletService {

    @Autowired
    private UserWalletMapper walletMapper;

    @Override
    public PageResult<List<UserWalletVO>> getWalletPage(WalletQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        List<UserWalletVO> list = walletMapper.selectWalletList(queryDTO);
        PageInfo<UserWalletVO> pageInfo = new PageInfo<>(list);
        return  PageResult.success(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualAdjustBalance(AdjustBalanceDTO dto) {
        // 1. 获取当前用户的钱包信息（加行锁，防止并发调账冲突）
        BigDecimal currentBalance = walletMapper.getBalanceForUpdate(dto.getUserId());
        if (currentBalance == null) {
            throw new RuntimeException("用户钱包账户不存在");
        }

        BigDecimal finalAmount = dto.getAmount();

        // 2. 如果是扣款，校验余额并转换为负数
        if (dto.getActionType() == 2) {
            if (currentBalance.compareTo(dto.getAmount()) < 0) {
                throw new RuntimeException("扣款失败：用户余额不足");
            }
            finalAmount = dto.getAmount().negate(); // 变负数
        }

        // 3. 执行账户金额更新 (底层直接执行 balance = balance + finalAmount)
        walletMapper.updateBalance(dto.getUserId(), finalAmount);

        // 4. 计算变动后的余额，用于记录流水
        BigDecimal balanceAfter = currentBalance.add(finalAmount);

        // 5. 写入资金变动流水表 (type设为5，代表系统人工调账)
        String orderNo = "ADJ" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
        walletMapper.insertAccountLog(
                dto.getUserId(),
                5,
                finalAmount,
                balanceAfter,
                orderNo,
                "【系统人工调账】" + dto.getRemark()
        );
    }
}