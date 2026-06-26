package com.studypot.aistudyleader.global.ratelimit;

/**
 * AI 팀장 채팅 일일 한도의 현재 사용 현황(증가 없이 조회).
 *
 * @param dailyLimit   플랜별 일일 한도
 * @param used         현재 윈도우에서 사용한 횟수(0..dailyLimit)
 * @param remaining    남은 횟수(0 이상)
 * @param resetSeconds 윈도우 리셋까지 남은 초(없으면 0)
 */
public record AiConversationQuotaView(long dailyLimit, long used, long remaining, long resetSeconds) {
}
