package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.mapper.IMessageMapperTool;
import cn.wyq.serverwebsocket.pojo.entity.Message;
import cn.wyq.serverwebsocket.service.IMessageServiceTool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class IMessageServiceToolImpl extends ServiceImpl<IMessageMapperTool, Message> implements IMessageServiceTool {
}
