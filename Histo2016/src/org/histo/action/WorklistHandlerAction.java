package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.histo.config.enums.View;
import org.histo.config.enums.Role;
import org.histo.config.enums.Worklist;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.enums.WorklistSortOrder;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.util.SearchOptions;
import org.histo.util.TaskUtil;
import org.histo.util.TimeUtil;
import org.histo.util.WorklistSortUtil;
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

	@Autowired
	private PatientDao patientDao;

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

	@Autowired
	private MainHandlerAction mainHandlerAction;

	/*
	 * ************************** Patient ****************************
	 */

	/**
	 * Currently selected patient
	 */
	private Patient selectedPatient;

	/*
	 * ************************** Patient ****************************
	 */

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

	/**
	 * Order of the Worklist, either by id or by patient name
	 */
	private WorklistSortOrder worklistSortOrder = WorklistSortOrder.TASK_ID;

	/**
	 * Order of the Worlist, either if true ascending, or if false descending
	 */
	private boolean worklistSortOrderAcs = true;

	/**
	 * A Filter which searches for a given pattern in the current worklist (
	 */
	private String worklistFilter;

	/**
	 * If true all not active task are skipped while the user useses the up and
	 * down arrows to navigate
	 */
	private boolean skipNotActiveTasks = true;
	/*
	 * ************************** Worklist ****************************
	 */

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
			getSearchOptions().setSearchIndex(WorklistSearchOption.STAINING_LIST);
			searhCurrentWorklist();
			break;
		case USER:
			break;
		case PHYSICIAN:
		case MODERATOR:
		case ADMIN:
			getSearchOptions().setSearchIndex(WorklistSearchOption.DIAGNOSIS_LIST);
			searhCurrentWorklist();
		default:
			break;
		}
	}

	/**
	 * Action is performed on selecting a patient in the patient list on the
	 * left hand side. If the receiptlog or the diagnosis view should be used it
	 * is checked if a task is selected. If not the program will select the
	 * first (active oder not depending on skipNotActiveTasks) task.
	 * 
	 * @return
	 */
	@SuppressWarnings("incomplete-switch")
	public String onSelectPatient(Patient patient) {
		setSelectedPatient(patient);

		if (patient == null)
			return View.WORKLIST.getPath();

		switch (mainHandlerAction.getCurrentView()) {
		case WORKLIST_PATIENT:
		case WORKLIST_RECEIPTLOG:
		case WORKLIST_DIAGNOSIS:
			System.out.println("task");
			Task task = patient.getSelectedTask();
			if (task == null && !patient.getTasks().isEmpty()) {
				if (skipNotActiveTasks)
					task = TaskUtil.getLastActiveTask(patient.getTasks());
				else
					task = TaskUtil.getLastTask(patient.getTasks());
			}

			if (task != null)
				return onSelectTask(task);

		}

		return View.WORKLIST.getPath();
	}

	/**
	 * Selects a task and sets the patient of this task as selectedPatient
	 * 
	 * @param task
	 */
	public void onSelectTaskAndPatient(Task task) {

		if (mainHandlerAction.getCurrentView() != View.WORKLIST_RECEIPTLOG
				|| mainHandlerAction.getCurrentView() != View.WORKLIST_DIAGNOSIS) {
			// TODO set favorite view depending on user
			mainHandlerAction.setCurrentView(View.WORKLIST_RECEIPTLOG);
		}

		onSelectPatient(task.getPatient());
	}

	/**
	 * Task - Select and init
	 */
	public String onSelectTask(Task task) {
		System.out.println(task.getTaskID());
		// set patient.selectedTask is performed by the gui
		// sets this task as active, so it will be show in the navigation column
		// whether there is an action to perform or not
		task.setActive(true);

		task.getPatient().setSelectedTask(task);

		Role userRole = userHandlerAction.getCurrentUser().getRole();

		TaskUtil.generateSlideGuiList(task);

		// Setzte action to none
		slideHandlerAction.setActionOnMany(SlideHandlerAction.STAININGLIST_ACTION_NONE);

		// init all available diagnoses
		settingsHandlerAction.updateAllDiagnosisPrototypes();

		// init all available materials
		taskHandlerAction.prepareForTask();

		if (mainHandlerAction.getCurrentView() == View.WORKLIST_RECEIPTLOG) {
			if (userRole == Role.MTA) {
				task.setTabIndex(Task.TAB_STAINIG);
			} else {
				task.setTabIndex(Task.TAB_DIAGNOSIS);
			}
		}

		return View.WORKLIST.getPath();
	}

	public String onDeselectTask(Patient patient) {
		patient.setSelectedTask(null);
		mainHandlerAction.setCurrentView(View.WORKLIST_PATIENT);
		return View.WORKLIST.getPath();
	}

	public String getCenterView() {
		View currentView = mainHandlerAction.getCurrentView();
		if (currentView == View.WORKLIST_BLANK || currentView == View.WORKLIST_DIAGNOSIS
				|| currentView == View.WORKLIST_PATIENT || currentView == View.WORKLIST_RECEIPTLOG)
			return mainHandlerAction.getCurrentView().getPath();
		return View.WORKLIST_BLANK.getPath();
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

	/**
	 * Sorts a list with patiens either by task id or name of the patient
	 * 
	 * @param patiens
	 * @param order
	 */
	public void sortWordklist(List<Patient> patiens, WorklistSortOrder order, boolean asc) {
		switch (order) {
		case TASK_ID:
			WorklistSortUtil.orderListByTaskID(patiens, asc);
			break;
		case PIZ:
			WorklistSortUtil.orderListByPIZ(patiens, asc);
			break;
		case NAME:
			WorklistSortUtil.orderListByName(patiens, asc);
			break;
		}
	}

	/******************************************************** General ********************************************************/

	/******************************************************** Task ********************************************************/

	/******************************************************** Task ********************************************************/

	/*
	 * ************************** Worklist ****************************
	 */

	/**
	 * Searches the database for the given searchOptions and overwrites the
	 * content of the current worklist with the found content.
	 */
	public void searhCurrentWorklist() {
		searhCurrentWorklist(getSearchOptions());
	}

	/**
	 * Searches the database for the given searchOptions and overwrites the
	 * content of the current worklist with the found content.
	 */
	public void searhCurrentWorklist(SearchOptions searchOptions) {

		ArrayList<Patient> result = new ArrayList<Patient>();

		Calendar cal = Calendar.getInstance();
		Date currentDate = new Date(System.currentTimeMillis());
		cal.setTime(currentDate);

		switch (searchOptions.getSearchIndex()) {
		case STAINING_LIST:

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
		case DIAGNOSIS_LIST:
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
		case TODAY:
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case YESTERDAY:
			cal.add(Calendar.DAY_OF_MONTH, -1);
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case CURRENTWEEK:
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setWeekBeginning(cal).getTimeInMillis(),
					TimeUtil.setWeekEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case LASTWEEK:
			cal.add(Calendar.WEEK_OF_YEAR, -1);
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setWeekBeginning(cal).getTimeInMillis(),
					TimeUtil.setWeekEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case LASTMONTH:
			cal.add(Calendar.MONDAY, -1);
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setMonthBeginning(cal).getTimeInMillis(),
					TimeUtil.setMonthEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case DAY:
			cal.setTime(searchOptions.getDay());
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case MONTH:
			cal.set(Calendar.MONTH, searchOptions.getSearchMonth().getNumber());
			cal.set(Calendar.YEAR, searchOptions.getYear());
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setMonthBeginning(cal).getTimeInMillis(),
					TimeUtil.setMonthEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case TIME:
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
		if (getWorkList() != null) {
			if (getSelectedPatient() != null) {
				int indexOfPatient = getWorkList().indexOf(getSelectedPatient());
				if (getWorkList().size() - 1 > indexOfPatient)
					setSelectedPatient(getWorkList().get(indexOfPatient + 1));
			} else {
				setSelectedPatient(getWorkList().get(0));
			}
		}
	}

	public void selectPreviouseTask() {
		if (getWorkList() != null) {
			if (getSelectedPatient() != null) {
				int indexOfPatient = getWorkList().indexOf(getSelectedPatient());
				if (indexOfPatient > 0)
					setSelectedPatient(getWorkList().get(indexOfPatient - 1));
			} else {
				setSelectedPatient(getWorkList().get(getWorkList().size() - 1));
			}
		}
	}

	/*
	 * ************************** Worklist ****************************
	 */

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

	//name (in current worklist)
	//name, surname (in current worklist)
	//piz (current, extern)
	//auftrag a (current, extern)
	//bloc b (current extern)
	//slide s (current extern)
	
	public void searchForExistingPatients() {
		if (getWorklistFilter().matches("[0-9]{6,8}")) {
			System.out.println("piz");
			System.out.println(patientDao.searchForPatientsPiz(getWorklistFilter()));
		} else if (getWorklistFilter().matches("[a-zA-Z ,]*")) {
			Pattern p = Pattern.compile("[a-zA-Z-]*?[ ,]*?[a-zA-Z-]*?");
			Matcher m = p.matcher(getWorklistFilter()); // get a matcher object
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

	public Patient getSelectedPatient() {
		return selectedPatient;
	}

	public void setSelectedPatient(Patient selectedPatient) {
		this.selectedPatient = selectedPatient;
	}

	public WorklistSortOrder getWorklistSortOrder() {
		return worklistSortOrder;
	}

	public void setWorklistSortOrder(WorklistSortOrder worklistSortOrder) {
		this.worklistSortOrder = worklistSortOrder;
	}

	public boolean isWorklistSortOrderAcs() {
		return worklistSortOrderAcs;
	}

	public void setWorklistSortOrderAcs(boolean worklistSortOrderAcs) {
		this.worklistSortOrderAcs = worklistSortOrderAcs;
	}

	public SearchOptions getSearchOptions() {
		return searchOptions;
	}

	public void setSearchOptions(SearchOptions searchOptions) {
		this.searchOptions = searchOptions;
	}

	public String getWorklistFilter() {
		return worklistFilter;
	}

	public void setWorklistFilter(String worklistFilter) {
		this.worklistFilter = worklistFilter;
	}

	public boolean isSkipNotActiveTasks() {
		return skipNotActiveTasks;
	}

	public void setSkipNotActiveTasks(boolean skipNotActiveTasks) {
		this.skipNotActiveTasks = skipNotActiveTasks;
	}
	/*
	 * ************************** Getters/Setters ****************************
	 */

}
