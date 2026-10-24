package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.food.FoodRecognitionDTO;
import cn.cccs7.cynexus.core.application.service.FoodIdentityResolver;
import cn.cccs7.cynexus.core.application.service.FoodRecognitionAppService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/core/food/recognition-tasks")
public class FoodRecognitionController {
    private final FoodRecognitionAppService app;
    private final FoodIdentityResolver identity;
    public FoodRecognitionController(FoodRecognitionAppService app, FoodIdentityResolver identity) {
        this.app = app;
        this.identity = identity;
    }

    @PostMapping
    public ResponseEntity<Result<FoodRecognitionDTO.Task>> enqueue(HttpServletRequest http,
            @RequestBody FoodRecognitionDTO.Request request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Result.success(app.enqueue(identity.resolve(http), request)));
    }

    @GetMapping
    public Result<List<FoodRecognitionDTO.Task>> list(HttpServletRequest http, @RequestParam(defaultValue = "30") int limit) {
        return Result.success(app.list(identity.owner(http), limit));
    }

    @GetMapping("/{id}")
    public Result<FoodRecognitionDTO.Task> get(HttpServletRequest http, @PathVariable long id) {
        FoodRecognitionDTO.Task task = app.get(id, identity.owner(http));
        if (task == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Food recognition task not found");
        return Result.success(task);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Result<FoodRecognitionDTO.Task>> retry(HttpServletRequest http, @PathVariable long id) {
        String owner = identity.owner(http);
        if (app.get(id, owner) == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Food recognition task not found");
        FoodRecognitionDTO.Task task = app.retry(id, owner);
        if (task == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Only failed food recognition tasks can be retried");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Result.success(task));
    }
}
