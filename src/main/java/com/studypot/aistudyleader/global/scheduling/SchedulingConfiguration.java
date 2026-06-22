package com.studypot.aistudyleader.global.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 애플리케이션 전역 스케줄링을 활성화한다. 개별 스케줄러 빈은 자체 조건으로 활성화 여부를 제어한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class SchedulingConfiguration {
}
