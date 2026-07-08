package cn.cccs7.cynexus.knowledgebase.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeDocumentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cynexus-knowledgebase", contextId = "knowledgeBaseFeignClient")
public interface KnowledgeBaseFeignClient {

    @GetMapping("/api/knowledge-base/documents/{id}")
    Result<KnowledgeDocumentDTO> getDocument(@PathVariable("id") Long id);
}
