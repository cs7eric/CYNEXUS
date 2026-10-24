package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.food.FoodDTO;
import cn.cccs7.cynexus.core.application.service.FoodAppService;
import cn.cccs7.cynexus.core.application.service.FoodIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/food")
public class FoodController {
    private final FoodAppService app;
    private final FoodIdentityResolver identity;
    public FoodController(FoodAppService app,FoodIdentityResolver identity) { this.app=app; this.identity=identity; }
    @GetMapping("/assets")
    public Result<List<FoodDTO.FoodAsset>> assets() { return Result.success(app.assets()); }
    @GetMapping("/catalog")
    public Result<List<FoodDTO.Food>> catalog(HttpServletRequest h,@RequestParam(required=false) String q,@RequestParam(defaultValue="20") int limit) { return Result.success(app.search(identity.owner(h),q,limit)); }
    @GetMapping("/days/{date}")
    public Result<FoodDTO.Day> day(HttpServletRequest h,@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date) { return Result.success(app.day(identity.owner(h),date)); }
    @GetMapping("/history")
    public Result<List<FoodDTO.Day>> history(HttpServletRequest h,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate start,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate end) { return Result.success(app.history(identity.owner(h),start,end)); }
    @PostMapping("/days/{date}/items")
    public Result<FoodDTO.Day> add(HttpServletRequest h,@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,@RequestBody FoodDTO.EntryRequest r) { return Result.success(app.add(identity.owner(h),date,r)); }
    @PostMapping("/days/{date}/fridge-items")
    public Result<FoodDTO.Day> addFromFridge(HttpServletRequest h,@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,@RequestBody FoodDTO.FridgeMealRequest r) { return Result.success(app.addFromFridge(identity.owner(h),date,r)); }
    @PutMapping("/days/{date}/items/{id}")
    public Result<FoodDTO.Day> update(HttpServletRequest h,@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,@PathVariable String id,@RequestBody FoodDTO.EditRequest r) { return Result.success(app.update(identity.owner(h),date,id,r)); }
    @DeleteMapping("/days/{date}/items/{id}")
    public Result<FoodDTO.Day> delete(HttpServletRequest h,@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,@PathVariable String id) { return Result.success(app.delete(identity.owner(h),date,id)); }
    @PostMapping("/days/{date}/complete")
    public Result<FoodDTO.Day> complete(HttpServletRequest h,@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,@RequestBody(required=false) FoodDTO.DayStatusRequest r) { return Result.success(app.status(identity.owner(h),date,true,r==null?null:r.timezone())); }
    @PostMapping("/days/{date}/reopen")
    public Result<FoodDTO.Day> reopen(HttpServletRequest h,@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,@RequestBody(required=false) FoodDTO.DayStatusRequest r) { return Result.success(app.status(identity.owner(h),date,false,r==null?null:r.timezone())); }
    @GetMapping("/goals")
    public Result<List<FoodDTO.Goal>> goals(HttpServletRequest h) { return Result.success(app.goals(identity.owner(h))); }
    @PostMapping("/goals")
    public Result<List<FoodDTO.Goal>> saveGoal(HttpServletRequest h,@RequestBody FoodDTO.GoalRequest r) { return Result.success(app.saveGoal(identity.owner(h),r)); }
    @GetMapping("/custom-foods")
    public Result<List<FoodDTO.Food>> custom(HttpServletRequest h) { return Result.success(app.customFoods(identity.owner(h))); }
    @PostMapping("/custom-foods")
    public Result<FoodDTO.Food> saveCustom(HttpServletRequest h,@RequestBody FoodDTO.CustomRequest r) { return Result.success(app.saveCustom(identity.owner(h),r)); }
    @GetMapping(value="/custom-foods/{id}/asset", produces=MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> customAsset(HttpServletRequest h,@PathVariable String id) {
        byte[] asset = app.customAsset(identity.owner(h), id);
        if (asset == null || asset.length == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noCache().cachePrivate()).body(asset);
    }
    @GetMapping("/favorites")
    public Result<List<FoodDTO.Food>> favorites(HttpServletRequest h) { return Result.success(app.favorites(identity.owner(h))); }
    @PostMapping("/favorites")
    public Result<FoodDTO.Food> saveFavorite(HttpServletRequest h,@RequestBody FoodDTO.FavoriteRequest r) { return Result.success(app.saveFavorite(identity.owner(h),r)); }
    @DeleteMapping("/favorites/{id}")
    public Result<List<FoodDTO.Food>> deleteFavorite(HttpServletRequest h,@PathVariable String id) { return Result.success(app.deleteFavorite(identity.owner(h),id)); }
}
