package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.core.application.dto.FoodImageMattingResponse;
import cn.cccs7.cynexus.core.application.service.FoodImageMattingAppService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Food image utility endpoint shared by the custom food and asset flows. */
@RestController
@RequestMapping("/api/core/food")
public class FoodImageMattingController {

    private final FoodImageMattingAppService appService;

    public FoodImageMattingController(FoodImageMattingAppService appService) {
        this.appService = appService;
    }

    @PostMapping(value = "/matting", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> matte(@RequestParam("file") MultipartFile file) {
        FoodImageMattingResponse response = appService.matte(file);
        byte[] content = response.content();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentLength(content.length)
                .body(content);
    }
}
