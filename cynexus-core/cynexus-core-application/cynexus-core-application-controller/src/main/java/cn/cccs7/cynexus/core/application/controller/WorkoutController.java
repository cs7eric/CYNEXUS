package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.workout.WorkoutDTO;
import cn.cccs7.cynexus.core.application.service.FoodIdentityResolver;
import cn.cccs7.cynexus.core.application.service.WorkoutAppService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/workout")
public class WorkoutController {
    private final WorkoutAppService app;
    private final FoodIdentityResolver identity;
    public WorkoutController(WorkoutAppService app, FoodIdentityResolver identity) { this.app = app; this.identity = identity; }

    @GetMapping
    public Result<WorkoutDTO.Snapshot> snapshot(HttpServletRequest request) {
        return Result.success(app.snapshot(identity.owner(request)));
    }

    @PutMapping("/plan")
    public Result<WorkoutDTO.Snapshot> savePlan(HttpServletRequest request, @RequestBody WorkoutDTO.PlanRequest body) {
        return Result.success(app.savePlan(identity.owner(request), body));
    }

    @PutMapping("/sessions")
    public Result<WorkoutDTO.Snapshot> saveSession(HttpServletRequest request, @RequestBody WorkoutDTO.SessionRequest body) {
        return Result.success(app.saveSession(identity.owner(request), body));
    }

    @DeleteMapping("/sessions/{id}")
    public Result<WorkoutDTO.Snapshot> deleteSession(HttpServletRequest request, @PathVariable String id,
                                                     @RequestParam(value = "idempotencyKey", required = false) String idempotencyKey,
                                                     @RequestHeader(value = "Idempotency-Key", required = false) String headerKey,
                                                     @RequestBody(required = false) WorkoutDTO.DeleteRequest body) {
        return Result.success(app.deleteSession(identity.owner(request), id, firstKey(body == null ? null : body.idempotencyKey(), idempotencyKey, headerKey)));
    }

    @PutMapping("/measurements")
    public Result<WorkoutDTO.Snapshot> saveMeasurement(HttpServletRequest request, @RequestBody WorkoutDTO.MeasurementRequest body) {
        return Result.success(app.saveMeasurement(identity.owner(request), body));
    }

    @DeleteMapping("/measurements/{id}")
    public Result<WorkoutDTO.Snapshot> deleteMeasurement(HttpServletRequest request, @PathVariable String id,
                                                         @RequestParam(value = "idempotencyKey", required = false) String idempotencyKey,
                                                         @RequestHeader(value = "Idempotency-Key", required = false) String headerKey,
                                                         @RequestBody(required = false) WorkoutDTO.DeleteRequest body) {
        return Result.success(app.deleteMeasurement(identity.owner(request), id, firstKey(body == null ? null : body.idempotencyKey(), idempotencyKey, headerKey)));
    }

    private static String firstKey(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return null;
    }
}
