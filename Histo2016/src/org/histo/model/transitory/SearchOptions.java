package org.histo.model.transitory;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.histo.config.enums.Month;
import org.histo.config.enums.WorklistSearchFilter;
import org.histo.config.enums.WorklistSearchOption;

public class SearchOptions {

	private WorklistSearchOption searchIndex;

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

	private Month searchMonth;
	private int year;

	private WorklistSearchFilter filterIndex;

	private Map<String, Integer> years;

	public SearchOptions() {
		this(WorklistSearchOption.STAINING_LIST, WorklistSearchFilter.ADDED_TO_WORKLIST);
	}

	public SearchOptions(WorklistSearchOption searchIndex, WorklistSearchFilter filterIndex) {
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
		setSearchMonth(Month.getMonthByNumber(cal.get(Calendar.MONTH)));
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

	public WorklistSearchOption getSearchIndex() {
		return searchIndex;
	}

	public void setSearchIndex(WorklistSearchOption searchIndex) {
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

	public Month getSearchMonth() {
		return searchMonth;
	}

	public void setSearchMonth(Month searchMonth) {
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

	public WorklistSearchFilter getFilterIndex() {
		return filterIndex;
	}

	public void setFilterIndex(WorklistSearchFilter filterIndex) {
		// workaround because gui submits null value if radiobuttun is disabled
		if (filterIndex != null)
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
