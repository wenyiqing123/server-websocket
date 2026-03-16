package cn.wyq.serverwebsocket.pojo.dto;

import cn.wyq.serverwebsocket.converter.RoleConverter;
import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URL;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
// 🚨 必须放在类上！统管所有数据行的高度，给图片留出垂直空间
@ContentRowHeight(100)
// 💡 建议顺手加个表头高度，让第一行的标题看起来更大气
@HeadRowHeight(25)
public class UserExportDTO {

    @ExcelProperty("用户头像")
    // 💡 放在字段上！控制单列宽度。15 大概就是个正方形，适合放头像
    @ColumnWidth(25)
    private URL path;

    @ExcelProperty("用户编号")
    @ColumnWidth(12) // 编号比较短，给 12 够了
    private Integer id;

    @ExcelProperty("用户名")
    @ColumnWidth(20) // 名字可能有点长，给 20
    private String userName;

    @ExcelIgnore
    private String password;

    @ExcelProperty(value = "用户权限", converter = RoleConverter.class)
    @ColumnWidth(15) // 权限字数固定，给 15
    private Integer role;
}