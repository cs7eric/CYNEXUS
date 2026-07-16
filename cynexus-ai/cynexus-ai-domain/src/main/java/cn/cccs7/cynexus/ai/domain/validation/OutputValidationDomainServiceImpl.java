package cn.cccs7.cynexus.ai.domain.validation;

import cn.cccs7.cynexus.ai.domain.validation.OutputValidationDomainService;

public class OutputValidationDomainServiceImpl implements OutputValidationDomainService {

    @Override
    public boolean validateJsonRequired(boolean jsonOutputRequired, String content) {
        if (!jsonOutputRequired) {
            return true;
        }
        String value = content == null ? "" : content.trim();
        return (value.startsWith("{") && value.endsWith("}")) || (value.startsWith("[") && value.endsWith("]"));
    }
}
