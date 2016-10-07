package org.histo.action;

import java.awt.Dialog;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.annotation.PostConstruct;
import javax.persistence.CacheStoreMode;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
import org.histo.config.enums.Display;
import org.histo.config.enums.Role;
import org.histo.config.enums.Worklist;
import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.Contact;
import org.histo.model.DiagnosisPrototype;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.StainingPrototype;
import org.histo.model.MaterialPreset;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Task;
import org.histo.model.util.ArchivAble;
import org.histo.model.util.TaskTree;
import org.histo.ui.transformer.StainingListTransformer;
import org.histo.util.SearchOptions;
import org.histo.util.TaskUtil;
import org.histo.util.TimeUtil;
import org.histo.util.WorklistUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class WorklistHandlerAction implements Serializable {

	private static final long serialVersionUID = 7122206530891485336L;

	public static String SEARCH_CURRENT = "%current%";
	public static String SEARCH_YESTERDAY = "%yesterday%";
	public static String SEARCH_LASTWEEK = "%lastweek%";
	public static String SEARCH_TIME = "%time%";
	public static String SEARCH_INDIVIDUAL = "%individual%";

	public final static int TIME_TODAY = 1;
	public final static int TIME_YESTERDAY = 2;
	public final static int TIME_LASTWEEK = 3;
	public final static int TIME_LASTMONTH = 4;
	public final static int TIME_CUSTOM = 5;
	public final static int TIME_FAVOURITE = 6;

	public final static int SORT_ORDER_TASK_ID = 0;
	public final static int SORT_ORDER_PIZ = 1;
	public final static int SORT_ORDER_NAME = 2;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private HelperHandlerAction helper;

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Lazy
	private SettingsHandlerAction settingsHandlerAction;

	@Autowired
	@Lazy
	private SlideHandlerAction slideHandlerAction;

	@Autowired
	@Lazy
	private DiagnosisHandlerAction diagnosisHandlerAction;

	@Autowired
	@Lazy
	private TaskHandlerAction taskHandlerAction;

	/******************************************************** Patient ********************************************************/
	/**
	 * Currently selected patient
	 */
	private Patient selectedPatient;

	/******************************************************** Patient ********************************************************/

	/******************************************************** Worklist ********************************************************/
	/**
	 * Ungefilterte Worklist
	 */
	private List<Patient> workList;

	/**
	 * Worklist, gefiltert und sortiert
	 */
	private List<Patient> restrictedWorkList;

	/**
	 * Order of the Worklist, either by id or by patient name
	 */
	private int worklistSortOrder = SORT_ORDER_TASK_ID;

	/**
	 * Order of the Worlist, either if true ascending, or if false descending
	 */
	private boolean worklistSortOrderAcs = true;

	/*
	 * ************************** Worklist ****************************
	 */

	/**
	 * The key of the current active worklist.
	 */
	private String activeWorklistKey;

	/**
	 * Hashmap containing all worklists for the current user.
	 */
	private HashMap<String, ArrayList<Patient>> worklists;

	/**
	 * Search Options
	 */
	private SearchOptions searchOptions;

	/******************************************************** General ********************************************************/

	private String searchType;

	private String searchString;

	/**
	 * Paiten search results form external Database
	 */
	private ArrayList<Person> searchResults;

	public WorklistHandlerAction() {
	}

	@PostConstruct
	public void init() {
		// init worklist
		worklists = new HashMap<String, ArrayList<Patient>>();

		worklists.put(Worklist.DEFAULT.getName(), new ArrayList<Patient>());

		setActiveWorklistKey(Worklist.DEFAULT.getName());

		setSearchOptions(new SearchOptions());

		// getting default worklist depending on role
		Role userRole = userHandlerAction.getCurrentUser().getRole();

		switch (userRole) {
		case MTA:
			getSearchOptions().setSearchIndex(SearchOptions.SEARCH_INDEX_STAINING);
			searCurrentWorklist();
			break;
		case USER:
			break;
		case PHYSICIAN:
		case MODERATOR:
		case ADMIN:
			getSearchOptions().setSearchIndex(SearchOptions.SEARCH_INDEX_DIAGNOSIS);
			searCurrentWorklist();
		default:
			break;
		}
	}

	public void selectPatient(Patient patient) {
		// TODO
	}

	/**
	 * Adds a patient to the worklist. If already added it is check if the
	 * patient should be selected. If so the patient will be selected. The
	 * patient isn't added twice.
	 * 
	 * @param patient
	 * @param asSelectedPatient
	 */
	public void addPatientToWorkList(Patient patient, boolean asSelectedPatient) {
		// checks if patient is already in database
		for (Patient patientInWorklis : getWorkList()) {
			if (patientInWorklis.getId() == patient.getId()) {
				if (asSelectedPatient)
					setSelectedPatient(patientInWorklis);
				return;
			}
		}

		getWorkList().add(patient);
		if (asSelectedPatient)
			setSelectedPatient(patient);
	}

	public void removeFromWorklist(Patient patient) {
		System.out.println(patient + " " + patient.getPerson().getName());
		getWorkList().remove(patient);
		if (getSelectedPatient() == patient)
			setSelectedPatient(null);

	}

	public String getCenterView(Display display) {
		switch (display) {
		case PATIENT:
			if (getSelectedPatient() == null)
				return HistoSettings.CENTER_INCLUDE_BLANK;
			else
				return HistoSettings.CENTER_INCLUDE_PATIENT;
		case RECEIPTLOG:
			if (getSelectedPatient() != null && getSelectedPatient().getSelectedTask() != null)
				return HistoSettings.CENTER_INCLUDE_RECEIPTLOG;
			else if (getSelectedPatient() != null) {
				if (getSelectedPatient().getTasks() != null && getSelectedPatient().getTasks().size() > 0) {
					getSelectedPatient().setSelectedTask(TaskUtil.getLastTask(getSelectedPatient().getTasks()));
					return HistoSettings.CENTER_INCLUDE_RECEIPTLOG;
				} else
					return HistoSettings.CENTER_INCLUDE_PATIENT;
			} else
				return HistoSettings.CENTER_INCLUDE_BLANK;
		case DIAGNOSIS_INTERN:
			if (getSelectedPatient() != null && getSelectedPatient().getSelectedTask() != null)
				return HistoSettings.CENTER_INCLUDE_DIAGNOSIS_INTERN;
			else if (getSelectedPatient() != null)
				return HistoSettings.CENTER_INCLUDE_PATIENT;
			else
				return HistoSettings.CENTER_INCLUDE_BLANK;

			// case DIAGNOSIS_INTERN_EXTENDED:
			// if (getSelectedPatient() != null &&
			// getSelectedPatient().getSelectedTask() != null)
			// return HistoSettings.CENTER_INCLUDE_EXTERN_EXTENDED;
			// else if (getSelectedPatient() != null)
			// return HistoSettings.CENTER_INCLUDE_PATIENT;
			// else
			// return HistoSettings.CENTER_INCLUDE_BLANK;
		default:
			break;
		}
		return "";
	}

	/**
	 * Sorts a list with patiens either by task id or name of the patient
	 * 
	 * @param patiens
	 * @param order
	 */
	public void sortWordklist(List<Patient> patiens, int order, boolean asc) {
		switch (order) {
		case SORT_ORDER_TASK_ID:
			setRestrictedWorkList((new WorklistUtil()).orderListByTaskID(patiens, asc));
			break;
		case SORT_ORDER_PIZ:
			setRestrictedWorkList((new WorklistUtil()).orderListByPIZ(patiens, asc));
			break;
		case SORT_ORDER_NAME:
			setRestrictedWorkList((new WorklistUtil()).orderListByName(patiens, asc));
			break;
		}

		hideSortWorklistDialog();
	}

	public void prepareSortWorklistDialog() {
		helper.showDialog(HistoSettings.dialog(HistoSettings.DIALOG_WORKLIST_ORDER), 300, 220, false, false, true);
	}

	public void hideSortWorklistDialog() {
		helper.hideDialog(HistoSettings.dialog(HistoSettings.DIALOG_WORKLIST_ORDER));
	}

	/******************************************************** General ********************************************************/

	/******************************************************** Task ********************************************************/
	/**
	 * Selects a task and sets the patient of this task as selectedPatient
	 * 
	 * @param task
	 */
	public void selectTaskOfPatient(Task task) {
		setSelectedPatient(task.getParent());
		selectPatient(getSelectedPatient());
		selectTask(task);
	}

	/**
	 * Task - Select and init
	 */
	public void selectTask(Task task) {
		System.out.println(task.getTaskID());
		// set patient.selectedTask is performed by the gui
		// sets this task as active, so it will be show in the navigation column
		// whether there is an action to perform or not
		task.setCurrentlyActive(true);

		if (task.getPatient().getSelectedTask() != task)
			task.getPatient().setSelectedTask(task);

		// log.info("Select and init sample");

		// int userLevel =
		// userHandlerAction.getCurrentUser().getRole().getLevel();
		//
		// task.generateStainingGuiList();
		//
		// if (userLevel == UserRole.ROLE_LEVEL_MTA) {
		// task.setTabIndex(Task.TAB_STAINIG);
		// } else {
		// task.setTabIndex(Task.TAB_DIAGNOSIS);
		// }

		// Setzte action to none
		slideHandlerAction.setActionOnMany(SlideHandlerAction.STAININGLIST_ACTION_NONE);

		// init all available diagnoses
		settingsHandlerAction.updateAllDiagnosisPrototypes();

		// init all available materials
		taskHandlerAction.prepareForTask();

		// setzte das diasplay auf das eingangsbuch wenn der patient angezeigt
		// wird
//		if (getWorklistDisplay() == Display.PATIENT)
//			setWorklistDisplay(Display.RECEIPTLOG);
	}

	public void deselectTask(Patient patient) {
//		setWorklistDisplay(Display.PATIENT);
//		patient.setSelectedTask(null);
	}

	/******************************************************** Task ********************************************************/

	/*
	 * ************************** Worklist ****************************
	 */

	/**
	 * Searches the database for the given searchOptions and overwrites the
	 * content of the current worklist with the found content.
	 */
	public void searCurrentWorklist() {
		searCurrentWorklist(getSearchOptions());
	}

	/**
	 * Searches the database for the given searchOptions and overwrites the
	 * content of the current worklist with the found content.
	 */
	public void searCurrentWorklist(SearchOptions searchOptions) {

		ArrayList<Patient> result = new ArrayList<Patient>();

		Calendar cal = Calendar.getInstance();
		Date currentDate = new Date(System.currentTimeMillis());
		cal.setTime(currentDate);

		switch (searchOptions.getSearchIndex()) {
		case SearchOptions.SEARCH_INDEX_STAINING:

			if (searchOptions.isStaining_new()) {
				result.addAll(patientDao.getPatientWithoutTasks(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
						TimeUtil.setDayEnding(cal).getTimeInMillis()));
			}

			if (searchOptions.isStaining_staining() && searchOptions.isStaining_restaining()) {
				result.addAll(patientDao.getPatientByStainingsBetweenDates(0, System.currentTimeMillis(), false));
			} else {
				List<Patient> paints = patientDao.getPatientByStainingsBetweenDates(0, System.currentTimeMillis(),
						false);
				for (Patient patient : paints) {
					if (searchOptions.isStaining_staining() && patient.isStainingNeeded()) {
						result.add(patient);
					} else if (searchOptions.isStaining_restaining() && patient.isReStainingNeeded()) {
						result.add(patient);
					}
				}
			}

			break;
		case SearchOptions.SEARCH_INDEX_DIAGNOSIS:
			if (searchOptions.isStaining_diagnosis() && searchOptions.isStaining_rediagnosis()) {
				result.addAll(patientDao.getPatientByDiagnosBetweenDates(0, System.currentTimeMillis(), false));
			} else {
				List<Patient> paints = patientDao.getPatientByDiagnosBetweenDates(0, System.currentTimeMillis(), false);
				for (Patient patient : paints) {
					if (searchOptions.isStaining_diagnosis() && patient.isDiagnosisNeeded()) {
						result.add(patient);
					} else if (searchOptions.isStaining_rediagnosis() && patient.isReDiagnosisNeeded()) {
						result.add(patient);
					}
				}
			}
			break;
		case SearchOptions.SEARCH_INDEX_TODAY:
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case SearchOptions.SEARCH_INDEX_YESTERDAY:
			cal.add(Calendar.DAY_OF_MONTH, -1);
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case SearchOptions.SEARCH_INDEX_CURRENTWEEK:
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setWeekBeginning(cal).getTimeInMillis(),
					TimeUtil.setWeekEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case SearchOptions.SEARCH_INDEX_LASTWEEK:
			cal.add(Calendar.WEEK_OF_YEAR, -1);
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setWeekBeginning(cal).getTimeInMillis(),
					TimeUtil.setWeekEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case SearchOptions.SEARCH_INDEX_LASTMONTH:
			cal.add(Calendar.MONDAY, -1);
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setMonthBeginning(cal).getTimeInMillis(),
					TimeUtil.setMonthEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case SearchOptions.SEARCH_INDEX_DAY:
			cal.setTime(searchOptions.getDay());
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case SearchOptions.SEARCH_INDEX_MONTH:
			cal.set(Calendar.MONTH, searchOptions.getSearchMonth());
			cal.set(Calendar.YEAR, searchOptions.getYear());
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setMonthBeginning(cal).getTimeInMillis(),
					TimeUtil.setMonthEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case SearchOptions.SEARCH_INDEX_TIME:
			cal.setTime(searchOptions.getSearchFrom());
			long fromTime = TimeUtil.setDayBeginning(cal).getTimeInMillis();
			cal.setTime(searchOptions.getSearchTo());
			long toTime = TimeUtil.setDayEnding(cal).getTimeInMillis();
			result.addAll(patientDao.getWorklistDynamicallyByType(fromTime, toTime, searchOptions.getFilterIndex()));
			break;
		default:
			break;
		}

		getWorklists().put(getActiveWorklistKey(), result);

		setSelectedPatient(null);
	}

	public void selectNextTask() {
		if (getRestrictedWorkList() != null) {
			if (getSelectedPatient() != null) {
				int indexOfPatient = getRestrictedWorkList().indexOf(getSelectedPatient());
				if (getRestrictedWorkList().size() - 1 > indexOfPatient)
					setSelectedPatient(getRestrictedWorkList().get(indexOfPatient + 1));
			} else {
				setSelectedPatient(getRestrictedWorkList().get(0));
			}
		}
	}

	public void selectPreviouseTask() {
		if (getRestrictedWorkList() != null) {
			if (getSelectedPatient() != null) {
				int indexOfPatient = getRestrictedWorkList().indexOf(getSelectedPatient());
				if (indexOfPatient > 0)
					setSelectedPatient(getRestrictedWorkList().get(indexOfPatient - 1));
			} else {
				setSelectedPatient(getRestrictedWorkList().get(getRestrictedWorkList().size() - 1));
			}
		}
	}

	/*
	 * ************************** Worklist ****************************
	 */

	/********************************************************
	 * Task Dialogs
	 ********************************************************/
	public void prepareAccountingDialog() {
		helper.showDialog("/pages/dialog/task/accounting", 430, 270, true, false, true);
	}

	public void hideAccountingDialog() {
		helper.hideDialog("/pages/dialog/task/accounting");
	}

	public void prepareNotificationDialog() {
		helper.showDialog("/pages/dialog/task/notification");
	}

	/********************************************************
	 * Task Dialogs
	 ********************************************************/

	// public void updateSample(Sample sample) {
	// boolean completed = true;
	//
	// for (Diagnosis diagnosis2 : sample.getDiagnoses()) {
	// if (!diagnosis2.isFinalized())
	// completed = false;
	// }
	//
	// if (completed) {
	// sample.setDiagnosisCompletionDate(System.currentTimeMillis());
	// sample.setDiagnosisCompleted(true);
	// } else {
	// sample.setDiagnosisCompleted(false);
	// sample.setDiagnosisCompletionDate(System.currentTimeMillis());
	// }
	// }

	public void searchForExistingPatients() {
		if (getSearchString().matches("[0-9]{6,8}")) {
			System.out.println("piz");
			System.out.println(patientDao.searchForPatientsPiz(getSearchString()));
		} else if (getSearchString().matches("[a-zA-Z ,]*")) {
			Pattern p = Pattern.compile("[a-zA-Z-]*?[ ,]*?[a-zA-Z-]*?");
			Matcher m = p.matcher(getSearchString()); // get a matcher object
			int count = 0;

			while (m.find()) {
				count++;
				System.out.println("Match number " + count);
				System.out.println("start(): " + m.start());
				System.out.println("end(): " + m.end());
				System.out.println(m.group());
			}
		}
	}

	public void prepareAddStaning() {
		helper.showDialog("/pages/dialog/staining", true, false, true);
	}

	/*
	 * ************************** Getters/Setters ****************************
	 */

	public String getActiveWorklistKey() {
		return activeWorklistKey;
	}

	public void setActiveWorklistKey(String activeWorklistKey) {
		this.activeWorklistKey = activeWorklistKey;
	}

	public HashMap<String, ArrayList<Patient>> getWorklists() {
		return worklists;
	}

	public void setWorklists(HashMap<String, ArrayList<Patient>> worklists) {
		this.worklists = worklists;
	}

	public ArrayList<Patient> getWorkList() {
		return worklists.get(getActiveWorklistKey());
	}

	/*
	 * ************************** Getters/Setters ****************************
	 */

	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	public String getSearchString() {
		return searchString;
	}

	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}

	public List<Person> getSearchResults() {
		return searchResults;
	}

	public void setSearchResults(ArrayList<Person> searchResults) {
		this.searchResults = searchResults;
	}

	public List<Patient> getRestrictedWorkList() {
		return restrictedWorkList;
	}

	public void setRestrictedWorkList(List<Patient> restrictedWorkList) {
		this.restrictedWorkList = restrictedWorkList;
	}

	public SearchOptions getSearchOptions() {
		return searchOptions;
	}

	public void setSearchOptions(SearchOptions searchOptions) {
		this.searchOptions = searchOptions;
	}

	public int getWorklistSortOrder() {
		return worklistSortOrder;
	}

	public void setWorklistSortOrder(int worklistSortOrder) {
		this.worklistSortOrder = worklistSortOrder;
	}

	public boolean isWorklistSortOrderAcs() {
		return worklistSortOrderAcs;
	}

	public void setWorklistSortOrderAcs(boolean worklistSortOrderAcs) {
		this.worklistSortOrderAcs = worklistSortOrderAcs;
	}

	public Patient getSelectedPatient() {
		return selectedPatient;
	}

	public void setSelectedPatient(Patient selectedPatient) {
		this.selectedPatient = selectedPatient;
	}

}
