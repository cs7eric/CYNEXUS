package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisConfigBO;
import java.util.List;

public interface ApplicationAnalysisConfigRepository {

    List<ApplicationAnalysisConfigBO> listAll();

    ApplicationAnalysisConfigBO findById(Long id);

    Long create(ApplicationAnalysisConfigBO config);

    boolean update(ApplicationAnalysisConfigBO config);

    boolean delete(Long id);
}
