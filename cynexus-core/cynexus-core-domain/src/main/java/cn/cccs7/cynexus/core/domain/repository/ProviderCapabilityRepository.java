package cn.cccs7.cynexus.core.domain.repository;

import cn.cccs7.cynexus.core.domain.entity.ProviderCapabilityBO;
import java.util.List;

public interface ProviderCapabilityRepository {

    List<ProviderCapabilityBO> listEnabled();
}
