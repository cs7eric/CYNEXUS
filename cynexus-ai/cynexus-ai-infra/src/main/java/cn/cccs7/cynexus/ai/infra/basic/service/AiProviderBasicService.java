package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.AiProviderPO;
import java.util.List;

public interface AiProviderBasicService {

    List<AiProviderPO> listAll();

    AiProviderPO getById(Long id);

    Long create(AiProviderPO po);

    boolean update(AiProviderPO po);

    boolean delete(Long id);
}
