package org.histo.config.enums;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum QuickSearchOptions {
	NAME, NAME_AND_SURNAME, PIZ, TASK_ID, SLIDE_ID, NONE;

	/**
	 * Checks the string for a name (Name, surname), PIZ (8), Task_ID (6), SLIDE_ID (10) 
	 * @param search
	 * @param toSearch
	 * @return
	 */
	public static QuickSearchOptions getQuickSearchOption(String search, String[] toSearch) {
		if (search == null || search.isEmpty())
			return NONE;

		// name + given name
		String pattern = "(.+)[, ](.+)";
		
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(search);

		if (m.find()) {
			toSearch[0] = m.group(1);
			toSearch[1] = m.group(2);
			return NAME_AND_SURNAME;
		}

		// name
		pattern = "[\\p{Alpha}\\-]+";

		r = Pattern.compile(pattern);
		m = r.matcher(search);

		if (m.find()) {
			toSearch[0] = m.group(0);
			return NAME;
		}
		
		// piz
		pattern = "(\\d{8})";

		r = Pattern.compile(pattern);
		m = r.matcher(search);

		if (m.find()) {
			toSearch[0] = m.group(1);
			return PIZ;
		}

		// task
		pattern = "(\\d{6})";

		r = Pattern.compile(pattern);
		m = r.matcher(search);

		if (m.find()) {
			toSearch[0] = m.group(1);
			return TASK_ID;
		}

		// slide
		pattern = "(\\d{10})";

		r = Pattern.compile(pattern);
		m = r.matcher(search);

		if (m.find()) {
			toSearch[0] = m.group(1);
			return SLIDE_ID;
		}

		return NONE;
	}
}
