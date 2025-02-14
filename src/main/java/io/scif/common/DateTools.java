/*
 * #%L
 * SCIFIO library for reading and converting scientific file formats.
 * %%
 * Copyright (C) 2011 - 2023 SCIFIO developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package io.scif.common;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * A utility class with convenience methods for working with dates.
 *
 * @author Curtis Rueden
 * @author Chris Allan
 * @author Melissa Linkert
 */
public final class DateTools {

	// -- Constants --

	/** Timestamp formats. */
	public static final int UNIX = 0; // January 1, 1970

	public static final int COBOL = 1; // January 1, 1601

	public static final int MICROSOFT = 2; // December 30, 1899

	public static final int ZVI = 3;

	public static final int ALT_ZVI = 4;

	/** Milliseconds until UNIX epoch. */
	public static final long UNIX_EPOCH = 0;

	public static final long COBOL_EPOCH = 11644473600000L;

	public static final long MICROSOFT_EPOCH = 2209143600000L;

	public static final long ZVI_EPOCH = 2921084975759000L;

	public static final long ALT_ZVI_EPOCH = 2921084284761000L;

	/** ISO 8601 date format string. */
	public static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

	// -- Constructor --

	private DateTools() {}

	// -- Date handling --

	/**
	 * Converts from two-word tick representation to milliseconds. Mainly useful
	 * in conjunction with COBOL date conversion.
	 */
	public static long getMillisFromTicks(final long hi, final long lo) {
		final long ticks = ((hi << 32) | lo);
		return ticks / 10000; // 100 ns = 0.0001 ms
	}

	/** Converts the given timestamp into an ISO8601 date. */
	public static String convertDate(final long stamp, final int format) {
		return convertDate(stamp, format, ISO8601_FORMAT);
	}

	/** Converts the given timestamp into a date string with the given format. */
	public static String convertDate(final long stamp, final int format,
		final String outputFormat)
	{
		return convertDate(stamp, format, outputFormat, false);
	}

	/**
	 * Converts the given timestamp into a date string with the given format. If
	 * correctTimeZoneForGMT is set, then the timestamp will be interpreted as
	 * being relative to GMT and not the local time zone.
	 */
	public static String convertDate(final long stamp, final int format,
		final String outputFormat, final boolean correctTimeZoneForGMT)
	{
		// see http://www.merlyn.demon.co.uk/critdate.htm for more information
		// on
		// dates than you will ever need (or want)

		long ms = stamp;

		switch (format) {
			case UNIX:
				ms -= UNIX_EPOCH;
				break;
			case COBOL:
				ms -= COBOL_EPOCH;
				break;
			case MICROSOFT:
				ms -= MICROSOFT_EPOCH;
				break;
			case ZVI:
				ms -= ZVI_EPOCH;
				break;
			case ALT_ZVI:
				ms -= ALT_ZVI_EPOCH;
				break;
		}

		final SimpleDateFormat fmt = new SimpleDateFormat(outputFormat);
		if (correctTimeZoneForGMT) {
			final TimeZone tz = TimeZone.getDefault();
			ms -= tz.getOffset(ms);
		}
		final StringBuffer sb = new StringBuffer();

		final Date d = new Date(ms);

		fmt.format(d, sb, new FieldPosition(0));
		return sb.toString();
	}

	/**
	 * Formats the given date as an ISO 8601 date. Delegates to
	 * {@link #formatDate(String, String, boolean)}, with the 'lenient' flag set
	 * to false.
	 *
	 * @param date The date to format as ISO 8601.
	 * @param format The date's input format.
	 */
	public static String formatDate(final String date, final String format) {
		return formatDate(date, format, false);
	}

	/**
	 * Formats the given date as an ISO 8601 date.
	 *
	 * @param date The date to format as ISO 8601.
	 * @param format The date's input format.
	 * @param lenient Whether or not to leniently parse the date.
	 */
	public static String formatDate(final String date, final String format,
		final boolean lenient)
	{
		if (date == null) return null;
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setLenient(lenient);
		final Date d = sdf.parse(date, new ParsePosition(0));
		if (d == null) return null;
		sdf = new SimpleDateFormat(ISO8601_FORMAT);
		return sdf.format(d);
	}

	/**
	 * Formats the given date as an ISO 8601 date. Delegates to
	 * {@link #formatDate(String, String[], boolean)}, with the 'lenient' flag set
	 * to false.
	 *
	 * @param date The date to format as ISO 8601.
	 * @param formats The date's possible input formats.
	 */
	public static String formatDate(final String date, final String[] formats) {
		return formatDate(date, formats, false);
	}

	/**
	 * Formats the given date as an ISO 8601 date.
	 *
	 * @param date The date to format as ISO 8601.
	 * @param formats The date's possible input formats.
	 * @param lenient Whether or not to leniently parse the date.
	 */
	public static String formatDate(final String date, final String[] formats,
		final boolean lenient)
	{
		for (final String format : formats) {
			final String result = formatDate(date, format, lenient);
			if (result != null) return result;
		}
		return null;
	}

	/**
	 * Converts a string date in the given format to a long timestamp (in Unix
	 * format: milliseconds since January 1, 1970).
	 */
	public static long getTime(final String date, final String format) {
		final SimpleDateFormat f = new SimpleDateFormat(format);
		final Date d = f.parse(date, new ParsePosition(0));
		if (d == null) return -1;
		return d.getTime();
	}

}
