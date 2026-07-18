package cn.cccs7.cynexus.ai.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiImageInputBO {
    private String fileName;
    private String contentType;
    private String dataUrl;
}
