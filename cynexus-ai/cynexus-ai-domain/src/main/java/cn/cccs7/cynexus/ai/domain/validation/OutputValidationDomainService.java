package cn.cccs7.cynexus.ai.domain.validation;

public interface OutputValidationDomainService {

    boolean validateJsonRequired(boolean jsonOutputRequired, String content);
}
