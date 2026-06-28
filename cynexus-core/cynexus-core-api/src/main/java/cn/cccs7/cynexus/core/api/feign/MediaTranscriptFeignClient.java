package cn.cccs7.cynexus.core.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.MediaTranscriptDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-core", contextId = "mediaTranscriptFeignClient", path = "/api/core/media-transcript")
public interface MediaTranscriptFeignClient {

    @GetMapping
    Result<List<MediaTranscriptDTO>> list();

    @GetMapping("/{id}")
    Result<MediaTranscriptDTO> get(@PathVariable("id") Long id);

    @PostMapping
    Result<Long> create(@RequestBody MediaTranscriptDTO dto);

    @PutMapping
    Result<Boolean> update(@RequestBody MediaTranscriptDTO dto);

    @DeleteMapping("/{id}")
    Result<Boolean> delete(@PathVariable("id") Long id);
}
