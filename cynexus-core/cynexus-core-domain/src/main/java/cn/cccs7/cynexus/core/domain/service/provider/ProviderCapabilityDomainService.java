package cn.cccs7.cynexus.core.domain.service.provider;

import cn.cccs7.cynexus.core.domain.entity.ProviderCapabilityBO;
import java.util.List;

public interface ProviderCapabilityDomainService {

    List<ProviderCapabilityBO> listEnabled();
}
