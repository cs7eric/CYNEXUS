package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationConfigPO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationResultPO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationTargetPO;
import java.util.List;
import java.util.Optional;

public interface IndustryApplicationBasicService {

    IndustryApplicationConfigPO getOrCreateConfig();

    void saveConfig(IndustryApplicationConfigPO value);

    List<IndustryApplicationTargetPO> listTargets();

    Optional<IndustryApplicationTargetPO> findTarget(String targetKey);

    void replaceTargets(List<IndustryApplicationTargetPO> targets);

    Optional<IndustryApplicationResultPO> findResult(String targetKey);

    void saveResult(IndustryApplicationResultPO value);
}
