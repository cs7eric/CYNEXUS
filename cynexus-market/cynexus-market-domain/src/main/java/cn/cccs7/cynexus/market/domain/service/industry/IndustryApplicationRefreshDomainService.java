package cn.cccs7.cynexus.market.domain.service.industry;

import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import java.util.List;

public interface IndustryApplicationRefreshDomainService {

    List<IndustryApplicationBO.RefreshResult> refresh(String targetId);
}
