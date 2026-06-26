package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobBasicService;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobCategoryMappingBasicService;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobCategoryBasicService;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobRunHistoryBasicService;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobStatusBasicService;
import cn.cccs7.cynexus.core.infra.convert.SchedulerJobCategoryMappingPOConverter;
import cn.cccs7.cynexus.core.infra.convert.SchedulerJobCategoryPOConverter;
import cn.cccs7.cynexus.core.infra.convert.SchedulerJobPOConverter;
import cn.cccs7.cynexus.core.infra.convert.SchedulerJobRunHistoryPOConverter;
import cn.cccs7.cynexus.core.infra.convert.SchedulerJobStatusPOConverter;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SchedulerRepositoryImpl implements SchedulerRepository {

    private final SchedulerJobBasicService schedulerJobBasicService;
    private final SchedulerJobCategoryBasicService schedulerJobCategoryBasicService;
    private final SchedulerJobCategoryMappingBasicService schedulerJobCategoryMappingBasicService;
    private final SchedulerJobRunHistoryBasicService schedulerJobRunHistoryBasicService;
    private final SchedulerJobStatusBasicService schedulerJobStatusBasicService;
    public SchedulerRepositoryImpl(SchedulerJobBasicService schedulerJobBasicService,
                                   SchedulerJobCategoryBasicService schedulerJobCategoryBasicService,
                                   SchedulerJobCategoryMappingBasicService schedulerJobCategoryMappingBasicService,
                                   SchedulerJobRunHistoryBasicService schedulerJobRunHistoryBasicService,
                                   SchedulerJobStatusBasicService schedulerJobStatusBasicService) {
        this.schedulerJobBasicService = schedulerJobBasicService;
        this.schedulerJobCategoryBasicService = schedulerJobCategoryBasicService;
        this.schedulerJobCategoryMappingBasicService = schedulerJobCategoryMappingBasicService;
        this.schedulerJobRunHistoryBasicService = schedulerJobRunHistoryBasicService;
        this.schedulerJobStatusBasicService = schedulerJobStatusBasicService;
    }

    @Override
    public List<SchedulerJobBO> listJobs() {
        return SchedulerJobPOConverter.INSTANCE.toTargetList(schedulerJobBasicService.listAll());
    }

    @Override
    public List<SchedulerJobCategoryBO> listCategories() {
        return SchedulerJobCategoryPOConverter.INSTANCE.toTargetList(schedulerJobCategoryBasicService.listAll());
    }

    @Override
    public List<SchedulerJobCategoryMappingBO> listCategoryMappings() {
        return SchedulerJobCategoryMappingPOConverter.INSTANCE.toTargetList(schedulerJobCategoryMappingBasicService.listAll());
    }

    @Override
    public List<SchedulerJobRunHistoryBO> listRunHistory() {
        return SchedulerJobRunHistoryPOConverter.INSTANCE.toTargetList(schedulerJobRunHistoryBasicService.listAll());
    }

    @Override
    public List<SchedulerJobStatusBO> listStatuses() {
        return SchedulerJobStatusPOConverter.INSTANCE.toTargetList(schedulerJobStatusBasicService.listAll());
    }
}
