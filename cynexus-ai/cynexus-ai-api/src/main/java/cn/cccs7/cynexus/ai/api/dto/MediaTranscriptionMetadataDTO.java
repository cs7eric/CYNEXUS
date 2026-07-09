package cn.cccs7.cynexus.ai.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionMetadataDTO {

    private String title;
    private List<String> categories;
    private List<String> tags;
}
