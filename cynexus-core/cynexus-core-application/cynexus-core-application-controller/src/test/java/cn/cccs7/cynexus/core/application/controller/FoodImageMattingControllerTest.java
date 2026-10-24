package cn.cccs7.cynexus.core.application.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.core.application.service.FoodImageMattingAppService;
import cn.cccs7.cynexus.core.domain.food.FoodImageMattingDomainService;
import cn.cccs7.cynexus.core.domain.food.FoodImageMattingPort;
import cn.cccs7.cynexus.core.domain.food.FoodImageMattingResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;

class FoodImageMattingControllerTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    @Test
    void returnsPngForUploadedMultipartImage() {
        FoodImageMattingPort port = (content, fileName, contentType) -> {
            assertThat(content).containsExactly("camera-image".getBytes());
            assertThat(fileName).isEqualTo("meal.jpg");
            assertThat(contentType).isEqualTo("image/jpeg");
            return new FoodImageMattingResult(PNG, "image/png");
        };
        FoodImageMattingAppService appService = new FoodImageMattingAppService(new FoodImageMattingDomainService(port));
        FoodImageMattingController controller = new FoodImageMattingController(appService);

        var response = controller.matte(new MockMultipartFile("file", "meal.jpg", "image/jpeg", "camera-image".getBytes()));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getBody()).containsExactly(PNG);
    }

    @Test
    void rejectsEmptyMultipartImage() {
        FoodImageMattingAppService appService = new FoodImageMattingAppService(
                new FoodImageMattingDomainService((content, fileName, contentType) -> new FoodImageMattingResult(PNG, "image/png")));
        FoodImageMattingController controller = new FoodImageMattingController(appService);

        assertThatThrownBy(() -> controller.matte(new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0])))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("An image is required");
    }

    @Test
    void rejectsImagesLargerThanTheUploadLimit() {
        FoodImageMattingAppService appService = new FoodImageMattingAppService(
                new FoodImageMattingDomainService((content, fileName, contentType) -> new FoodImageMattingResult(PNG, "image/png")));
        FoodImageMattingController controller = new FoodImageMattingController(appService);
        byte[] oversized = new byte[12 * 1024 * 1024 + 1];

        assertThatThrownBy(() -> controller.matte(new MockMultipartFile("file", "large.jpg", "image/jpeg", oversized)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("smaller than 12 MB");
    }
}
