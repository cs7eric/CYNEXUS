package cn.cccs7.cynexus.core.domain.food.recognition;

import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionModels.*;
import java.util.List;

public interface FoodRecognitionRepository {
    Task create(Identity identity, Command command, String fingerprint);
    Task find(long id, String owner);
    List<Task> list(String owner, int limit);
    Task retry(long id, String owner);
    int purgeExpiredInputs();
    Task claimNext();
    void awaitAi(Task task, String aiTaskId);
    boolean prepare(Task task, Prepared prepared);
    Task lockForCompletion(Task task);
    void succeed(Task task, String foodId, String foodName);
    void fail(Task task, String error, boolean terminal, int retryDelaySeconds);
}
