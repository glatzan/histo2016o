package org.histo.action;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
import org.histo.config.enums.Display;
import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.Block;
import org.histo.model.Contact;
import org.histo.model.Diagnosis;
import org.histo.model.DiagnosisPrototype;
import org.histo.model.Patient;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.Sample;
import org.histo.model.StainingPrototype;
import org.histo.model.StainingPrototypeList;
import org.histo.model.Task;
import org.histo.model.UserRole;
import org.histo.model.util.ArchiveAble;
import org.histo.model.util.StainingTreeParent;
import org.histo.model.util.transientObjects.PDFTemplate;
import org.histo.ui.PatientList;
import org.histo.ui.transformer.StainingListTransformer;
import org.histo.util.PersonAdministration;
import org.histo.util.SearchOptions;
import org.histo.util.TaskUtil;
import org.histo.util.TimeUtil;
import org.histo.util.WorklistUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
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
	private GenericDAO genericDAO;
	@Autowired
	private PatientDao patientDao;
	@Autowired
	private TaskDAO taskDAO;
	@Autowired
	private HelperDAO helperDAO;
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
	
	

	/******************************************************** Patient ********************************************************/
	/**
	 * Currently selected patient
	 */
	private Patient selectedPatient;
	/******************************************************** Patient ********************************************************/

	/******************************************************** Task ********************************************************/
	/**
	 * all staininglists, default not initialized
	 */
	private List<StainingPrototypeList> allAvailableStainingLists;

	/**
	 * selected stainingList for task
	 */
	private StainingPrototypeList selectedStainingList;

	/**
	 * amount of samples for the new tasks
	 */
	private int sampleCount;

	/**
	 * Transformer for selecting staininglist
	 */
	private StainingListTransformer stainingListTransformer;
	/******************************************************** Task ********************************************************/

	/******************************************************** Archivieren ********************************************************/

	/**
	 * Variable wird zum archivieren von Objekten verwendet.
	 */
	private ArchiveAble toArchive;

	/**
	 * Wenn true wird das Objekt archiviert
	 */
	private boolean archived;
	/******************************************************** Archivieren ********************************************************/

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

	/**
	 *  
	 */
	private Display worklistDisplay = Display.PATIENT;

	/******************************************************** Worklist ********************************************************/

	private String searchType;

	private String searchString;

	/**
	 * Search Options
	 */
	private SearchOptions searchOptions;

	/**
	 * Paiten search results form external Database
	 */
	private ArrayList<Person> searchResults;

	public WorklistHandlerAction() {
	}

	/******************************************************** General ********************************************************/

	@PostConstruct
	public void goToLogin() {
		goToWorkList();
	}

	public String goToWorkList() {
		if (getWorkList() == null) {
		//	log.debug("Standard Arbeitsliste geladen");
			Date currentDate = new Date(System.currentTimeMillis());
			// standard settings patients for today
			setWorkList(new ArrayList<Patient>());

			setSearchOptions(new SearchOptions());

			// getting default worklist depending on role
			int userLevel = userHandlerAction.getCurrentUser().getRole().getLevel();

			if (userLevel == UserRole.ROLE_LEVEL_MTA) {
				getSearchOptions().setSearchIndex(SearchOptions.SEARCH_INDEX_STAINING);
				updateWorklistOptions(getSearchOptions());
			} else if (userLevel == UserRole.ROLE_LEVEL_HISTO || userLevel == UserRole.ROLE_LEVEL_MODERATOR) {
				getSearchOptions().setSearchIndex(SearchOptions.SEARCH_INDEX_DIAGNOSIS);
				updateWorklistOptions(getSearchOptions());
			} else {
				// not adding anything to workilist -> superadmin or user
			}

			setRestrictedWorkList(getWorkList());
		}

		return "/pages/worklist/workList";
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

//		log.info("Entferne Patient aus der Arbeitsliste");
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

		case DIAGNOSIS_INTERN_EXTENDED:
			if (getSelectedPatient() != null && getSelectedPatient().getSelectedTask() != null)
				return HistoSettings.CENTER_INCLUDE_EXTERN_EXTENDED;
			else if (getSelectedPatient() != null)
				return HistoSettings.CENTER_INCLUDE_PATIENT;
			else
				return HistoSettings.CENTER_INCLUDE_BLANK;
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

		if(task.getPatient().getSelectedTask() != task)
			task.getPatient().setSelectedTask(task);
		
//		log.info("Select and init sample");

		int userLevel = userHandlerAction.getCurrentUser().getRole().getLevel();

		task.generateStainingGuiList();

		if (userLevel == UserRole.ROLE_LEVEL_MTA) {
			task.setTabIndex(Task.TAB_STAINIG);
		} else {
			task.setTabIndex(Task.TAB_DIAGNOSIS);
		}

		// Setzte action to none
		slideHandlerAction.setActionOnMany(SlideHandlerAction.STAININGLIST_ACTION_PERFORMED);

		// init all available diagnoses
		settingsHandlerAction.updateAllDiagnosisPrototypes();

		// setzte das diasplay auf das eingangsbuch wenn der patient angezeigt
		// wird
		if (getWorklistDisplay() == Display.PATIENT)
			setWorklistDisplay(Display.RECEIPTLOG);
	}

	public void deselectTask(Patient patient) {
		setWorklistDisplay(Display.PATIENT);
		patient.setSelectedTask(null);
	}

	/**
	 * Displays a dialog for creating a new task
	 */
	public void prepareNewTaskDialog() {
		setAllAvailableStainingLists(helperDAO.getAllStainingLists());
		// checks if default statingsList is empty
		if (!getAllAvailableStainingLists().isEmpty()) {
			setSelectedStainingList(getAllAvailableStainingLists().get(0));
			setStainingListTransformer(new StainingListTransformer(getAllAvailableStainingLists()));
		}

		setSampleCount(1);
		helper.showDialog(HistoSettings.dialog(HistoSettings.DIALOG_CREATE_TASK));
	}

	/**
	 * Creates a new Task for the given Patient
	 * 
	 * @param patient
	 */
	public void createNewTask(Patient patient, StainingPrototypeList stainingPrototypeList, int sampleCount) {
		if (patient.getTasks() == null) {
			patient.setTasks(new ArrayList<>());
		}

		Task task = TaskUtil.createNewTask(patient, taskDAO.countSamplesOfCurrentYear());

		task.setTypeOfMaterial(stainingPrototypeList);
		task.setMaterialName(stainingPrototypeList.getName());

		// TODO material -> in gui
		patient.getTasks().add(0, task);
		// sets the new task as the selected task
		patient.setSelectedTask(task);

		genericDAO.save(patient);

//		log.info("Neuer Auftrag erstell: TaskID:" + task.getTaskID(), patient);

		for (int i = 0; i < sampleCount; i++) {
			// autogenerating first sample
			createNewSample(task, stainingPrototypeList);
		}

		genericDAO.save(patient);

		hideNewTaskDialog();
	}

	public void hideNewTaskDialog() {
		helper.hideDialog(HistoSettings.dialog(HistoSettings.DIALOG_CREATE_TASK));
	}

	/******************************************************** Task ********************************************************/

	/******************************************************** Sample ********************************************************/
	/**
	 * Adds a new Sample with one diagnosis an the standard stainings.
	 * 
	 * @param task
	 */
	public void createNewSample(Task task, StainingPrototypeList stainingPrototypeList) {
		// TODO remove task and statningP list an use it from
		// patient.selectedTask....
		Sample newSample = TaskUtil.createNewSample(task);

//		log.info("Neue Probe erstellt: TaskID: " + task.getTaskID() + ", SampleID: " + newSample.getSampleID(),
//				getSelectedPatient());

		genericDAO.save(newSample);

		// creating first default diagnosis
		diagnosisHandlerAction.createNewDiagnosis(newSample, Diagnosis.TYPE_DIAGNOSIS);

		// creating needed blocks
		createNewBlock(newSample, stainingPrototypeList);

	}

	/******************************************************** Sample ********************************************************/

	/******************************************************** Block ********************************************************/
	public void createNewBlock(Sample sample, StainingPrototypeList stainingPrototypeList) {
		Block block = TaskUtil.createNewBlock(sample);

//		log.info("Neuen Block erstellt: SampleID: " + sample.getSampleID() + ", BlockID: " + block.getBlockID(),
//				getSelectedPatient());

		genericDAO.save(block);

		// adding standard staining
		for (StainingPrototype proto : stainingPrototypeList.getStainingPrototypes()) {
			slideHandlerAction.addStaining(proto, block);
		}

		// updating Gui
		sample.getParent().generateStainingGuiList();
	}

	/******************************************************** Block ********************************************************/

	/******************************************************** Archivieren ********************************************************/
	/**
	 * Zeigt eine Dialog zum archiveren von Sample/Block/Task/Objektträger an
	 * 
	 * @param sample
	 * @param archived
	 */
	public void prepareArchiveObjectForStainingList(StainingTreeParent<?> archive, boolean archived) {
		setArchived(archived);
		setToArchive(archive);
		// wenn kein Dialog vorhanden ist wird das objekt sofort archiviert
		if (archive.getArchiveDialog() == null)
			archiveObjectForStainingList(archive, archived);
		else
			helper.showDialog(archive.getArchiveDialog());
	}

	/**
	 * Archiviert einen Sample/Block/Task/Objektträger und updated die
	 * ObjektträgerListe
	 * 
	 * @param task
	 * @param archiveAble
	 * @param archived
	 */
	public void archiveObjectForStainingList(StainingTreeParent<?> archive, boolean archived) {

		archive.setArchived(archived);
		genericDAO.save(archive);

		// updated die Objektträgerliste
		((StainingTreeParent<?>) archive).getPatient().getSelectedTask().generateStainingGuiList();

		// versteckt den dialog
		hideObjectForStainingListDialog();
	}

	/**
	 * Versteckt den Dialog zum Archivieren von Sample/Block/Task/Objektträger
	 */
	public void hideObjectForStainingListDialog() {
		helper.hideDialog(getToArchive().getArchiveDialog());
	}

	/******************************************************** Archivieren ********************************************************/

	/******************************************************** Worklistoptions ********************************************************/
	public void prepareWorklistOptions() {
		helper.showDialog(HistoSettings.dialog(HistoSettings.DIALOG_WORKLIST_OPTIONS), 650, 470, true, false, false);
	}

	public void hideWorklistOptions() {
		helper.hideDialog(HistoSettings.dialog(HistoSettings.DIALOG_WORKLIST_OPTIONS));
	}

	public void updateWorklistOptions(SearchOptions searchOptions) {

//		log.info("Select worklist");

		Calendar cal = Calendar.getInstance();
		Date currentDate = new Date(System.currentTimeMillis());
		cal.setTime(currentDate);

//		log.inf7o("Current Day - " + cal.getTime());

		switch (searchOptions.getSearchIndex()) {
		case SearchOptions.SEARCH_INDEX_STAINING:
			getWorkList().clear();

			System.out.println(TimeUtil.setDayBeginning(cal).getTime() + " " + TimeUtil.setDayEnding(cal).getTime());
			if (searchOptions.isStaining_new()) {
				getWorkList().addAll(patientDao.getPatientWithoutTasks(TimeUtil.setDayBeginning(cal).getTime(),
						TimeUtil.setDayEnding(cal).getTime()));
			}

			if (searchOptions.isStaining_staining() && searchOptions.isStaining_restaining()) {
				getWorkList().addAll(patientDao.getPatientByStainingsBetweenDates(new Date(0),
						new Date(System.currentTimeMillis()), false));
			} else {
				List<Patient> paints = patientDao.getPatientByStainingsBetweenDates(new Date(0),
						new Date(System.currentTimeMillis()), false);
				for (Patient patient : paints) {
					if (searchOptions.isStaining_staining() && patient.isStainingNeeded()) {
						getWorkList().add(patient);
					} else if (searchOptions.isStaining_restaining() && patient.isReStainingNeeded()) {
						getWorkList().add(patient);
					}
				}
			}

//			log.info("Staining list");
			break;
		case SearchOptions.SEARCH_INDEX_DIAGNOSIS:
			getWorkList().clear();
			if (searchOptions.isStaining_diagnosis() && searchOptions.isStaining_rediagnosis()) {
				getWorkList().addAll(patientDao.getPatientByDiagnosBetweenDates(new Date(0),
						new Date(System.currentTimeMillis()), false));
			} else {
				List<Patient> paints = patientDao.getPatientByDiagnosBetweenDates(new Date(0),
						new Date(System.currentTimeMillis()), false);
				for (Patient patient : paints) {
					if (searchOptions.isStaining_diagnosis() && patient.isDiagnosisNeeded()) {
						getWorkList().add(patient);
					} else if (searchOptions.isStaining_rediagnosis() && patient.isReDiagnosisNeeded()) {
						getWorkList().add(patient);
					}
				}
			}
//			log.info("Diagnosis list");
			break;
		case SearchOptions.SEARCH_INDEX_TODAY:
			getWorkList().clear();
			getWorkList().addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTime(),
					TimeUtil.setDayEnding(cal).getTime(), searchOptions.getFilterIndex()));
