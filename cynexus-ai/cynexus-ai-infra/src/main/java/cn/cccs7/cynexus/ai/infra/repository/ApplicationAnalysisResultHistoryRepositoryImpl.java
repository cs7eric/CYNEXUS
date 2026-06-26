package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultHistoryBO;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisResultHistoryRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisResultHistoryBasicService;
import cn.cccs7.cynexus.ai.infra.convert.ApplicationAnalysisResultHistoryPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationAnalysisResultHistoryRepositoryImpl implements ApplicationAnalysisResultHistoryRepository {

    private final ApplicationAnalysisResultHistoryBasicService applicationAnalysisResultHistoryBasicService;

    public ApplicationAnalysisResultHistoryRepositoryImpl(
            ApplicationAnalysisResultHistoryBasicService applicationAnalysisResultHistoryBasicService) {
        this.applicationAnalysisResultHistoryBasicService = applicationAnalysisResultHistoryBasicService;
    }

    @Override
    public List<ApplicationAnalysisResultHistoryBO> listAll() {
        return ApplicationAnalysisResultHistoryPOConverter.INSTANCE
                .toTargetList(applicationAnalysisResultHistoryBasicService.listAll());
    }

    @Override
    public ApplicationAnalysisResultHistoryBO findById(Long id) {
        return Optional.ofNullable(applicationAnalysisResultHistoryBasicService.getById(id))
                .map(ApplicationAnalysisResultHistoryPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(ApplicationAnalysisResultHistoryBO result) {
        return applicationAnalysisResultHistoryBasicService.create(
                ApplicationAnalysisResultHistoryPOConverter.INSTANCE.toSource(result));
    }

    @Override
    public boolean update(ApplicationAnalysisResultHistoryBO result) {
        return applicationAnalysisResultHistoryBasicService.update(
                ApplicationAnalysisResultHistoryPOConverter.INSTANCE.toSource(result));
    }

    @Override
    public boolean delete(Long id) {
        return applicationAnalysisResultHistoryBasicService.delete(id);
    }
}
