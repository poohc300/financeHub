package com.example.financeHub.krx.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class KrxCommonUtil {
    
    /**
     * 최근 영업일을 반환
     * @return
     */
   public static LocalDate getLastTradingDay() {
       LocalDate date = LocalDate.now().minusDays(1); // 전일 기준 (KRX 당일 데이터는 19시 이후 게시)

       switch (date.getDayOfWeek()) {
           case DayOfWeek.SATURDAY: return date.minusDays(1); // 금요일
           case DayOfWeek.SUNDAY:   return date.minusDays(2); // 금요일
           default:                  return date;
       }
   }
   /**
    * YYYYMMDD 포맷으로 변환
    * @return
    */
   public static String formatDate(LocalDate date) {
       return date.format(DateTimeFormatter.BASIC_ISO_DATE);
   }
   
   /**
    * null-safe 하게 Map에서 String 값 꺼내기
    */
   public static String getString(Map<String, Object> map, String key) {
       Object value = map.get(key);
       return value != null ? value.toString() : "";
   }
   
   
}
