package cn.cccs7.cynexus.core;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Executable architecture contract for the Core starter.
 *
 * The starter module intentionally has no test-framework dependency. Maven
 * compiles this contract with the module and the focused verification command
 * runs its main method with assertions enabled.
 */
public final class CynexusCoreApplicationArchitectureTest {

    private static final Set<String> FORBIDDEN_CONTEXT_PACKAGES = Set.of(
            "cn.cccs7.cynexus.ai",
            "cn.cccs7.cynexus.market",
            "cn.cccs7.cynexus.knowledgebase");

    private CynexusCoreApplicationArchitectureTest() {
    }

    public static void main(String[] args) {
        verifiesCoreOnlyComponentScan();
        verifiesSchedulingIsExplicitlyOwnedByCoreStarter();
    }

    static void verifiesCoreOnlyComponentScan() {
        SpringBootApplication application = CynexusCoreApplication.class
                .getAnnotation(SpringBootApplication.class);

        assert application != null : "CynexusCoreApplication must be a Spring Boot application";
        assert Arrays.equals(application.scanBasePackages(),
                new String[]{CynexusCoreApplication.CORE_COMPONENT_BASE_PACKAGE})
                : "Core starter must scan only " + CynexusCoreApplication.CORE_COMPONENT_BASE_PACKAGE;
        assert Arrays.stream(application.scanBasePackages())
                .noneMatch(CynexusCoreApplicationArchitectureTest::isForbiddenContext)
                : "Core starter must not scan AI, Market, or Knowledge Base contexts";
    }

    static void verifiesSchedulingIsExplicitlyOwnedByCoreStarter() {
        EnableScheduling scheduling = CoreSchedulingConfiguration.class
                .getAnnotation(EnableScheduling.class);
        assert scheduling != null : "Core scheduling must be enabled by the Core starter configuration";

        Set<String> schedulingConfigurationPackages = Arrays.stream(
                        CoreSchedulingConfiguration.class.getAnnotations())
                .map(Annotation::annotationType)
                .map(Class::getPackageName)
                .collect(Collectors.toSet());
        assert schedulingConfigurationPackages.stream()
                .noneMatch(CynexusCoreApplicationArchitectureTest::isForbiddenContext)
                : "Scheduling configuration must not belong to another context";
    }

    private static boolean isForbiddenContext(String packageName) {
        return FORBIDDEN_CONTEXT_PACKAGES.stream()
                .anyMatch(packageName::startsWith);
    }
}
