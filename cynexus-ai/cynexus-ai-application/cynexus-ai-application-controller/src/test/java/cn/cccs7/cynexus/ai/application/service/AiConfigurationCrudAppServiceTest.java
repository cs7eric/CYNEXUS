package cn.cccs7.cynexus.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.service.configuration.AiConfigurationDomainService;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiConfigurationCrudAppServiceTest {

    @Test
    void delegatesConfigurationUseCaseToDomainService() {
        AiProviderBO provider = AiProviderBO.builder().id(1L).code("custom").build();
        AiConfigurationDomainService domainService = (AiConfigurationDomainService) Proxy.newProxyInstance(
                AiConfigurationDomainService.class.getClassLoader(),
                new Class<?>[]{AiConfigurationDomainService.class},
                (proxy, method, args) -> method.getName().equals("listProviders")
                        ? List.of(provider) : defaultValue(method.getReturnType()));

        List<AiProviderBO> result = new AiConfigurationCrudAppService(domainService).listProviders();

        assertThat(result).containsExactly(provider);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType.equals(boolean.class)) return false;
        if (returnType.equals(List.class)) return List.of();
        return null;
    }
}
