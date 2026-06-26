package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultCurrentBO;
import java.util.List;

public interface ApplicationAnalysisResultCurrentRepository {

    List<ApplicationAnalysisResultCurrentBO> listAll();

    ApplicationAnalysisResultCurrentBO findById(Long id);

    Long create(ApplicationAnalysisResultCurrentBO result);

    boolean update(ApplicationAnalysisResultCurrentBO result);

    boolean delete(Long id);
}
