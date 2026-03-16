package cn.wyq.serverwebsocket.converter;

import cn.wyq.serverwebsocket.enumeration.UserRoleEnum;
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;

/**
 * 角色权限转换器：将数据库的 Integer (0,1,2) 转换为 Excel 中的文字 (超级管理员等)
 */
public class RoleConverter implements Converter<Integer> {

    @Override
    public Class<?> supportJavaTypeKey() {
        return Integer.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public WriteCellData<?> convertToExcelData(Integer value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        // 直接调用你枚举类里写好的静态工具方法
        return new WriteCellData<>(UserRoleEnum.getDescByCode(value));
    }
}