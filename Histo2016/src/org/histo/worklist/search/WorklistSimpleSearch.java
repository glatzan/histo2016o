package org.histo.worklist.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.apache.log4j.Logger;
import org.histo.config.enums.Month;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.WorklistSearchFilter;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Patient;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable
public class WorklistSimpleSearch extends WorklistSearch {

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private FavouriteListDAO favouriteListDAO;

	@Getter
	@Setter
	private PredefinedFavouriteList[] lists;

	@Getter
	@Setter
	private PredefinedFavouriteList[] selectedLists;

	@Getter
	@Setter
	private boolean newPatients;

	@Getter
	@Setter
	private Date day;

	@Getter
	@Setter
	private Date searchFrom;

	@Getter
	@Setter
	private Date searchTo;

	@Getter
	@Setter
	private Month searchMonth;

	@Getter
	@Setter
	private int year;

	@Getter
	@Setter
	private Map<String, Integer> years;

	@Getter
	@Setter
	private WorklistSearchFilter filterIndex;

	@Getter
	@Setter
	private WorklistSearchOption searchIndex;

	public WorklistSimpleSearch() {
		setLists(PredefinedFavouriteList.values());

		setSearchIndex(WorklistSearchOption.STAINING_LIST);
		setFilterIndex(WorklistSearchFilter.ADDED_TO_WORKLIST);

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

		setFilterIndex(WorklistSearchFilter.ADDED_TO_WORKLIST);

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
					PredefinedFavouriteList.CouncilLendingMTA });
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
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis(), getFilterIndex()));
			break;
		case YESTERDAY:
			logger.debug("Yesterdy selected");
			cal.add(Calendar.DAY_OF_MONTH, -1);
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis(), getFilterIndex()));
			break;
		case CURRENTWEEK:
			logger.debug("Current week selected");
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setWeekBeginning(cal).getTimeInMillis(),
					TimeUtil.setWeekEnding(cal).getTimeInMillis(), getFilterIndex()));
			break;
		case LASTWEEK:
			logger.debug("Last week selected");
			cal.add(Calendar.WEEK_OF_YEAR, -1);
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setWeekBeginning(cal).getTimeInMillis(),
					TimeUtil.setWeekEnding(cal).getTimeInMillis(), getFilterIndex()));
			break;
		case LASTMONTH:
			logger.debug("Last month selected");
			cal.add(Calendar.MONDAY, -1);
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setMonthBeginning(cal).getTimeInMillis(),
					TimeUtil.setMonthEnding(cal).getTimeInMillis(), getFilterIndex()));
			break;
		case DAY:
			logger.debug("Day selected");
			cal.setTime(getDay());
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis(), getFilterIndex()));
			break;
		case MONTH:
			logger.debug("Month selected");
			cal.set(Calendar.MONTH, getSearchMonth().getNumber());
			cal.set(Calendar.YEAR, getYear());
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setMonthBeginning(cal).getTimeInMillis(),
					TimeUtil.setMonthEnding(cal).getTimeInMillis(), getFilterIndex()));
			break;
		case TIME:
			logger.debug("Time selected");
			cal.setTime(getSearchFrom());
			long fromTime = TimeUtil.setDayBeginning(cal).getTimeInMillis();
			cal.setTime(getSearchTo());
			long toTime = TimeUtil.setDayEnding(cal).getTimeInMillis();
			result.addAll(patientDao.getWorklistDynamicallyByType(fromTime, toTime, getFilterIndex()));
			break;
		default:
			break;
		}

		return result;
	}

}
