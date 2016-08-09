package org.histo.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class SearchOptions {

    public static final int SEARCH_INDEX_STAINING = 0;
    public static final int SEARCH_INDEX_DIAGNOSIS = 1;
    public static final int SEARCH_INDEX_TODAY = 2;
    public static final int SEARCH_INDEX_YESTERDAY = 3;
    public static final int SEARCH_INDEX_CURRENTWEEK = 4;
    public static final int SEARCH_INDEX_LASTWEEK = 5;
    public static final int SEARCH_INDEX_LASTMONTH = 6;
    public static final int SEARCH_INDEX_DAY = 7;
    public static final int SEARCH_INDEX_MONTH = 8;
    public static final int SEARCH_INDEX_TIME = 9;

    public static final int SEARCH_FILTER_ADDTOWORKLIST = 0;
    public static final int SEARCH_FILTER_TASKCREATION = 1;
    public static final int SEARCH_FILTER_STAINING = 2;
    public static final int SEARCH_FILTER_DIAGNOSIS = 3;

    private int searchIndex;

    private boolean staining_new;
    private boolean staining_staining;
    private boolean staining_restaining;

    private boolean staining_diagnosis;
    private boolean staining_rediagnosis;

    private boolean deadline_staining; 	
    
    private boolean deadline_diagnosis;
    
    private Date day;

    private Date searchFrom;
    private Date searchTo;

    private int searchMonth;
    private int year;

    private int filterIndex;

    private Map<String, Integer> years;

    public SearchOptions() {
	this(0, 0);
    }

    public SearchOptions(int searchIndex, int filterIndex) {
	setSearchIndex(searchIndex);
	setFilterIndex(filterIndex);

	setStaining_new(true);
	setStaining_staining(true);
	setStaining_restaining(true);

	setStaining_diagnosis(true);
	setStaining_rediagnosis(true);

	// date for day
	setDay(new Date(System.currentTimeMillis()));

	Calendar cal = Calendar.getInstance();
	cal.setTime(new Date(System.currentTimeMillis()));

	// date for month in year
	setSearchMonth(cal.get(Calendar.MONTH));
	setYear(cal.get(Calendar.YEAR));

	// adding 30 years for date for month in year
	setYears(new TreeMap<String, Integer>());
	for (int i = 0; i < 30; i++) {
	    getYears().put(Integer.toString(year - i), year - i);
	}

	// set search form to
	setSearchTo(new Date(System.currentTimeMillis()));
	cal.add(Calendar.DAY_OF_MONTH, -1);
	setSearchFrom(cal.getTime());

    }

    public int getSearchIndex() {
	return searchIndex;
    }

    public void setSearchIndex(int searchIndex) {
	this.searchIndex = searchIndex;
    }

    public boolean isStaining_new() {
	return staining_new;
    }

    public void setStaining_new(boolean staining_new) {
	this.staining_new = staining_new;
    }

    public boolean isStaining_staining() {
	return staining_staining;
    }

    public void setStaining_staining(boolean staining_staining) {
	this.staining_staining = staining_staining;
    }

    public boolean isStaining_restaining() {
	return staining_restaining;
    }

    public void setStaining_restaining(boolean staining_restaining) {
	this.staining_restaining = staining_restaining;
    }

    public boolean isStaining_diagnosis() {
	return staining_diagnosis;
    }

    public void setStaining_diagnosis(boolean staining_diagnosis) {
	this.staining_diagnosis = staining_diagnosis;
    }

    public boolean isStaining_rediagnosis() {
	return staining_rediagnosis;
    }

    public void setStaining_rediagnosis(boolean staining_rediagnosis) {
	this.staining_rediagnosis = staining_rediagnosis;
    }

    public Date getSearchFrom() {
	return searchFrom;
    }

    public void setSearchFrom(Date searchFrom) {
	this.searchFrom = searchFrom;
    }

    public Date getSearchTo() {
	return searchTo;
    }

    public void setSearchTo(Date searchTo) {
	this.searchTo = searchTo;
    }

    public int getSearchMonth() {
	return searchMonth;
    }

    public void setSearchMonth(int searchMonth) {
	this.searchMonth = searchMonth;
    }

    public Map<String, Integer> getYears() {
	return years;
    }

    public void setYears(Map<String, Integer> years) {
	this.years = years;
    }

    public int getYear() {
	return year;
    }

    public void setYear(int year) {
	this.year = year;
    }

    public int getFilterIndex() {
	return filterIndex;
    }

    public void setFilterIndex(int filterIndex) {
	this.filterIndex = filterIndex;
    }

    public Date getDay() {
	return day;
    }

    public void setDay(Date day) {
	this.day = day;
    }

    public boolean isDeadline_staining() {
        return deadline_staining;
    }

    public void setDeadline_staining(boolean deadline_staining) {
        this.deadline_staining = deadline_staining;
    }

    public boolean isDeadline_diagnosis() {
        return deadline_diagnosis;
    }

    public void setDeadline_diagnosis(boolean deadline_diagnosis) {
        this.deadline_diagnosis = deadline_diagnosis;
    }
    
}
