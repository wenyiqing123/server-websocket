package cn.wyq.serverwebsocket.convert;

import cn.wyq.serverwebsocket.pojo.dto.TestDTO;
import cn.wyq.serverwebsocket.pojo.entity.Test;
import org.mapstruct.Mapper;

// componentModel = "spring" 让它变成一个 Spring Bean
@Mapper(componentModel = "spring")
public interface TestConvert {

    /**
     * DTO -> Entity
     * MapStruct 会自动匹配 id -> id, username -> username
     * 剩下的字段 (createTime, createUser 等) 默认为 null
     */
    Test toEntity(TestDTO testDTO);

    /**
     * Entity -> DTO (通常也会需要反向转换)
     */
    TestDTO toDTO(Test test);


}


/**
 * // 场景 1：单对象转换
 *     @Mappings({
 *         // 字段名不同：源(username) -> 目标(name)
 *         @Mapping(source = "username", target = "name"),
 *         // 类型不同：LocalDateTime -> String (自动格式化)
 *         @Mapping(source = "createTime", target = "createTime", dateFormat = "yyyy-MM-dd HH:mm:ss"),
 *         // 忽略字段：目标里没有 password，默认就会忽略，不需要特意配置，
 *         // 但如果目标里有 password 而你想忽略，可以写 @Mapping(target = "password", ignore = true)
 *         @Mapping(target = "statusDesc", expression = "java(mapStatus(user.getStatus()))") // 高级：自定义逻辑
 *     })
 *     UserDTO toDTO(User user);
 *
 *     // 场景 2：列表转换 (MapStruct 会自动循环调用上面的 toDTO 方法，极其方便)
 *     List<UserDTO> toDTOList(List<User> userList);
 *
 *     // 自定义转换逻辑 (Java 8 default 方法)
 *     default String mapStatus(Integer status) {
 *         if (status == null) return "未知";
 *         return status == 1 ? "正常" : "禁用";
 *     }
 */





