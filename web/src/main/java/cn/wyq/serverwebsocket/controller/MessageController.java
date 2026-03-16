package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.AjaxResult;
import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.MessageExportDTO;
import cn.wyq.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.Message;
import cn.wyq.serverwebsocket.pojo.socket.MessageFull;
import cn.wyq.serverwebsocket.pojo.socket.MessageInfo;
import cn.wyq.serverwebsocket.service.MessageService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/message")
@Validated
@Tag(name = "消息相关", description = "消息相关接口")
@Slf4j // 💡 新增：Lombok 日志注解
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 查询所有消息
     *
     * @param messageInfo
     * @return
     */
    @GetMapping
    @Operation(summary = "查询所有消息", description = "查询所有消息，返回消息列表")
    public Result findAll(MessageInfo messageInfo) {
        log.info("请求查询所有消息，查询参数 payload: {}", messageInfo);
        List<MessageFull> messageFullListList = messageService.findAll(messageInfo);
        return Result.success(messageFullListList);
    }

    /**
     * 撤回消息
     *
     * @param id
     * @return
     */
    @PatchMapping("/{id}")
    @Operation(summary = "撤回消息", description = "根据消息ID撤回消息")
    public Result recallMessage(@PathVariable int id) {
        log.info("请求撤回消息，待撤回消息ID: {}", id);
        messageService.recallMessage(id);
        return Result.success();
    }

    /**
     * 分页查询消息
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询消息", description = "根据分页查询条件分页查询消息，返回消息列表")
    public PageResult<List<Message>> list(@ParameterObject MessageQueryDTO messageQueryDTO) {
        log.info("请求分页查询消息，分页条件 payload: {}", messageQueryDTO);
        return messageService.pageQuery(messageQueryDTO);
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除消息", description = "根据消息ID删除消息")
    public AjaxResult<Void> deleteMessage(@PathVariable Integer id) {
        log.info("请求彻底删除消息，待删除消息ID: {}", id);
        messageService.deleteMessage(id);
        return AjaxResult.success();
    }

    @GetMapping("/export")
    @Operation(summary = "按条件导出excel", description = "按条件导出excel")
    public void exportUser(@ParameterObject MessageExportDTO messageExportDTO, HttpServletResponse response) throws IOException {
        log.info("导出消息记录到excel：{}",messageExportDTO);
        // 1. 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("消息列表", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        // 2. 获取数据（模拟从 service 获取）
        List<Message> messageList = messageService.export(messageExportDTO);
        // 3. 一行代码写出
        EasyExcel.write(response.getOutputStream(), Message.class)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("消息信息")
                .doWrite(messageList);
    }
}