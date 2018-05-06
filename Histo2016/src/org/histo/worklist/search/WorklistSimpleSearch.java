package org.histo.worklist.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.histo.config.enums.DateFormat;
import org.histo.config.enums.Month;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Patient;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class WorklistSimpleSearch extends WorklistSearch {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	private PredefinedFavouriteList[] lists;

	private PredefinedFavouriteList[] selectedLists;

	private boolean newPatients;

	private Date day;

	private Date searchFrom;

	private Date searchTo;

	private Month searchMonth;

	private int year;

	private Map<String, Integer> years;

	private SimpleTimeSearchFilter filterIndex;

	private SimpleSearchOption searchIndex;

	public WorklistSimpleSearch() {
		setLists(PredefinedFavouriteList.values());

		if (getSearchIndex() == null)
			setSearchIndex(SimpleSearchOption.STAINING_LIST);

		if (getFilterIndex() == null)
			setFilterIndex(SimpleTimeSearchFilter.ADDED_TO_WORKLIST);

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

		updateSearchIndex();
	}

	/**
	 * Returns false if any pre configured list is selected. All Filter options will
	 * be disabled
	 * 
	 * @return
	 */
	public boolean isSearchForLists() {
		switch (getSearchIndex()) {
		case STAINING_LIST:
		case NOTIFICATION_LIST:
		case DIAGNOSIS_LIST:
		case CUSTOM_LIST:
			return false;
		default:
			return true;
		}
	}

	public void updateSearchIndex() {
		switch (getSearchIndex()) {
		case STAINING_LIST:
			setSelectedLists(new PredefinedFavouriteList[] { PredefinedFavouriteList.StainingList,
					PredefinedFavouriteList.StayInStainingList, PredefinedFavouriteList.ReStainingList,
					PredefinedFavouriteList.CouncilLendingMTA, PredefinedFavouriteList.ScannList });
			setNewPatients(true);
			break;
		case DIAGNOSIS_LIST:
			setSelectedLists(new PredefinedFavouriteList[] { PredefinedFavouriteList.DiagnosisList,
					PredefinedFavouriteList.ReDiagnosisList, PredefinedFavouriteList.StayInDiagnosisList,
					PredefinedFavouriteList.CouncilCompleted });
			setNewPatients(false);
			break;
		case NOTIFICATION_LIST:
			setSelectedLists(new PredefinedFavouriteList[] { PredefinedFavouriteList.NotificationList,
					PredefinedFavouriteList.StayInNotificationList, PredefinedFavouriteList.CouncilLendingSecretary });
			setNewPatients(false);
			break;
		default:
			break;
		}

	}

	@Override
	public List<Patient> getWorklist() {
		logger.debug("Searching current worklist");

		ArrayList<Patient> result = new ArrayList<Patient>();

		Calendar cal = Calendar.getInstance();
		Date currentDate = new Date(System.currentTimeMillis());
		cal.setTime(currentDate);

		switch (getSearchIndex()) {
		case STAINING_LIST:
		case DIAGNOSIS_LIST:
		case NOTIFICATION_LIST:
		case CUSTOM_LIST:

			logger.debug("Staining list selected");

			// getting new stainigs
			if (isNewPatients()) {
				result.addAll(patientDao.getPatientWithoutTasks(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
						TimeUtil.setDayEnding(cal).getTimeInMillis(), true));
			}

			if (getSelectedLists() != null && getSelectedLists().length > 0) {
				result.addAll(favouriteListDAO.getPatientFromFavouriteList(
						Arrays.asList(getSelectedLists()).stream().map(p -> p.getId()).collect(Collectors.toList()),
						true));
			}

			break;
		case TODAY:
			logger.debug("Today selected");
			result.addAll(getWorklistByTimeSearchFilter(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis()));
			break;
		case YESTERDAY:
			logger.debug("Yesterdy selected");
			cal.add(Calendar.DAY_OF_MONTH, -1);
			result.addAll(getWorklistByTimeSearchFilter(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis()));
			break;
		case CURRENTWEEK:
			logger.debug("Current week selected");
			result.addAll(getWorklistByTimeSearchFilter(TimeUtil.setWeekBeginning(cal).getTimeInMillis(),
					TimeUtil.setWeekEnding(cal).getTimeInMillis()));
			break;
		case LASTWEEK:
			logger.debug("Last week selected");
			cal.add(Calendar.WEEK_OF_YEAR, -1);
			result.addAll(getWorklistByTimeSearchFilter(TimeUtil.setWeekBeginning(cal).getTimeInMillis(),
					TimeUtil.setWeekEnding(cal).getTimeInMillis()));
			break;
		case CURRENTMONTH:
			logger.debug("Current month selected");
			result.addAll(getWorklistByTimeSearchFilter(TimeUtil.setMonthBeginning(cal).getTimeInMillis(),
					TimeUtil.setMonthEnding(cal).getTimeInMillis()));
			break;
		case LASTMONTH:
			cal.add(Calendar.MONTH, -1);
			logger.debug("Last month selected " + cal);
			result.addAll(getWorklistByTimeSearchFilter(TimeUtil.setMonthBeginning(cal).getTimeInMillis(),
					TimeUtil.setMonthEnding(cal).getTimeInMillis()));
			break;
		case DAY:
			logger.debug("Day selected");
			cal.setTime(getDay());
			result.addAll(getWorklistByTimeSearchFilter(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis()));
			break;
		case MONTH:
			logger.debug("Month selected");
			cal.set(Calendar.MONTH, getSearchMonth().getNumber());
			cal.set(Calendar.YEAR, getYear());
			result.addAll(getWorklistByTimeSearchFilter(TimeUtil.setMonthBeginning(cal).getTimeInMillis(),
					TimeUtil.setMonthEnding(cal).getTimeInMillis()));
			break;
		case TIME:
			logger.debug("Time selected");
			cal.setTime(getSearchFrom());
			long fromTime = TimeUtil.setDayBeginning(cal).getTimeInMillis();
			cal.setTime(getSearchTo());
			long toTime = TimeUtil.setDayEnding(cal).getTimeInMillis();
			result.addAll(getWorklistByTimeSearchFilter(fromTime, toTime));
			break;
		default:
			break;
		}

		return result;
	}

	public List<Patient> getWorklistByTimeSearchFilter(long fromDate, long toDate) {
		switch (getFilterIndex()) {
		case ADDED_TO_WORKLIST:
			logger.debug("Searching for add date from "
					+ TimeUtil.formatDate(fromDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()) + " to "
					+ TimeUtil.formatDate(toDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()));
			return patientDao.getPatientByAddDateToDatabaseDate(fromDate, toDate, true);
		case TASK_CREATION:
			logger.debug("Searching for task creation date from "
					+ TimeUtil.formatDate(fromDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()) + " to "
					+ TimeUtil.formatDate(toDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()));
			return patientDao.getPatientByTaskCreationDate(fromDate, toDate, true);
		case STAINING_COMPLETED:
			logger.debug("Searching for staining completed "
					+ TimeUtil.formatDate(fromDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()) + " to "
					+ TimeUtil.formatDate(toDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()));
			return patientDao.getPatientByStainingsCompletionDate(fromDate, toDate, true);
		case DIAGNOSIS_COMPLETED:
			logger.debug("Searching for diagnosis completed "
					+ TimeUtil.formatDate(fromDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()) + " to "
					+ TimeUtil.formatDate(toDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()));
			return patientDao.getPatientByDiagnosisCompletionDate(fromDate, toDate, true);
		case NOTIFICATION_COMPLETED:
			logger.debug("Searching for notification completed "
					+ TimeUtil.formatDate(fromDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()) + " to "
					+ TimeUtil.formatDate(toDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()));
			return patientDao.getPatientByNotificationCompletionDate(fromDate, toDate, true);
		case FINALIZED:
			logger.debug("Searching for finalized completed "
					+ TimeUtil.formatDate(fromDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()) + " to "
					+ TimeUtil.formatDate(toDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()));
			return patientDao.getPatientByFinalizedTaskDate(fromDate, toDate, true);
		default:
			return null;
		}
	}

	/**
	 * List with seach options
	 * 
	 * @author andi
	 *
	 */
	public enum SimpleSearchOption {
		STAINING_LIST, DIAGNOSIS_LIST, NOTIFICATION_LIST, CUSTOM_LIST, EMPTY_LIST, TODAY, YESTERDAY, CURRENTWEEK, LASTWEEK, CURRENTMONTH, LASTMONTH, DAY, MONTH, TIME,;
	}

	/**
	 * Filter options when time is selected
	 * 
	 * @author andi
	 *
	 */
	public enum SimpleTimeSearchFilter {
		ADDED_TO_WORKLIST, TASK_CREATION, STAINING_COMPLETED, DIAGNOSIS_COMPLETED, NOTIFICATION_COMPLETED, FINALIZED;
	}

}
