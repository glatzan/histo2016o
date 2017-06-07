package org.histo.action.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.histo.action.CommonDataHandlerAction;
import org.histo.action.WorklistHandlerAction;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.Gender;
import org.histo.config.enums.Month;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.WorklistSearchFilter;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.model.Physician;
import org.histo.model.patient.Patient;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class WorklistSearchDialogHandler extends AbstractDialog {

	@Autowired
	@Lazy
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private CommonDataHandlerAction commonDataHandlerAction;

	private WorklistSearchOption searchIndex;

	private boolean initialized;
	
	private boolean newPatients;
	private boolean stainingList;
	private boolean stainingReList;
	private boolean stainingStayInList;
	private boolean stainingDueDate;

	private boolean diagnosisList;
	private boolean diagnosisReList;
	private boolean diagnosisStayInList;
	private boolean diagnosisDueDate;

	private Date day;

	private Date searchFrom;
	private Date searchTo;

	private Month searchMonth;

	private int year;

	private WorklistSearchFilter filterIndex;

	private Map<String, Integer> years;

	private String extendedPatientName;
	private String extendedPatientSurename;
	private Date extendedPatientBirthday;
	private Gender extendedPatientGener;
	
	private String extendedMaterialName;
	private String extendedHistory;
	private Physician extendedSurgeon;
	private Physician extendedPrivatePhysician;
	
	private Date extendedEntryDate;
	
	private String extendedDiagnosis;
	private String extendedCategory;
	

	public void initAndPrepareBean() {
		initBean();
		prepareDialog();
	}

	public boolean initBean() {
		super.initBean(null, Dialog.WORKLIST_SEARCH);

		// init only on first init
		if (!initialized) {
			setSearchIndex(WorklistSearchOption.STAINING_LIST);
			setFilterIndex(WorklistSearchFilter.ADDED_TO_WORKLIST);

			// staining list
			setNewPatients(true);
			setStainingList(true);
			setStainingReList(true);
			setStainingStayInList(true);
			setStainingDueDate(true);

			setDiagnosisList(true);
			setDiagnosisReList(true);
			setDiagnosisStayInList(true);
			setDiagnosisDueDate(true);

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
			initialized = true;
		}

		return true;
	}

	/**
	 * Searches for a worklist an sets it as active worklist
	 */
	public void createWorklistAndSetAsActive() {

		ArrayList<Patient> result = createWorklist();

		worklistViewHandlerAction.getWorklists().put(worklistViewHandlerAction.getActiveWorklistKey(), result);

		commonDataHandlerAction.setSelectedPatient(null);
	}

	/**
	 * Searches for a worklist and returns it
	 * 
	 * @return
	 */
	public ArrayList<Patient> createWorklist() {

		logger.debug("Searching current worklist");

		ArrayList<Patient> result = new ArrayList<Patient>();

		Calendar cal = Calendar.getInstance();
		Date currentDate = new Date(System.currentTimeMillis());
		cal.setTime(currentDate);

		switch (getSearchIndex()) {
		case STAINING_LIST:
			logger.debug("Staining list selected");

			// getting new stainigs
			if (isNewPatients()) {
				result.addAll(patientDao.getPatientWithoutTasks(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
						TimeUtil.setDayEnding(cal).getTimeInMillis()));
			}

			ArrayList<Long> search = new ArrayList<Long>();

			if (isStainingList())
				search.add((long) PredefinedFavouriteList.StainingList.getId());

			if (isStainingReList())
				search.add((long) PredefinedFavouriteList.ReStainingList.getId());

			// TODO add check options in gui
			if (isDiagnosisStayInList())
				search.add((long) PredefinedFavouriteList.StayInStainingList.getId());

			result.addAll(patientDao.getPatientByTaskList(search));

			break;
		case DIAGNOSIS_LIST:
			logger.debug("Diagnosis list selected");
			// getting diagnoses an re_diagnoses
			search = new ArrayList<Long>();

			if (isDiagnosisList())
				search.add((long) PredefinedFavouriteList.DiagnosisList.getId());

			if (isDiagnosisReList())
				search.add((long) PredefinedFavouriteList.ReDiagnosisList.getId());

			if (isDiagnosisStayInList())
				search.add((long) PredefinedFavouriteList.StayInDiagnosisList.getId());

			result.addAll(patientDao.getPatientByTaskList(search));

			break;
		case NOTIFICATION_LIST:
			logger.debug("Notification list selected");
			search = new ArrayList<Long>();
			search.add((long) PredefinedFavouriteList.NotificationList.getId());
			search.add((long) PredefinedFavouriteList.StayInNotificationList.getId());

			result.addAll(patientDao.getPatientByTaskList(search));
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

		for (Patient patient : result) {
			try {
				patientDao.initilaizeTasksofPatient(patient);
			} catch (CustomDatabaseInconsistentVersionException e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	/**
	 * Returns false if any pre configured list is selected. All Filter options
	 * will be disabled
	 * 
	 * @return
	 */
	public boolean isEnableFilterOption() {
		switch (getSearchIndex()) {
		case STAINING_LIST:
		case NOTIFICATION_LIST:
		case DIAGNOSIS_LIST:
			return false;
		default:
			return true;
		}
	}

	// ************************ Getter/Setter ************************
	public WorklistSearchOption getSearchIndex() {
		return searchIndex;
	}

	public void setSearchIndex(WorklistSearchOption searchIndex) {
		this.searchIndex = searchIndex;
	}

	public boolean isNewPatients() {
		return newPatients;
	}

	public void setNewPatients(boolean newPatients) {
		this.newPatients = newPatients;
	}

	public boolean isStainingList() {
		return stainingList;
	}

	public void setStainingList(boolean stainingList) {
		this.stainingList = stainingList;
	}

	public boolean isStainingReList() {
		return stainingReList;
	}

	public void setStainingReList(boolean stainingReList) {
		this.stainingReList = stainingReList;
	}

	public boolean isStainingStayInList() {
		return stainingStayInList;
	}

	public void setStainingStayInList(boolean stainingStayInList) {
		this.stainingStayInList = stainingStayInList;
	}

	public boolean isStainingDueDate() {
		return stainingDueDate;
	}

	public void setStainingDueDate(boolean stainingDueDate) {
		this.stainingDueDate = stainingDueDate;
	}

	public boolean isDiagnosisList() {
		return diagnosisList;
	}

	public void setDiagnosisList(boolean diagnosisList) {
		this.diagnosisList = diagnosisList;
	}

	public boolean isDiagnosisReList() {
		return diagnosisReList;
	}

	public void setDiagnosisReList(boolean diagnosisReList) {
		this.diagnosisReList = diagnosisReList;
	}

	public boolean isDiagnosisStayInList() {
		return diagnosisStayInList;
	}

	public void setDiagnosisStayInList(boolean diagnosisStayInList) {
		this.diagnosisStayInList = diagnosisStayInList;
	}

	public boolean isDiagnosisDueDate() {
		return diagnosisDueDate;
	}

	public void setDiagnosisDueDate(boolean diagnosisDueDate) {
		this.diagnosisDueDate = diagnosisDueDate;
	}

	public Date getDay() {
		return day;
	}

	public void setDay(Date day) {
		this.day = day;
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
		this.filterIndex = filterIndex;
	}

	public Map<String, Integer> getYears() {
		return years;
	}

	public void setYears(Map<String, Integer> years) {
		this.years = years;
	}

}
