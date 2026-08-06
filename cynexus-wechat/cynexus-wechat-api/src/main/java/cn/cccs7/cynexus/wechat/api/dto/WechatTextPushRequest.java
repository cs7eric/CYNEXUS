package cn.cccs7.cynexus.wechat.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WechatTextPushRequest {
    @NotEmpty
    @Size(max = 100)
    private List<@NotBlank @Size(max = 64) String> openIds;

    @NotBlank
    @Size(max = 600)
    private String content;
}
