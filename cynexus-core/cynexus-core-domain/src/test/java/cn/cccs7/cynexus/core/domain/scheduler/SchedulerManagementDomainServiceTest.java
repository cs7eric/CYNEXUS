package cn.cccs7.cynexus.core.domain.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerRequestBO;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.service.scheduler.SchedulerManagementDomainService;
import cn.cccs7.cynexus.core.domain.service.scheduler.impl.SchedulerManagementDomainServiceImpl;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryCreateCommand;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryDeleteOutcome;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryMutationResult;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryUpdateCommand;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SchedulerManagementDomainServiceTest {

    @org.junit.jupiter.api.Test
    void ownsCategoryLifecycleRules() {
        InMemorySchedulerDomainService repository = new InMemorySchedulerDomainService();
        repository.createCategory(SchedulerJobCategoryBO.builder().code("market").label("Market").sortOrder(10).build());
        SchedulerManagementDomainService service = new SchedulerManagementDomainServiceImpl(repository);

        SchedulerCategoryMutationResult created = service.createCategory(
                new SchedulerCategoryCreateCommand("market", " Market Data ", null, null, " feeds "));

        assertThat(created.category().getCode()).isEqualTo("market_2");
        assertThat(created.category().getLabel()).isEqualTo("Market Data");
        assertThat(created.category().getIconHint()).isEqualTo("grid");
        assertThat(created.category().getSortOrder()).isEqualTo(20);
        assertThat(created.category().getDescription()).isEqualTo("feeds");

        repository.createCategoryMapping(SchedulerJobCategoryMappingBO.builder()
                .jobId(1L).categoryId(created.category().getId()).sortOrder(10).build());
        SchedulerCategoryMutationResult updated = service.updateCategory(created.category().getId(),
                new SchedulerCategoryUpdateCommand(null, "Market Jobs", "chart", null, " updated "));
        assertThat(updated.category().getCode()).isEqualTo("market_2");
        assertThat(updated.category().getLabel()).isEqualTo("Market Jobs");
        assertThat(updated.category().getIconHint()).isEqualTo("chart");
        assertThat(updated.category().getDescription()).isEqualTo("updated");
        assertThat(updated.mappingCount()).isEqualTo(1);

        SchedulerCategoryDeleteOutcome deleted = service.deleteCategory(created.category().getId());

        assertThat(deleted.deleted()).isTrue();
        assertThat(deleted.deletedMappingCount()).isEqualTo(1);
        assertThat(repository.findCategoryById(created.category().getId()).getDeletedAt()).isNotNull();
        assertThat(repository.listCategoryMappings().getFirst().getDeletedAt()).isNotNull();
    }

    private static final class InMemorySchedulerDomainService implements SchedulerDomainService {
        private long categoryId = 1L;
        private long mappingId = 1L;
        private final List<SchedulerJobCategoryBO> categories = new ArrayList<>();
        private final List<SchedulerJobCategoryMappingBO> mappings = new ArrayList<>();

        @Override public List<SchedulerJobBO> listJobs() { return List.of(); }
        @Override public SchedulerJobBO findJobById(Long id) { return null; }
        @Override public SchedulerJobBO findJobByCode(String code) { return null; }
        @Override public Long createJob(SchedulerJobBO job) { return null; }
        @Override public boolean updateJob(SchedulerJobBO job) { return false; }
        @Override public boolean updateJobEnabled(Long id, Boolean enabled) { return false; }
        @Override public boolean deleteJob(Long id) { return false; }
        @Override public List<SchedulerJobCategoryBO> listCategories() { return categories; }
        @Override public SchedulerJobCategoryBO findCategoryById(Long id) {
            return categories.stream().filter(category -> id.equals(category.getId())).findFirst().orElse(null);
        }
        @Override public Long createCategory(SchedulerJobCategoryBO category) {
            category.setId(categoryId++);
            categories.add(category);
            return category.getId();
        }
        @Override public boolean updateCategory(SchedulerJobCategoryBO category) {
            SchedulerJobCategoryBO existing = findCategoryById(category.getId());
            if (existing == null) {
                return false;
            }
            categories.remove(existing);
            categories.add(category);
            return true;
        }
        @Override public boolean deleteCategory(Long id) {
            SchedulerJobCategoryBO existing = findCategoryById(id);
            if (existing == null) {
                return false;
            }
            existing.setDeletedAt(OffsetDateTime.now());
            return true;
        }
        @Override public List<SchedulerJobCategoryMappingBO> listCategoryMappings() { return mappings; }
        @Override public List<SchedulerJobCategoryMappingBO> listCategoryMappingsByJobId(Long jobId) {
            return mappings.stream().filter(mapping -> jobId.equals(mapping.getJobId())).toList();
        }
        @Override public SchedulerJobCategoryMappingBO findCategoryMappingById(Long id) { return null; }
        @Override public Long createCategoryMapping(SchedulerJobCategoryMappingBO mapping) {
            mapping.setId(mappingId++);
            mappings.add(mapping);
            return mapping.getId();
        }
        @Override public boolean updateCategoryMapping(SchedulerJobCategoryMappingBO mapping) { return false; }
        @Override public boolean deleteCategoryMapping(Long id) {
            mappings.stream().filter(mapping -> id.equals(mapping.getId())).findFirst()
                    .ifPresent(mapping -> mapping.setDeletedAt(OffsetDateTime.now()));
            return true;
        }
        @Override public List<SchedulerJobRunHistoryBO> listRunHistory() { return List.of(); }
        @Override public List<SchedulerJobRunHistoryBO> listRunHistoryByJobId(Long jobId, Integer limit) { return List.of(); }
        @Override public List<SchedulerJobRunHistoryBO> listRunHistorySince(OffsetDateTime startedAt) { return List.of(); }
        @Override public SchedulerJobRunHistoryBO findRunHistoryById(Long id) { return null; }
        @Override public Long createRunHistory(SchedulerJobRunHistoryBO runHistory) { return null; }
        @Override public boolean updateRunHistory(SchedulerJobRunHistoryBO runHistory) { return false; }
        @Override public boolean deleteRunHistory(Long id) { return false; }
        @Override public List<SchedulerJobStatusBO> listStatuses() { return List.of(); }
        @Override public SchedulerJobStatusBO findStatusByJobId(Long jobId) { return null; }
        @Override public SchedulerJobStatusBO findStatusById(Long id) { return null; }
        @Override public Long createStatus(SchedulerJobStatusBO status) { return null; }
        @Override public boolean updateStatus(SchedulerJobStatusBO status) { return false; }
        @Override public boolean updateStatusEnabled(Long id, Boolean enabled) { return false; }
        @Override public boolean updateStatusRunning(Long id, Boolean running, OffsetDateTime schedulerStartedAt, OffsetDateTime stoppedAt) { return false; }
        @Override public boolean updateStatusRunResult(Long id, OffsetDateTime lastRunAt, Boolean lastRunOk, String lastRunStatus, String lastRunErrorMessage, BigDecimal lastDurationSeconds) { return false; }
        @Override public boolean updateStatusRunResult(Long id, OffsetDateTime lastRunAt, Boolean lastRunOk, String lastRunStatus, String lastRunErrorMessage, BigDecimal lastDurationSeconds, Integer lastTargetsProcessed, String progress, String extra, String remark) { return false; }
        @Override public boolean updateStatusProgress(Long id, Boolean running, OffsetDateTime lastRunAt, String lastRunStatus, String progress, String extra, String remark) { return false; }
        @Override public boolean deleteStatus(Long id) { return false; }
        @Override public Map<String, Object> dataJobExecutorHealth() { return Map.of(); }
        @Override public Map<String, Object> listDataJobs() { return Map.of(); }
        @Override public Map<String, Object> getDataJob(String jobKey) { return Map.of(); }
        @Override public DataJobTriggerResultBO triggerDataJob(String jobKey, DataJobTriggerRequestBO request) { return null; }
        @Override public Map<String, Object> getDataJobRun(String runId) { return Map.of(); }
        @Override public Map<String, Object> startSocialLogin(String platform, String profileName) { return Map.of(); }
        @Override public Map<String, Object> getSocialLogin(String sessionId) { return Map.of(); }
        @Override public Map<String, Object> completeSocialLogin(String sessionId) { return Map.of(); }
        @Override public Map<String, Object> closeSocialLogin(String sessionId) { return Map.of(); }
    }
}
