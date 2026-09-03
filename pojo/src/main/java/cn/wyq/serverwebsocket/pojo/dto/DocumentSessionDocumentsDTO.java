package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * DocumentSessionDocumentsDTO：数据传输对象，用于承载接口请求参数。
 */
@Data
public class DocumentSessionDocumentsDTO {
    private List<String> docIds;
}
