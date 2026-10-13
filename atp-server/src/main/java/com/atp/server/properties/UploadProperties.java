package com.atp.server.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 附件存储配置。默认存在项目目录下的 data/uploads。
 */
@Data
@Component
@ConfigurationProperties(prefix = "atp.file")
public class UploadProperties {

    private String uploadDir = "./data/uploads"; // 附件根目录
    private String urlPrefix = "/files"; // 对外访问前缀
}
