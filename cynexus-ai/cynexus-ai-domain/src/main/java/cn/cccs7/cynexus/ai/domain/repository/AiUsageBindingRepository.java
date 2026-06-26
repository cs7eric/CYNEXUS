package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import java.util.List;

public interface AiUsageBindingRepository {

    List<AiUsageBindingBO> listAll();

    AiUsageBindingBO findById(Long id);

    Long create(AiUsageBindingBO binding);

    boolean update(AiUsageBindingBO binding);

    boolean delete(Long id);
}