//			log.info("Worklist today - " + TimeUtil.setDayBeginning(cal).getTime() + " "
//					+ TimeUtil.setDayEnding(cal).getTime());
			break;
		case SearchOptions.SEARCH_INDEX_YESTERDAY:
			getWorkList().clear();
			cal.add(Calendar.DAY_OF_MONTH, -1);
			getWorkList().addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTime(),
					TimeUtil.setDayEnding(cal).getTime(), searchOptions.getFilterIndex()));
//			log.info("Worklist yesterday - " + TimeUtil.setDayBeginning(cal).getTime() + " "
//					+ TimeUtil.setDayEnding(cal).getTime());
			break;
		case SearchOptions.SEARCH_INDEX_CURRENTWEEK:
			getWorkList().clear();
			getWorkList().addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setWeekBeginning(cal).getTime(),
					TimeUtil.setWeekEnding(cal).getTime(), searchOptions.getFilterIndex()));
//			log.info("Worklist current week - " + TimeUtil.setWeekBeginning(cal).getTime() + " "
//					+ TimeUtil.setWeekEnding(cal).getTime());
			break;
		case SearchOptions.SEARCH_INDEX_LASTWEEK:
			getWorkList().clear();
			cal.add(Calendar.WEEK_OF_YEAR, -1);
			getWorkList().addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setWeekBeginning(cal).getTime(),
					TimeUtil.setWeekEnding(cal).getTime(), searchOptions.getFilterIndex()));
