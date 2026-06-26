package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisTargetBO;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisTargetRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisTargetBasicService;
import cn.cccs7.cynexus.ai.infra.convert.ApplicationAnalysisTargetPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationAnalysisTargetRepositoryImpl implements ApplicationAnalysisTargetRepository {

    private final ApplicationAnalysisTargetBasicService applicationAnalysisTargetBasicService;

    public ApplicationAnalysisTargetRepositoryImpl(ApplicationAnalysisTargetBasicService applicationAnalysisTargetBasicService) {
        this.applicationAnalysisTargetBasicService = applicationAnalysisTargetBasicService;
    }

    @Override
    public List<ApplicationAnalysisTargetBO> listAll() {
        return ApplicationAnalysisTargetPOConverter.INSTANCE.toTargetList(applicationAnalysisTargetBasicService.listAll());
    }

    @Override
    public ApplicationAnalysisTargetBO findById(Long id) {
        return Optional.ofNullable(applicationAnalysisTargetBasicService.getById(id))
                .map(ApplicationAnalysisTargetPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(ApplicationAnalysisTargetBO target) {
        return applicationAnalysisTargetBasicService.create(ApplicationAnalysisTargetPOConverter.INSTANCE.toSource(target));
    }

    @Override
    public boolean update(ApplicationAnalysisTargetBO target) {
        return applicationAnalysisTargetBasicService.update(ApplicationAnalysisTargetPOConverter.INSTANCE.toSource(target));
    }

    @Override
    public boolean delete(Long id) {
        return applicationAnalysisTargetBasicService.delete(id);
    }
}
