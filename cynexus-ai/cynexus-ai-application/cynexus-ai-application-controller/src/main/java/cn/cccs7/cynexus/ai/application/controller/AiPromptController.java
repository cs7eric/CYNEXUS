package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.api.dto.AiPromptDTO;
import cn.cccs7.cynexus.ai.api.dto.AiPromptTemplateDTO;
import cn.cccs7.cynexus.ai.application.convert.AiPromptDTOConverter;
import cn.cccs7.cynexus.ai.application.service.AiConfigurationCrudAppService;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.common.core.result.Result;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/prompts")
public class AiPromptController {

    private final AiConfigurationCrudAppService appService;

    public AiPromptController(AiConfigurationCrudAppService appService) {
        this.appService = appService;
    }

    @GetMapping
    public Result<List<AiPromptDTO>> listPrompts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "scenarioCode", required = false) String scenarioCode,
            @RequestParam(value = "status", required = false) String status) {
        return Result.success(AiPromptDTOConverter.INSTANCE.toSourceList(
                appService.listPrompts(keyword, scenarioCode, status)));
    }

    @GetMapping("/{id}")
    public Result<AiPromptDTO> getPrompt(@PathVariable("id") Long id) {
        AiPromptBO prompt = appService.findPromptById(id);
        return Result.success(prompt == null ? null : AiPromptDTOConverter.INSTANCE.toSource(prompt));
    }

    @PostMapping
    public Result<Long> createPrompt(@RequestBody AiPromptDTO dto) {
        return Result.success(appService.createPrompt(AiPromptDTOConverter.INSTANCE.toTarget(dto)));
    }

    @PutMapping
    public Result<Boolean> updatePrompt(@RequestBody AiPromptDTO dto) {
        return Result.success(appService.updatePrompt(AiPromptDTOConverter.INSTANCE.toTarget(dto)));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deletePrompt(@PathVariable("id") Long id) {
        return Result.success(appService.deletePrompt(id));
    }

    @GetMapping("/templates")
    public Result<List<AiPromptTemplateDTO>> listPromptTemplates() {
        return Result.success(appService.listPrompts(null, null, AiConstants.STATUS_ACTIVE).stream()
                .map(prompt -> new AiPromptTemplateDTO(prompt.getScenarioCode(),
                        prompt.getUserPromptTemplate() == null
                                || prompt.getUserPromptTemplate().isBlank()
                                ? prompt.getSystemPrompt()
                                : prompt.getUserPromptTemplate()))
                .toList());
    }
}