//			log.info("Worklist last week - " + TimeUtil.setWeekBeginning(cal).getTime() + " "
//					+ TimeUtil.setWeekEnding(cal).getTime());
			break;
		case SearchOptions.SEARCH_INDEX_LASTMONTH:
			getWorkList().clear();
			cal.add(Calendar.MONDAY, -1);
			getWorkList().addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setMonthBeginning(cal).getTime(),
					TimeUtil.setMonthEnding(cal).getTime(), searchOptions.getFilterIndex()));
//			log.info("Worklist last month - " + TimeUtil.setMonthBeginning(cal).getTime() + " "
//					+ TimeUtil.setMonthEnding(cal).getTime());
			break;
		case SearchOptions.SEARCH_INDEX_DAY:
			cal.setTime(searchOptions.getDay());
			getWorkList().clear();
			getWorkList().addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTime(),
					TimeUtil.setDayEnding(cal).getTime(), searchOptions.getFilterIndex()));
//			l´7g.info("Day - " + TimeUtil.setDayBeginning(cal).getTime() + " " + TimeUtil.setDayEnding(cal).getTime());
			break;
		case SearchOptions.SEARCH_INDEX_MONTH:
			cal.set(Calendar.MONTH, searchOptions.getSearchMonth());
			cal.set(Calendar.YEAR, searchOptions.getYear());
			getWorkList().addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setMonthBeginning(cal).getTime(),
					TimeUtil.setMonthEnding(cal).getTime(), searchOptions.getFilterIndex()));
