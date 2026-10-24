package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionModels.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import static org.junit.jupiter.api.Assertions.*;

class FoodRecognitionRepositoryImplTest {
    @Test void startsWithoutALegacyJacksonMapperBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().registerSingleton("jdbcTemplate", new RecordingJdbc());
            context.register(FoodRecognitionRepositoryImpl.class);
            context.refresh();
            assertNotNull(context.getBean(FoodRecognitionRepositoryImpl.class));
        }
    }

    @Test void publicReadsAreOwnerScopedAndExcludeOriginalPhotosAndPreparedArtwork() {
        RecordingJdbc jdbc = new RecordingJdbc();
        FoodRecognitionRepositoryImpl repository = new FoodRecognitionRepositoryImpl(jdbc);
        repository.find(12, "owner-a");
        repository.list("owner-a", 30);
        assertEquals(List.of(12L, "owner-a"), List.of(jdbc.queries.get(0).args()));
        for (Query query : jdbc.queries) {
            assertTrue(query.sql().contains("owner_key=?"));
            assertTrue(query.sql().contains("null::jsonb as command_json,null::jsonb as prepared_json"));
        }
    }

    @Test void expiredRetryInputsCannotBeRequeuedAndCleanupErasesAllPhotoCopies() {
        RecordingJdbc jdbc = new RecordingJdbc();
        FoodRecognitionRepositoryImpl repository = new FoodRecognitionRepositoryImpl(jdbc);
        assertNull(repository.retry(12, "owner-a"));
        assertTrue(jdbc.queries.get(0).sql().contains("completed_at>=now()-interval '7 days'"));
        assertTrue(jdbc.queries.get(0).sql().contains("command_json is not null"));
        assertEquals(1, repository.purgeExpiredInputs());
        assertTrue(jdbc.updates.get(0).sql().contains("command_json=null,prepared_json=null"));
        assertTrue(jdbc.updates.get(0).sql().contains("task_status='failed' and completed_at<now()-interval '7 days'"));
    }

    @Test void aReplacedWorkerClaimCannotCommitOrReleaseAnotherWorkersTask() {
        RecordingJdbc jdbc = new RecordingJdbc();
        FoodRecognitionRepositoryImpl repository = new FoodRecognitionRepositoryImpl(jdbc);
        Task task = new Task(12, new Identity("owner", "tenant", "principal"), "fingerprint", null,
                "running", "save", "45", null, 0, 0, "expired-claim", null, null, null,
                OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now(), null);
        repository.awaitAi(task, "45");
        assertTrue(jdbc.updates.get(0).sql().contains("claimed_by=? and claimed_until>now()"));
        jdbc.updateCount = 0;
        assertThrows(IllegalStateException.class, () -> repository.succeed(task, "food-42", "Oats"));
        assertEquals("expired-claim", jdbc.updates.get(1).args()[3]);
        assertTrue(jdbc.updates.get(1).sql().contains("command_json=null,prepared_json=null"));
    }

    private record Query(String sql, Object[] args) { }
    private static final class RecordingJdbc extends JdbcTemplate {
        final List<Query> queries = new ArrayList<>();
        final List<Query> updates = new ArrayList<>();
        int updateCount = 1;
        @Override public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            queries.add(new Query(sql, args));
            return List.of();
        }
        @Override public int update(String sql, Object... args) {
            updates.add(new Query(sql, args));
            return updateCount;
        }
        @Override public int update(String sql) { return update(sql, new Object[0]); }
    }
}
