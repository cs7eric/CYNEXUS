package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import java.time.LocalDate;
import java.util.List;

public interface ApplicationAnalysisPageRepository {

    ApplicationAnalysisPageBO.Config getDefaultConfig();

    List<ApplicationAnalysisPageBO.Target> listTargets();

    ApplicationAnalysisPageBO.Target findTarget(String targetKey);

    ApplicationAnalysisPageBO.Config saveTargets(ApplicationAnalysisPageBO.SaveTargetsCommand command);

    ApplicationAnalysisPageBO.CurrentResult findCurrentResult(String targetKey);

    List<ApplicationAnalysisPageBO.HistoryResult> listHistory(String targetKey, int limit);

    List<ApplicationAnalysisPageBO.DailySnapshot> listDailySnapshots(String targetKey, int limit);

    ApplicationAnalysisPageBO.DailySnapshot findDailySnapshot(String targetKey, LocalDate tradeDate);
}
