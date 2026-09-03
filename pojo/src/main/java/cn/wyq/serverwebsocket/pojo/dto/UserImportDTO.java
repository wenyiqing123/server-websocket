package cn.wyq.serverwebsocket.pojo.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

@Data
@ContentRowHeight(20)
// 💡 建议顺手加个表头高度，让第一行的标题看起来更大气
/**
 * UserImportDTO：数据传输对象，用于承载接口请求参数。
 */
@HeadRowHeight(25)
public class UserImportDTO {

    @ExcelProperty("用户名")
    @ColumnWidth(20)
    private String userName;

    @ExcelProperty("邮箱")
    @ColumnWidth(35)
    private String email;

    @ExcelProperty("角色权限(0普通,1管理,2群主)")
    @ColumnWidth(100)
    private Integer role;
}