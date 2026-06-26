package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisTargetBO;
import java.util.List;

public interface ApplicationAnalysisTargetRepository {

    List<ApplicationAnalysisTargetBO> listAll();

    ApplicationAnalysisTargetBO findById(Long id);

    Long create(ApplicationAnalysisTargetBO target);

    boolean update(ApplicationAnalysisTargetBO target);

    boolean delete(Long id);
}
