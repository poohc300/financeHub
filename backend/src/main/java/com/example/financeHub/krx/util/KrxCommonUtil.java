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
       LocalDate date = LocalDate.now();
       
       switch (date.getDayOfWeek()) {
       	case DayOfWeek.MONDAY: return date.minusDays(3);
       	case DayOfWeek.SUNDAY: return date.minusDays(2);
       	default: return date.minusDays(1);
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
