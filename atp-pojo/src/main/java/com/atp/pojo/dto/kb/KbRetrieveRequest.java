package com.atp.pojo.dto.kb;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识库检索测试入参。
 */
@Data
public class KbRetrieveRequest {

    /** 查询文本。 */
    @NotBlank(message = "查询内容不能为空")
    private String query;

    /** 检索模式：vector/keyword/hybrid。 */
    private String mode = "hybrid";

    /** 返回条数。 */
    private Integer topK = 5;
}
