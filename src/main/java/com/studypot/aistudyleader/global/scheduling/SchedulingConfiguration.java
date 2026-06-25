package com.studypot.aistudyleader.global.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 애플리케이션 전역 스케줄링/비동기 실행을 활성화한다. 개별 스케줄러 빈은 자체 조건으로 활성화 여부를 제어한다.
 * 비동기(@Async)는 전원 회고 완료 시 주차 리포트 생성을 백그라운드로 돌려 회고 제출 응답을 막지 않기 위함이다.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableAsync
class SchedulingConfiguration {
}
