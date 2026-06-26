package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultCurrentBO;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisResultCurrentRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisResultCurrentBasicService;
import cn.cccs7.cynexus.ai.infra.convert.ApplicationAnalysisResultCurrentPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationAnalysisResultCurrentRepositoryImpl implements ApplicationAnalysisResultCurrentRepository {

    private final ApplicationAnalysisResultCurrentBasicService applicationAnalysisResultCurrentBasicService;

    public ApplicationAnalysisResultCurrentRepositoryImpl(
            ApplicationAnalysisResultCurrentBasicService applicationAnalysisResultCurrentBasicService) {
        this.applicationAnalysisResultCurrentBasicService = applicationAnalysisResultCurrentBasicService;
    }

    @Override
    public List<ApplicationAnalysisResultCurrentBO> listAll() {
        return ApplicationAnalysisResultCurrentPOConverter.INSTANCE
                .toTargetList(applicationAnalysisResultCurrentBasicService.listAll());
    }

    @Override
    public ApplicationAnalysisResultCurrentBO findById(Long id) {
        return Optional.ofNullable(applicationAnalysisResultCurrentBasicService.getById(id))
                .map(ApplicationAnalysisResultCurrentPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(ApplicationAnalysisResultCurrentBO result) {
        return applicationAnalysisResultCurrentBasicService.create(
                ApplicationAnalysisResultCurrentPOConverter.INSTANCE.toSource(result));
    }

    @Override
    public boolean update(ApplicationAnalysisResultCurrentBO result) {
        return applicationAnalysisResultCurrentBasicService.update(
                ApplicationAnalysisResultCurrentPOConverter.INSTANCE.toSource(result));
    }

    @Override
    public boolean delete(Long id) {
        return applicationAnalysisResultCurrentBasicService.delete(id);
    }
}
