package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import cn.cccs7.cynexus.core.api.dto.food.FoodRecognitionDTO;
import cn.cccs7.cynexus.core.application.service.FoodIdentityResolver;
import cn.cccs7.cynexus.core.application.service.FoodRecognitionAppService;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class FoodRecognitionControllerTest {
    private static final String SECRET = "food-recognition-controller-test-secret";

    @Test void unsignedRequestsCannotSubmitOrReadRecognitionTasks() throws Exception {
        FoodRecognitionAppService app = new FoodRecognitionAppService(null);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FoodRecognitionController(app, new FoodIdentityResolver(SECRET))).build();
        assertEquals(401, mvc.perform(post("/api/core/food/recognition-tasks").contentType("application/json").content("{}"))
                .andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(get("/api/core/food/recognition-tasks/12")).andReturn().getResponse().getStatus());
    }

    @Test void submissionReturnsAcceptedImmediatelyAndReadsUseTheSignedOwner() throws Exception {
        AtomicReference<FoodIdentityResolver.Identity> submitted = new AtomicReference<>();
        AtomicReference<String> readOwner = new AtomicReference<>();
        FoodRecognitionDTO.Task task = new FoodRecognitionDTO.Task("12", "12", true, "queued", "submit", "FOOD_RECOGNITION",
                "food.nutrition-recognition", "/api/core/food/recognition-tasks/12", null, null, null, null, false, 0, 0, null, null, null);
        FoodRecognitionAppService app = new FoodRecognitionAppService(null) {
            @Override public FoodRecognitionDTO.Task enqueue(FoodIdentityResolver.Identity identity, FoodRecognitionDTO.Request request) {
                submitted.set(identity);
                return task;
            }
            @Override public FoodRecognitionDTO.Task get(long id, String owner) {
                readOwner.set(owner);
                return null;
            }
        };
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FoodRecognitionController(app, new FoodIdentityResolver(SECRET))).build();
        String signature = TrustedPrincipalSigner.sign("user-a", "tenant-a", SECRET);
        var response = mvc.perform(post("/api/core/food/recognition-tasks").contentType("application/json").content("{}")
                .header("X-Cynexus-Principal", "user-a").header("X-Cynexus-Tenant", "tenant-a")
                .header("X-Cynexus-Identity-Signature", signature)).andReturn().getResponse();
        assertEquals(202, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"status\":\"queued\""));
        assertEquals("8:tenant-a6:user-a", submitted.get().owner());
        assertEquals(404, mvc.perform(get("/api/core/food/recognition-tasks/12")
                .header("X-Cynexus-Principal", "user-a").header("X-Cynexus-Tenant", "tenant-a")
                .header("X-Cynexus-Identity-Signature", signature)).andReturn().getResponse().getStatus());
        assertEquals("8:tenant-a6:user-a", readOwner.get());
    }

    @Test void invalidSubmissionReturnsTheValidationMessage() throws Exception {
        FoodRecognitionAppService app = new FoodRecognitionAppService(null) {
            @Override public FoodRecognitionDTO.Task enqueue(FoodIdentityResolver.Identity identity, FoodRecognitionDTO.Request request) {
                throw new IllegalArgumentException("A nutrition label image is required");
            }
        };
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FoodRecognitionController(app, new FoodIdentityResolver(SECRET)))
                .setControllerAdvice(new FoodErrorAdvice()).build();
        var response = mvc.perform(post("/api/core/food/recognition-tasks").contentType("application/json").content("{}")
                .header("X-Cynexus-Principal", "user-a").header("X-Cynexus-Tenant", "tenant-a")
                .header("X-Cynexus-Identity-Signature", TrustedPrincipalSigner.sign("user-a", "tenant-a", SECRET)))
                .andReturn().getResponse();
        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("A nutrition label image is required"));
        var advice = FoodErrorAdvice.class.getAnnotation(org.springframework.web.bind.annotation.RestControllerAdvice.class);
        assertTrue(java.util.Arrays.asList(advice.assignableTypes()).contains(FoodRecognitionController.class));
    }
}
