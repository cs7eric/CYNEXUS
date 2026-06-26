package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisDailySnapshotBO;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisDailySnapshotRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisDailySnapshotBasicService;
import cn.cccs7.cynexus.ai.infra.convert.ApplicationAnalysisDailySnapshotPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationAnalysisDailySnapshotRepositoryImpl implements ApplicationAnalysisDailySnapshotRepository {

    private final ApplicationAnalysisDailySnapshotBasicService applicationAnalysisDailySnapshotBasicService;

    public ApplicationAnalysisDailySnapshotRepositoryImpl(
            ApplicationAnalysisDailySnapshotBasicService applicationAnalysisDailySnapshotBasicService) {
        this.applicationAnalysisDailySnapshotBasicService = applicationAnalysisDailySnapshotBasicService;
    }

    @Override
    public List<ApplicationAnalysisDailySnapshotBO> listAll() {
        return ApplicationAnalysisDailySnapshotPOConverter.INSTANCE
                .toTargetList(applicationAnalysisDailySnapshotBasicService.listAll());
    }

    @Override
    public ApplicationAnalysisDailySnapshotBO findById(Long id) {
        return Optional.ofNullable(applicationAnalysisDailySnapshotBasicService.getById(id))
                .map(ApplicationAnalysisDailySnapshotPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(ApplicationAnalysisDailySnapshotBO snapshot) {
        return applicationAnalysisDailySnapshotBasicService.create(
                ApplicationAnalysisDailySnapshotPOConverter.INSTANCE.toSource(snapshot));
    }

    @Override
    public boolean update(ApplicationAnalysisDailySnapshotBO snapshot) {
        return applicationAnalysisDailySnapshotBasicService.update(
                ApplicationAnalysisDailySnapshotPOConverter.INSTANCE.toSource(snapshot));
    }

    @Override
    public boolean delete(Long id) {
        return applicationAnalysisDailySnapshotBasicService.delete(id);
    }
}
