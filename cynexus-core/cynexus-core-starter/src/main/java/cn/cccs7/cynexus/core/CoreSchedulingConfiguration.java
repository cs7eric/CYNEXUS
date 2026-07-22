package cn.cccs7.cynexus.core;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Core scheduling after the Core-only component scan has been applied.
 * Scheduling inspects beans in the application context, so keeping this
 * configuration in the Core starter prevents unrelated contexts from being
 * enabled when the starter is used on its own.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class CoreSchedulingConfiguration {
}
