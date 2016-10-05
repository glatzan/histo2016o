package org.histo.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

public class TimeUtil {

	private static Logger log = Logger.getLogger(TimeUtil.class.getName());

	public static final int getCurrentYear() {
		int year = Calendar.getInstance().get(Calendar.YEAR);
		return year;
	}

	public static final long getDateInUnixTimestamp(int year, int month, int day, int hour, int minute, int second) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(0);
		cal.set(year, month, day, hour, minute, second);
		return cal.getTimeInMillis();
	}

	public static final boolean isDateOnSameDay(long date, long timeOfDay) {
		return isDateOnSameDay(new Date(date), new Date(timeOfDay));
	}
	
	public static final boolean isDateOnSameDay(Date date, Date timeOfDay) {
		Calendar dateC = Calendar.getInstance();
		dateC.setTime(date);
		Calendar timeOfDayC = Calendar.getInstance();
		timeOfDayC.setTime(timeOfDay);

		return (dateC.get(Calendar.YEAR) == timeOfDayC.get(Calendar.YEAR))
				&& (dateC.get(Calendar.DAY_OF_YEAR) == timeOfDayC.get(Calendar.DAY_OF_YEAR));
	}

	public static final Calendar setMonthEnding(Calendar cal) {
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		setDayEnding(cal);
		return cal;
	}

	public static final Calendar setMonthBeginning(Calendar cal) {
		cal.set(Calendar.DAY_OF_MONTH, 1);
		setDayBeginning(cal);
		return cal;
	}

	public static final Calendar setWeekEnding(Calendar cal) {
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		setDayEnding(cal);
		return cal;
	}

	public static final Calendar setWeekBeginning(Calendar cal) {
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		setDayBeginning(cal);
		return cal;
	}

	public static final Calendar setDayEnding(Calendar cal) {
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
		return cal;
	}

	public static final long setDayBeginning(long date){
		return setDayBeginning(new Date(date)).getTime();
	}
	
	public static final Date setDayBeginning(Date date){
		Calendar dateC = Calendar.getInstance();
		dateC.setTime(date);
		return setDayBeginning(dateC).getTime();
	}

	public static final Calendar setDayBeginning(Calendar cal) {
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}
	
	/**
	 * Formats a given date using the given formatString. Returns a string.
	 * @param date
	 * @param formatString
	 * @return
	 */
	public static final String formatDate(Date date, String formatString){
		String dateString = "";

		try {
			SimpleDateFormat sdfr = new SimpleDateFormat(formatString);
			dateString = sdfr.format(date);
		} catch (Exception ex) {
			System.out.println(ex);
		}
		return dateString;
	}
}
