package cn.wyq.serverwebsocket.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能客服输入联想返回对象。
 *
 * <p>只向前端暴露问题文本、相似度和使用次数，避免泄露题库内部字段。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiServiceQuestionSuggestionVO {

    /** 推荐的问题文本。 */
    private String question;
    /** 与当前输入的相似度，范围为 0 到 1。 */
    private Double similarity;
    /** 该问题在题库中的累计使用次数。 */
    private Integer useCount;
}
