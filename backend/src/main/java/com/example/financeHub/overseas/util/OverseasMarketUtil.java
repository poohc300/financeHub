package com.example.financeHub.overseas.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class OverseasMarketUtil {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime MARKET_OPEN = LocalTime.of(23, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(6, 0);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 미국 시장이 현재 열려있는지 확인 (23:30 ~ 06:00 KST, 주말 제외)
     * - 월~금 23:30 ~ 다음날 06:00
     * - 토요일 06:00 이전: 금요일 밤 장 포함 (open)
     * - 토요일 06:00 이후 ~ 일요일 23:29: 닫힘
     */
    public static boolean isMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        LocalTime time = now.toLocalTime();
        DayOfWeek day = now.getDayOfWeek();

        // 일요일 23:30 이전은 완전 휴장
        if (day == DayOfWeek.SUNDAY && time.isBefore(MARKET_OPEN)) return false;
        // 토요일 06:00 이후는 마감 (금요일 밤 장 종료)
        if (day == DayOfWeek.SATURDAY && !time.isBefore(MARKET_CLOSE)) return false;

        boolean isNightSession = !time.isBefore(MARKET_OPEN);  // 23:30 이후
        boolean isEarlyMorning = time.isBefore(MARKET_CLOSE);  // 06:00 이전

        return isNightSession || isEarlyMorning;
    }

    /**
     * 스케줄러 수집 기준일 (07:00 KST 수집 → 전날이 미국 거래일)
     */
    public static String getCollectionDate() {
        return LocalDate.now(KST).minusDays(1).format(DATE_FMT);
    }

    public static String formatDate(LocalDate date) {
        return date.format(DATE_FMT);
    }
}
