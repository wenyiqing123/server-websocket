package cn.wyq.serverwebsocket.service.impl;


import cn.wyq.serverwebsocket.convert.TestConvert;
import cn.wyq.serverwebsocket.mapper.TestMapper;
import cn.wyq.serverwebsocket.pojo.dto.TestDTO;
import cn.wyq.serverwebsocket.pojo.entity.Test;
import cn.wyq.serverwebsocket.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl implements TestService {
    @Autowired
    private TestMapper testMapper;;
    @Autowired
    private TestConvert testConvert;

    @Override
    public void testAutoFill(TestDTO testDTO) {
        Test test = testConvert.toEntity(testDTO);
        testMapper.insert(test);
    }
}
