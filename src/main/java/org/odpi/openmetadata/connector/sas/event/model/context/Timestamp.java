//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model.context;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;

/**
 * This class provides static utility methods for parsing and formatting timestamps (date/time
 * strings), time, and date strings. Formatting uses W3C / ISO 8601 timestamps, in the form
 * "2013-10-02T15:30:00.00Z" format (timestamps) "yyyy-MM-ddZ" (dates) and "HH:mm:ss.SSSZ" (time).
 * When parsing, the fractional part of the seconds value is optional, as is the time zone value.
 * <p>
 * Use the <code>parse*As*(String)</code> methods to parse such strings into a {@link Date} or
 * Java 8 {@link ZonedDateTime} value.
 * <p>
 * Use the <code>timestamp(*), timeString(*)</code>, and <code>dateString(*)</code>
 * methods to format a <code>Date</code> or
 * Java 8 <code>ZonedDateTime</code> or long in this format.
 * <p>
 * All the methods use or result in UTC (GMT) time zone dates.
 * <p>
 * Also includes methods for parsing and formatting HTTP Timestamps in the form
 * "EEE, dd MMM yyyy HH:mm:ss zzz", as described in RFC-7231.
 * This format requires 2-digit day-of-month and 4-digit years and the GMT time zone.
 * Any milliseconds will be truncated and ignored when formatting.
 * <p>
 * Use the <code>parseHttpTimestampAs*(String)</code> methods to parse
 * such strings into a {@link Date} or Java 8 {@link ZonedDateTime} value.
 * <p>
 * Use the <code>httpTimestamp(*)</code> methods to format a <code>Date</code> or
 * Java 8 <code>ZonedDateTime</code> or long in this format. This format will
 * always output "GMT" time zone dates.
 * <p>
 * Use <code>truncateDateToSeconds(Date)</code> to truncate a {@link Date}
 * that might include milliseconds into a corresponding {@link Date} truncated
 * to the previous whole second value.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
 *
 */
public class Timestamp {

    // We try to parse with the Java 8 DateTimeFormatter parser,
    // but also add extra custom parsers for some cases we support,
    // such as missing .SSS.
    private static final DateTimeFormatter TIME_PARSERS[] = {
            DateTimeFormatter.ISO_OFFSET_TIME,
            DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ISO_TIME,
            DateTimeFormatter.ofPattern("HH:mm:ssZ"),
            DateTimeFormatter.ofPattern("HH:mm:ss")
    };
    private static final DateTimeFormatter DATE_PARSERS[] = {
            DateTimeFormatter.ISO_OFFSET_DATE,
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-ddZ")
    };
    private static final DateTimeFormatter TIMESTAMP_PARSERS[] = {
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
    };
    // NOTE: DateTimeFormatter.RFC_1123_DATE_TIME incorrectly uses
    // a single digit day-of-month.
    private static final DateTimeFormatter HTTP_TIMESTAMP_PARSERS[] = {
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz")
    };

    // For error messages (including expectation that timezone is GMT)
    public static final String EXPECTED_HTTP_TIMESTAMP_FORMAT = "EEE, dd MMM yyyy HH:mm:ss GMT";

