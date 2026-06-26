package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import java.util.List;

public interface AiProviderRepository {

    List<AiProviderBO> listAll();

    AiProviderBO findById(Long id);

    Long create(AiProviderBO provider);

    boolean update(AiProviderBO provider);

    boolean delete(Long id);
}