//			log.info("Worklist month - " + TimeUtil.setMonthBeginning(cal).getTime() + " "
//					+ TimeUtil.setMonthEnding(cal).getTime());
			break;
		case SearchOptions.SEARCH_INDEX_TIME:
			cal.setTime(searchOptions.getSearchFrom());
			Date fromTime = TimeUtil.setDayBeginning(cal).getTime();
			cal.setTime(searchOptions.getSearchTo());
			Date toTime = TimeUtil.setDayEnding(cal).getTime();
			getWorkList()
					.addAll(patientDao.getWorklistDynamicallyByType(fromTime, toTime, searchOptions.getFilterIndex()));
//			log.info("Worklist time - " + fromTime + " " + toTime);
			break;
		default:
			break;
		}

		setRestrictedWorkList(getWorkList());
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

	/******************************************************** Worklistoptions ********************************************************/

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

	public List<Patient> getWorkList() {
		return workList;
	}

	public void setWorkList(List<Patient> workList) {
		this.workList = workList;
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

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public List<StainingPrototypeList> getAllAvailableStainingLists() {
		return allAvailableStainingLists;
	}

	public void setAllAvailableStainingLists(List<StainingPrototypeList> allAvailableStainingLists) {
		this.allAvailableStainingLists = allAvailableStainingLists;
	}

	public StainingPrototypeList getSelectedStainingList() {
		return selectedStainingList;
	}

	public void setSelectedStainingList(StainingPrototypeList selectedStainingList) {
		System.out.println("setting statingn lsit" + selectedStainingList);
		this.selectedStainingList = selectedStainingList;
	}

	public int getSampleCount() {
		return sampleCount;
	}

	public void setSampleCount(int sampleCount) {
		this.sampleCount = sampleCount;
	}

	public StainingListTransformer getStainingListTransformer() {
		return stainingListTransformer;
	}

	public void setStainingListTransformer(StainingListTransformer stainingListTransformer) {
		this.stainingListTransformer = stainingListTransformer;
	}

	public ArchiveAble getToArchive() {
		return toArchive;
	}

	public void setToArchive(ArchiveAble toArchive) {
		this.toArchive = toArchive;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	public int getWorklistSortOrder() {
		return worklistSortOrder;
	}

	public void setWorklistSortOrder(int worklistSortOrder) {
		this.worklistSortOrder = worklistSortOrder;
	}

	public Display getWorklistDisplay() {
		return worklistDisplay;
	}

	public void setWorklistDisplay(Display worklistDisplay) {
		System.out.println(worklistDisplay + "-----------");
		this.worklistDisplay = worklistDisplay;
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

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
