package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultHistoryBO;
import java.util.List;

public interface ApplicationAnalysisResultHistoryRepository {

    List<ApplicationAnalysisResultHistoryBO> listAll();

    ApplicationAnalysisResultHistoryBO findById(Long id);

    Long create(ApplicationAnalysisResultHistoryBO result);

    boolean update(ApplicationAnalysisResultHistoryBO result);

    boolean delete(Long id);
}