    // For specifying the TimeStamp pattern for DateTimeFormat annotation
    public static final String EXPECTED_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // Formatters:
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS'Z'").withLocale(Locale.US);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'Z'").withLocale(Locale.US);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern(EXPECTED_TIMESTAMP_FORMAT).withLocale(Locale.US);
    private static final DateTimeFormatter HTTP_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz").withLocale(Locale.US);

    /**
     * Parse a string that may represent a date, a time or a timestamp
     * and return a Date.  If no parsing is successful, throw an
     * IllegalArgumentException.
     * @param string The input string.  This should be in ISO8601 format
     * and represent a valid date, time or timestamp.
     * @return The Date value that the string represents
     * @throws IllegalArgumentException if the string cannot be parsed.
     */
    public static Date parseStringAsDate(String string)
            throws IllegalArgumentException
    {
        ZonedDateTime dateTime = null;
        try
        {
            dateTime = parseTimestampAsDateTime(string);
        }
        catch(IllegalArgumentException ex)
        {
            try
            {
                dateTime = parseDateAsDateTime(string);
            }
            catch(IllegalArgumentException exx)
            {
                // If this fails, it throws out the IllegalArgumentException
                dateTime = parseTimeAsDateTime(string);
            }
        }
        return Date.from(dateTime.toInstant());
    }

    /**
     * Parse a timestamp string using "yyyy-MM-dd'T'HH:mm:ss.SSSZ" format; the fractional part of
     * the seconds and the time zone are optional. Use the token Z to denote UTC (GMT).
     *
     * @param timestamp a timestamp string
     * @return the Date corresponding to the timestamp, in UTC time zone.
     * @throws IllegalArgumentException if the <var>timestamp</var> string is not properly formatted
     */
    public static Date parseTimestampAsDate(String timestamp)
            throws IllegalArgumentException
    {
        ZonedDateTime dateTime = parseTimestampAsDateTime(timestamp);
        Date out = Date.from(dateTime.toInstant());
        return out;
    }

    /**
     * Parse a timestamp string using "yyyy-MM-dd'T'HH:mm:ss.SSSZ" format; the fractional part of
     * the seconds and the time zone are optional. Use the token Z to denote UTC (GMT).
     *
     * @param timestamp a timestamp string
     * @return the Instant corresponding to the timestamp, in UTC time zone.
     * @throws IllegalArgumentException if the <var>timestamp</var> string is not properly formatted
     */
    public static Instant parseTimestampAsInstant(final String timestamp)
    {
        ZonedDateTime dateTime = parseTimestampAsDateTime(timestamp);
        final Instant instant = dateTime.toInstant();
        return instant;
    }

    /**
     * Parse a timestamp string using "yyyy-MM-ddZ" The the time zone are. Use the token Z to denote
     * UTC (GMT).
     *
     * @param date a timestamp string
     * @return the DateTime corresponding to the date string, in UTC time zone.
     * @throws IllegalArgumentException if the <var>date</var> string is not properly formatted
     */
    public static ZonedDateTime parseDateAsDateTime(String date)
            throws IllegalArgumentException
    {
        return tryParsing(date, DATE_PARSERS);
    }

    /**
     * Parse a time string using "HH:mm:ss.SSSZ"; the fractional part of the seconds and time zone
     * are. Use the token Z to denote UTC (GMT).
     *
     * @param time a timestamp string
     * @return the DateTime corresponding to the time string, in UTC time zone.
     * @throws IllegalArgumentException if the <var>time</var> string is not properly formatted
     */
    public static ZonedDateTime parseTimeAsDateTime(String time)
            throws IllegalArgumentException
    {
        return tryParsing(time, TIME_PARSERS);
    }

    /**
     * Parse a timestamp string using "yyyy-MM-dd'T'HH:mm:ss.SSSZ" or "yyyy-MM-dd'T'HH:mm:ssZ"
     * format. The fractional part of the seconds is optional, but the
     * <em>time zone is required</em>. Use the token Z to denote UTC (GMT).
     *
     * @param timestamp a timestamp string
     * @return the DateTime corresponding to the timestamp, in UTC time zone.
     * @throws IllegalArgumentException if the <var>timestamp</var> string is not properly formatted
     */
    public static ZonedDateTime parseTimestampAsDateTime(String timestamp)
            throws IllegalArgumentException
    {
        return tryParsing(timestamp, TIMESTAMP_PARSERS);
    }

    /**
     * Parse an httpTimestamp string using RFC 7231
     * "EEE, dd MMM yyyy HH:mm:ss GMT" format (must be in GMT timezone).
     * Negative years (before 0000) are not supported.
     *
     * @param httpTimestamp a string using HTTP format in GMT timezone
     * @return the Date corresponding to the httpTimestamp, in GMT/UTC time zone.
     * @throws IllegalArgumentException if the <var>httpTimestamp</var>
     *          string is not properly formatted
     */
    public static Date parseHttpTimestampAsDate(String httpTimestamp)
            throws IllegalArgumentException {
        ZonedDateTime dateTime = parseHttpTimestampAsDateTime(httpTimestamp);
        return Date.from(dateTime.toInstant());
    }

    /**
     * Parse an httpTimestamp string using RFC 7231
     * "EEE, dd MMM yyyy HH:mm:ss GMT" format (must be in GMT timezone).
     * Negative years (before 0000) are not supported.
     *
     * @param httpTimestamp a string using HTTP format in GMT timezone
     * @return the ZonedDateTime corresponding to the httpTimestamp, in UTC time zone.
     * @throws IllegalArgumentException if the <var>httpTimestamp</var>
     *          string is not properly formatted
     */
    public static ZonedDateTime parseHttpTimestampAsDateTime(
            String httpTimestamp)
            throws IllegalArgumentException {
        return tryParsing(httpTimestamp, HTTP_TIMESTAMP_PARSERS);
    }

    /*
     * Parse a string and figure out what it is, then make a ZonedDateTime in UTC
     * from it.
     */
    private static ZonedDateTime tryParsing(String time, DateTimeFormatter parsers[])
            throws IllegalArgumentException
    {
        ZonedDateTime dateTime = null;
        Exception last = null;
        for (DateTimeFormatter parser : parsers)
        {
            try
            {
                /*
                 * Parse the string using the next parser and get the
                 * TemporalAccessor.  Then try to figure out what we can
                 * make from it and turn that into a ZonedDateTime in zone
                 * UTC.
                 */
                TemporalAccessor ta = parser.parse(time);
                try
                {
                    /*
                     * If this has sufficient information to make a ZonedDateTime, make
                     * it directly and set the zone to "UTC".
                     */
                    dateTime = ZonedDateTime.from(ta).withZoneSameInstant(ZoneId.of("UTC"));
                    return dateTime;
                }
                catch(DateTimeException ex)
                {
                    /*
                     * If the zone was provided, get it and use it, otherwise
                     * fall back to UTC.
                     */
                    ZoneId zone = null;
                    try
                    {
                        zone = ZoneId.from(ta);
                    }
                    catch(Exception rex)
                    {
                        zone = ZoneId.of("UTC");
                    }
                    try
                    {
                        /*
                         * If there was no zone information, assume they intended
                         * UTC.
                         */
                        LocalDateTime ldt = LocalDateTime.from(ta);
                        dateTime = ZonedDateTime.of(ldt, zone);
                        return dateTime;
                    }
                    catch(DateTimeException ex2)
                    {
                        try
                        {
                            /*
                             * See if it was just a date. Create the ZonedDateTime using
                             * the date and zone with the time values zeroed.
                             */
                            LocalDate ld = LocalDate.from(ta);
                            LocalTime lt = LocalTime.of(0, 0, 0);
                            dateTime = ZonedDateTime.of(ld, lt, zone).withZoneSameInstant(ZoneId.of("UTC"));
                            return dateTime;
                        }
                        catch(DateTimeException ex3)
                        {
                            /*
                             * Last try.  If this was only a time, get the zone
                             * and fall back to UTC if it wasn't provided.  The default
                             * month and day are 1, since zero isn't allowed.
                             */
                            try
                            {
                                LocalTime lt = LocalTime.from(ta);
                                LocalDate ld = LocalDate.of(0, 1, 1);
                                dateTime = ZonedDateTime.of(ld, lt, zone).withZoneSameInstant(ZoneId.of("UTC"));
                                return dateTime;
                            }
                            catch(DateTimeException ex4)
                            {
                                last = ex;
                            }
                        }
                    }

                }
            }
            catch(DateTimeParseException ex)
            {
                last = ex;
            }
            finally
            {

            }
        }
        throw new IllegalArgumentException(last);
    }

    /**
     * Format a DateTime timestamp as a W3C/ISO 8601 compliant timestamp string using
     * "yyyy-MM-dd'T'HH:mm:ss.SSSZ" format
     *
     * @param timestamp a timestamp string
     * @return the Date corresponding to the timestamp, in UTC time zone.
     */
    public static String timestamp(ZonedDateTime timestamp)
    {
        /*
         * As ugly as this is, I can't find any other way to handle negative years.  The formatter
         * just seems to mangle them, and won't output the '-' no matter what I try.
         */
        if(timestamp.getYear() < 0)
        {
            ZonedDateTime temp = ZonedDateTime.of(-1*(timestamp.getYear()), timestamp.getMonthValue(), timestamp.getDayOfMonth(), timestamp.getHour(), timestamp.getMinute(), timestamp.getSecond(), timestamp.getNano(), timestamp.getZone());
            return "-"+temp.format(TIMESTAMP_FORMATTER);
        }
        return timestamp.format(TIMESTAMP_FORMATTER);
    }

    /**
     * Format a Date timestamp value as a W3C/ISO 8601 compliant timestamp string using
     * "yyyy-MM-dd'T'HH:mm:ss.SSSZ" format
     *
     * @param timestamp a date/time value
     * @return the formatted date/time string, in UTC time zone
     */
    public static String timestamp(Date timestamp)
    {
        return timestamp(timestamp.getTime());
    }

    /**
     * Format datetime value <var>millisSinceJavaEpoch</var> as a W3C/ISO 8601 compliant timestamp string using
     * "yyyy-MM-dd'T'HH:mm:ss.SSSZ" format
     *
     * @param millisSinceJavaEpoch milliseconds since the Java epoch (12:00AM, Jan 1, 1970 UTC)
     * @return the formatted date/time string, in UTC.
     */
    public static String timestamp(long millisSinceJavaEpoch)
    {
        return timestamp(Instant.ofEpochMilli(millisSinceJavaEpoch));
    }

    /**
     * Format datetime value <var>millisSinceJavaEpoch</var> as a W3C/ISO 8601 compliant timestamp string using
     * "yyyy-MM-dd'T'HH:mm:ss.SSSZ" format
     *
     * @param instant an Instant value
     * @return the formatted date/time string, in UTC.
     */
    public static String timestamp(final Instant instant)
    {
        ZonedDateTime timestamp = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"));
        return timestamp(timestamp);
    }

    /**
     * Format a ZonedDateTime value as a W3C/ISO 8601 compliant timestamp string using
     * "HH:mm:ss.SSSZ" format
     *
     * @param time a timestamp string
     * @return the formatted time string, in UTC time zone
     */
    public static String timeString(ZonedDateTime time)
    {
        return time.format(TIME_FORMATTER);
    }

    /**
     * Format a Date value as a W3C/ISO 8601 compliant timestamp string using
     * "HH:mm:ss.SSSZ" format
     *
     * @param time a time value
     * @return the formatted time string, in UTC time zone
     */
    public static String timeString(Date time)
    {
        return timeString(time.getTime());
    }

    /**
     * Format datetime value <var>millisSinceJavaEpoch</var> as a W3C/ISO 8601 compliant timestamp string using
     * "HH:mm:ss.SSSZ" format
     *
     * @param millisSinceJavaEpoch milliseconds since the Java epoch (12:00AM, Jan 1, 1970 UTC)
     * @return the formatted time string, in UTC.
     */
    public static String timeString(long millisSinceJavaEpoch)
    {
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millisSinceJavaEpoch), ZoneId.of("UTC"));
        return timeString(dateTime);
    }

    /**
     * Format a ZonedDateTime as a W3C/ISO 8601 compliant time string using "yyyy-MM-dd" format
     *
     * @param date a date value
     * @return the formatted time string, in UTC time zone
     */
    public static String dateString(ZonedDateTime date)
    {
        /*
         * As ugly as this is, I can't find any other way to handle negative years.  The formatter
         * just seems to mangle them, and won't output the '-' no matter what I try.
         */
        if(date.getYear() < 0)
        {
            ZonedDateTime temp = ZonedDateTime.of(-1*(date.getYear()), date.getMonthValue(), date.getDayOfMonth(), date.getHour(), date.getMinute(), date.getSecond(), date.getNano(), date.getZone());
            return "-"+temp.format(DATE_FORMATTER);
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * Format a Date as a W3C/ISO 8601 compliant time string using "yyyy-MM-dd" format
     *
     * @param date a time value
     * @return the formatted time string, in UTC time zone
     */
    public static String dateString(Date date)
    {
        return dateString(date.getTime());
    }

    /**
     * Format a datetime value <var>millisSinceJavaEpoch</var> as a W3C/ISO 8601 compliant time string using "yyyy-MM-dd" format
     *
     * @param millisSinceJavaEpoch milliseconds since the Java epoch (12:00AM, Jan 1, 1970 UTC)
     * @return the formatted time string, in UTC.
     */
    public static String dateString(long millisSinceJavaEpoch)
    {
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millisSinceJavaEpoch), ZoneId.of("UTC"));
        return dateString(dateTime);
    }

    /**
     * Format a DateTime as an RFC 7231 compliant HTTP Timestamp string
     * using "EEE, dd MMM yyyy HH:mm:ss GMT" format.
     * Negative years (before 0000) are not supported.
     *
     * @param dateTime ZonedDateTime to format
     * @return the String corresponding to the HTTP timestamp, in GMT time zone.
     */
    public static String httpTimestamp(ZonedDateTime dateTime) {
        if (dateTime.getYear() < 0) {
            throw new IllegalArgumentException("Negative years are not supported");
        }
        return dateTime.format(HTTP_TIMESTAMP_FORMATTER);
    }

    /**
     * Format a Date as a RFC 7231 compliant HTTP Timestamp string
     * using "EEE, dd MMM yyyy HH:mm:ss GMT" format
     *
     * @param date a date value including time
     * @return the formatted HTTP Timestamp string, in GMT time zone
     */
    public static String httpTimestamp(Date date) {
        return httpTimestamp(date.getTime());
    }

    /**
     * Format a datetime value <var>millisSinceJavaEpoch</var> as a RFC 7231 compliant
     * HTTP Timestamp string using "EEE, dd MMM yyyy HH:mm:ss GMT" format
     *
     * @param millisSinceJavaEpoch milliseconds since the Java epoch (12:00:00AM, Jan 01, 1970 GMT)
     * @return the formatted HTTP Timestamp string, in GMT.
     */
    public static String httpTimestamp(long millisSinceJavaEpoch) {
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millisSinceJavaEpoch),
                ZoneId.of("GMT"));
        return httpTimestamp(dateTime);
    }

    /**
     * Produces a new java.util.Date object with the same value as the
     * input preciseDate, but with any milliseconds (or nanoseconds)
     * truncated off, always rounding down to the previous whole second value.
     * A null input value will result in a null output value.
     *
     * @param preciseDate a Date value (possibly including milliseconds)
     * @return a new Date truncated to the previous whole second value or null if input is null
     */
    public static Date truncateDateToSeconds(Date preciseDate) {
        if (preciseDate == null) {
            return null;
        }

        Date truncatedDate;
        if (preciseDate.getTime() < 0) {
            truncatedDate = truncateNegativeDateToSeconds(preciseDate);
        } else {
            truncatedDate = truncatePositiveDateToSeconds(preciseDate);
        }

        return truncatedDate;
    }

    private static Date truncatePositiveDateToSeconds(Date preciseDate) {

        // java.util.Date, Instant, and SimpleDateFormat do strange things with
        // negative values, before 1970.
        // For negative values, use alternate algorithm in truncateNegativeDateToSeconds
        Instant preciseDateInstant = preciseDate.toInstant();
        Instant truncatedInstant = preciseDateInstant.truncatedTo(ChronoUnit.SECONDS);
        Date truncatedDate = Date.from(truncatedInstant);

        return truncatedDate;
    }

    private static Date truncateNegativeDateToSeconds(Date preciseDate) {

        // java.time.Instant.truncatedTo will throw away any negative milliseconds modifiers for
        // Date.getTime() millisecond values before the Epoch of Jan 1, 1970 0:00:00 GMT
        // This will have the appearance of "rounding up" to the previous calendar second.
        // SimpleDateFormat (and other formatters) will first calculate all of the effective
        // calendar components (e.g. 23:59:59.999 GMT for Date(-1)), then truncate the
        // milliseconds, always leaving it at the previous whole second.
        //
        // Simulate that algorithm here (without using heavyweight formatters/parsers),
        // by adding 1 millisecond (to potentially bump an initially "whole second" into the
        // next whole second with a -999 millisecond modifier), then truncate (which will
        // look like it is rounding up), then subtract one whole second (which should now
        // effectively always be the previous whole second (or the original value, if it
        // started as a whole second).
        Instant preciseDateInstant = preciseDate.toInstant();
        Instant adjustInstantUpOneMillisecond = preciseDateInstant.plus(1, ChronoUnit.MILLIS);
        Instant truncatedInstant = adjustInstantUpOneMillisecond.truncatedTo(ChronoUnit.SECONDS);
        Instant adjustTruncatedInstantMinusOneSecond = truncatedInstant.minus(1, ChronoUnit.SECONDS);
        Date truncatedDate = Date.from(adjustTruncatedInstantMinusOneSecond);

        return truncatedDate;
    }
}
