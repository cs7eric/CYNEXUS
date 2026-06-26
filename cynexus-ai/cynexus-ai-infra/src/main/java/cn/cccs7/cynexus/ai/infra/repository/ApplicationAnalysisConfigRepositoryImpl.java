package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisConfigBO;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisConfigRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisConfigBasicService;
import cn.cccs7.cynexus.ai.infra.convert.ApplicationAnalysisConfigPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationAnalysisConfigRepositoryImpl implements ApplicationAnalysisConfigRepository {

    private final ApplicationAnalysisConfigBasicService applicationAnalysisConfigBasicService;

    public ApplicationAnalysisConfigRepositoryImpl(ApplicationAnalysisConfigBasicService applicationAnalysisConfigBasicService) {
        this.applicationAnalysisConfigBasicService = applicationAnalysisConfigBasicService;
    }

    @Override
    public List<ApplicationAnalysisConfigBO> listAll() {
        return ApplicationAnalysisConfigPOConverter.INSTANCE.toTargetList(applicationAnalysisConfigBasicService.listAll());
    }

    @Override
    public ApplicationAnalysisConfigBO findById(Long id) {
        return Optional.ofNullable(applicationAnalysisConfigBasicService.getById(id))
                .map(ApplicationAnalysisConfigPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(ApplicationAnalysisConfigBO config) {
        return applicationAnalysisConfigBasicService.create(ApplicationAnalysisConfigPOConverter.INSTANCE.toSource(config));
    }

    @Override
    public boolean update(ApplicationAnalysisConfigBO config) {
        return applicationAnalysisConfigBasicService.update(ApplicationAnalysisConfigPOConverter.INSTANCE.toSource(config));
    }

    @Override
    public boolean delete(Long id) {
        return applicationAnalysisConfigBasicService.delete(id);
    }
}
