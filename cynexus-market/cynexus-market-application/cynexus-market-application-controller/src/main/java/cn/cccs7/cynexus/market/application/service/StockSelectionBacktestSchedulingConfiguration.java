package cn.cccs7.cynexus.market.application.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class StockSelectionBacktestSchedulingConfiguration {
}
