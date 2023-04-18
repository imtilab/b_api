package com.imtilab.bittracer.utils

import groovy.util.logging.Slf4j
import com.imtilab.bittracer.constant.Constants
import net.sf.json.JSONArray
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.time.DateFormatUtils

import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Slf4j
class DateUtil {

    /**
     * Format date into given format
     * @param date
     * @param pattern
     * @return formatted date
     */
    static LocalDateTime getFormattedDateTime(String date, String pattern) {
        if (date == null) {
            return date
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern)
            LocalDateTime.parse(CommonUtils.convertToPrimitive(date), formatter)
        } catch (Exception ex) {
            throw new Exception("Date paarse: The date($date) can't be format($pattern)")
        }
    }

    static List<LocalDateTime> getFormattedDateTime(List date, String pattern) {
        List localDateTimes = []
        date.each {
            localDateTimes.add(getFormattedDateTime(it, pattern))
        }
        localDateTimes
    }

    static LocalDate getFormattedDate(String date, String pattern) {
        if (date == null) {
            return date
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern)
            LocalDate.parse(CommonUtils.convertToPrimitive(date), formatter)
        } catch (Exception ex) {
            throw new Exception("Date paarse: The date($date) can't be format($pattern)")
        }
    }

    static List<LocalDate> getFormattedDate(List date, String pattern) {
        List localDates = []
        date.each {
            localDates.add(getFormattedDate(it, pattern))
        }
        localDates
    }

    /**
     *
     * @return timestamp
     */
    static def getTimestamp() {
        Date date = new Date()
        return DateFormatUtils.format(date, Constants.TIMESTAMP_FORMAT)
    }

    /**
     * localDateTime to String in specific pattern
     *
     * @param localDateTime
     * @param pattern
     * @return Date time in given pattern
     */
    static String getDate(LocalDateTime localDateTime, String pattern) {
        localDateTime.format(DateTimeFormatter.ofPattern(pattern))
    }

    /**
     * localDateTime to String in specific pattern
     *
     * @param localDateTimes
     * @param pattern
     * @return Date time in given pattern
     */
    static List<String> getDate(List<LocalDateTime> localDateTimes, String pattern) {
        List<String> dateTimes = []
        localDateTimes.each {
            dateTimes.add(getDate(it, pattern))
        }
        dateTimes
    }

    static def getDateByFormat(def date, String format) {
        if ((date instanceof JSONArray) && CollectionUtils.isNotEmpty(date) && !(date.get(0) instanceof String)) {
            throw new Exception("$date is neither Date nor list of Date")
        }
        getFormattedDateTime(date, format)
    }

    /**
     * Convert date to given zone date
     * @param date
     * @param currentZone
     * @param toZone
     * @return
     */
    static LocalDateTime convertDateToZone(LocalDateTime date, String currentZone, String toZone){
        ZonedDateTime zonedDateTime = date.atZone(ZoneId.of(currentZone))
        ZonedDateTime newZoneDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of(toZone))
        newZoneDateTime.toLocalDateTime()
    }

    static List generateStartAndEndDate() {
        List list = new ArrayList()
        Instant startDateInstant = Instant.now()
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.AUTO_GEN_START_DATE_DATE_FORMAT)
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"))
        def arr = sdf.format(startDateInstant.toDate()).split("T|Z|:")
        arr[2] = String.format("%02d", Integer.parseInt(arr[2]) + 2)
        def startDate = arr[0] + "T" + arr[1] + ":" + arr [2] + ":" + arr[3] + "Z"
        list.add(startDate)

        int days = 1
        Instant endDateInstant = startDateInstant.plus(days, ChronoUnit.DAYS)
        sdf = new SimpleDateFormat(Constants.AUTO_GEN_END_DATE_DATE_FORMAT)
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"))
        list.add(sdf.format(endDateInstant.toDate()))

        list.add(getTimeDiffFromNextMinInSec(startDateInstant))
        log.info("\nAUTO GEN START & END TIME: {}", list)
        list
    }

    static int getTimeDiffFromNextMinInSec(Instant instant) {
        SimpleDateFormat formatForSec = new SimpleDateFormat("ss")
        def sec = formatForSec.format(instant.toDate())
        int timeDiffInSec = 120 - Integer.parseInt(sec)
        return timeDiffInSec
    }

    static List<String> getOldToNewFormattedDate(List date,String oldPattern,String newPattern){
        List formattedDate = []
        date.each {
            formattedDate.add(getOldToNewFormattedDate(it,oldPattern,newPattern))
        }
        formattedDate
    }

    static String getOldToNewFormattedDate(String dateInput,String oldPattern,String newPattern){
        SimpleDateFormat formatInput = new SimpleDateFormat(oldPattern)
        SimpleDateFormat formatOutput = new SimpleDateFormat(newPattern)

        Date date = formatInput.parse(dateInput)
        return formatOutput.format(date).toString()
    }
}
