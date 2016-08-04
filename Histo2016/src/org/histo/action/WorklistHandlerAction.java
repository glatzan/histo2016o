package org.histo.action;

import java.awt.Paint;
import java.awt.Robot;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
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
import org.histo.model.Staining;
import org.histo.model.StainingPrototype;
import org.histo.model.StainingPrototypeList;
import org.histo.model.Task;
import org.histo.model.UserRole;
import org.histo.ui.StainingListTransformer;
import org.histo.util.PersonAdministration;
import org.histo.util.SearchOptions;
import org.histo.util.TaskUtil;
import org.histo.util.TimeUtil;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;

import histo.model.util.ArchiveAble;
import histo.model.util.StainingTreeParent;

@Component
@Scope(value = "session")
public class WorklistHandlerAction implements Serializable {

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

	public final static int SORT_ORDER_ID = 0;
	public final static int SORT_ORDER_NAME = 1;

	public final static int DISPLAY_PATIENT = 0;
	public final static int DISPLAY_RECEIPTLOG = 1;
	public final static int DISPLAY_EXTERNAL_RESULT_SHORT = 2;
	public final static int DISPLAY_EXTERNAL_RESULT_EXTENDED = 3;
	public final static int DISPLAY_INTERNAL_RESULT_SHORT = 4;
	public final static int DISPLAY_INTERNAL_RESULT_EXTENDED = 5;

	private static Logger log = Logger.getLogger(WorklistHandlerAction.class.getName());

	@Autowired
	private GenericDAO genericDAO;
	@Autowired
	private PatientDao patientDao;
	@Autowired
	private TaskDAO taskDAO;
	@Autowired
	private HelperDAO helperDAO;
	@Autowired
	private PhysicianDAO physicianDAO;

	@Autowired
	private HelperHandlerAction helper;
	@Autowired
	@Lazy
	private SlideHandlerAction slideHandlerAction;
	@Autowired
	private UserHandlerAction userHandlerAction;
	@Autowired
	@Lazy
	private SettingsHandlerAction settingsHandlerAction;

	/******************************************************** Patient ********************************************************/
	/**
	 * Tabindex of the addPatient dialog
	 */
	private int activePatientDialogIndex = 0;

	/**
	 * Currently selected patient
	 */
	private Patient selectedPatient;

	/**
	 * Patientdummy for creating a new patient
	 */
	private Patient tmpPatient;
	
	private List<Patient> searchForPatientList;

	private String searchForPatientPiz;

	private String searchForPatientName;

	private String searchForPatientSurname;

	private Date searchForPatientBirthday;
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

	/******************************************************** Diagnosis ********************************************************/
	/**
	 * Used for selecting a new diagnosis
	 */
	private DiagnosisPrototype tmpDiagnosisPrototype;
	/******************************************************** Diagnosis ********************************************************/

	/******************************************************** Contact ********************************************************/
	private List<Contact> allAvailableContact;

	private boolean personSurgeon = true;

	private boolean personExtern = true;

	private boolean personOther = true;

	private boolean addedContacts = false;
	/******************************************************** Contact ********************************************************/

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
	 * Art wie die Worklist sortiert werden soll
	 */
	private int worklistSortOrder = SORT_ORDER_ID;

	/**
	 *  
	 */
	private int worklistDisplay = DISPLAY_PATIENT;

	/******************************************************** Worklist ********************************************************/

	private String searchType;

	private String searchString;

	/**
	 * Search Options
	 */
	private SearchOptions searchOptions;

	/**
	 * Diagnosis for manipulating
	 */
	private Diagnosis tmpDiagnosis;

	/**
	 * Paiten search results form external Database
	 */
	private ArrayList<Person> searchResults;

	public WorklistHandlerAction() {
	}

	/******************************************************** General ********************************************************/

	@PostConstruct
	public void goToLogin() {
		helper.log.debug("Login erfolgreich", log);
		goToWorkList();
	}

	public String goToWorkList() {
		if (getWorkList() == null) {
			helper.log.debug("Standard Arbeitsliste geladen", log);
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
	 * TODO
	 */
	public void addToWorkList() {
		setTmpPatient(new Patient());
		getTmpPatient().setPerson(new Person());
		setSearchResults(new ArrayList<>());

		helper.showDialog("/pages/dialog/addToWorkList", true, false, true);
		System.out.println("addToWorkListDialog");
	}

	public void removeFromWorklist(Patient patient) {
		System.out.println(patient + " " + patient.getPerson().getName());
		getWorkList().remove(patient);
		if (getSelectedPatient() == patient)
			setSelectedPatient(null);

		log.info("Entferne Patient aus der Arbeitsliste");
	}

	public String getCenterView(int view) {
		switch (view) {
		case DISPLAY_PATIENT:
			if (getSelectedPatient() == null)
				return HistoSettings.CENTER_INCLUDE_BLANK;
			else
				return HistoSettings.CENTER_INCLUDE_PATIENT;
		case DISPLAY_RECEIPTLOG:
			if (getSelectedPatient() != null && getSelectedPatient().getSelectedTask() != null)
				return HistoSettings.CENTER_INCLUDE_RECEIPTLOG;
			else if (getSelectedPatient() != null)
				return HistoSettings.CENTER_INCLUDE_PATIENT;
			else
				return HistoSettings.CENTER_INCLUDE_BLANK;
		case DISPLAY_EXTERNAL_RESULT_EXTENDED:
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

	// <p:panel style="width:100%;"
	// rendered="#{worklistHandlerAction.selectedPatient ne null and
	// worklistHandlerAction.selectedPatient.selectedTask eq null}"
	// styleClass=" collapsedBorders noPadding noBorders">
	// <ui:include src="workListPatient.xhtml"></ui:include>
	// </p:panel>
	// <!-- Übersicht -->
	//
	// <!-- Auftrag -->
	// <h:panelGroup styleClass="contentHolder" layout="block"
	// rendered="#{worklistHandlerAction.selectedPatient !=null and
	// worklistHandlerAction.selectedPatient.selectedTask !=null}">
	// <ui:include src="workListTask.xhtml"></ui:include>
	// </h:panelGroup>
	/******************************************************** General ********************************************************/

	/******************************************************** Patient ********************************************************/

	/**
	 * Shows the "/pages/dialog/patient/addPatient" dialog and creates an new
	 * empty patient.
	 */
	public void prepareAddPatient() {
		setTmpPatient(new Patient());
		getTmpPatient().setPerson(new Person());

		helper.showDialog("/pages/dialog/patient/addPatient", 1024, 500, true, false, false);
	}

	/**
	 * Adds an external Patient to the database and worklist
	 * 
	 * @param patient
	 *            Patient to save in the database and add to worklist
	 */
	public void createNewPatient(Patient patient) {
		// maks the patient as externally
		patient.setExternalPatient(true);
		patient.setAddDate(new Date(System.currentTimeMillis()));

		genericDAO.save(patient.getPerson());
		genericDAO.save(patient);
		getWorkList().add(tmpPatient);
		setSelectedPatient(getTmpPatient());

		helper.log.info("Neuer externer Patient erstellt, " + patient.asGson(), log, patient);

		hidePatientDialog();
	}

	/**
	 * Hides the "/pages/dialog/patient/addPatient" dialog
	 */
	public void hidePatientDialog() {
		setTmpPatient(null);
		helper.hideDialog("/pages/dialog/patient/addPatient");
	}

	/**
     * http://auginfo/piz?name=xx&vorname=xx&geburtsdatum=2000-01-01
     * @param piz
     */
    public void searchPatient(String piz, String name, String surename, Date birthday){
    	
    	PersonAdministration admim = new PersonAdministration();
    	
    	// if piz is given ignore other parameters 
    	if(piz != null && piz.matches("^[0-9]{6,8}$")){
    		List<Patient> patients = patientDao.searchForPatientsPiz(piz);
    		
    		// updates all patients from the local database with data from the clinic backend
    		for (Patient patient : patients) {
    			String userResult = admim.getRequest(HistoSettings.PATIENT_GET_URL+"/"+patient.getPiz());
    			admim.updatePatientFromClinicJson(patient, userResult);
			}
    		
    		// saves the results
    		genericDAO.save(patients);
    		
    		// only get patient from clinic backend if piz is completely provided and was not added to the local database before
    		if(piz.matches("^[0-9]{8}$") && patients.isEmpty()){
    			String userResult = admim.getRequest(HistoSettings.PATIENT_GET_URL+"/"+piz);
    			patients.add(admim.getPatientFromClinicJson(userResult));
    		}
    	
    		setSearchForPatientList(patients);
    		return;
    	}
    	
    	return;
    }

	/******************************************************** Patient ********************************************************/

	/******************************************************** Task ********************************************************/
	/**
	 * Task - Select and init
	 */
	public void selectTask(Task task) {
		// set patient.selectedTask is performed by the gui
		task.setInitialized(true);

		log.info("Select and init sample");

		int userLevel = userHandlerAction.getCurrentUser().getRole().getLevel();

		task.generateStainingGuiList();

		if (userLevel == UserRole.ROLE_LEVEL_MTA) {
			task.setTabIndex(Task.TAB_STAINIG);
		} else {
			task.setTabIndex(Task.TAB_DIAGNOSIS);
		}

		// Setzte action to none
		slideHandlerAction.setActionOnMany(SlideHandlerAction.STAININGLIST_ACTION_NONE);

		// init all available diagnoses
		settingsHandlerAction.updateAllDiagnosisPrototypes();

		// setzte das diasplay auf das eingangsbuch wenn der patient angezeigt
		// wird
		if (getWorklistDisplay() == DISPLAY_PATIENT)
			setWorklistDisplay(DISPLAY_RECEIPTLOG);
	}

	public void deselectTask(Patient patient) {
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

		helper.log.info("Neuer Auftrag erstell: TaskID:" + task.getTaskID(), log, patient);

		for (int i = 0; i < sampleCount; i++) {
			// autogenerating first sample
			createNewSample(task, stainingPrototypeList);
		}

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

		helper.log.info("Neue Probe erstellt: TaskID: " + task.getTaskID() + ", SampleID: " + newSample.getSampleID(),
				log, getSelectedPatient());

		// creating first default diagnosis
		createNewDiagnosis(newSample, Diagnosis.TYPE_DIAGNOSIS);

		genericDAO.save(newSample);

		createNewBlock(newSample, stainingPrototypeList);

		// updating Gui
		task.generateStainingGuiList();

	}

	/******************************************************** Sample ********************************************************/

	/******************************************************** Block ********************************************************/
	public void createNewBlock(Sample sample, StainingPrototypeList stainingPrototypeList) {
		Block block = TaskUtil.createNewBlock(sample);

		helper.log.info("Neuen Block erstellt: SampleID: " + sample.getSampleID() + ", BlockID: " + block.getBlockID(),
				log, getSelectedPatient());

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

	/******************************************************** Diagnosis ********************************************************/
	public void updateDiagnosisPrototype(Diagnosis diagnosis, DiagnosisPrototype diagnosisPrototype) {
		if (diagnosisPrototype != null) {
			diagnosis.setDiagnosis(diagnosisPrototype.getName());
		}
	}

	/**
	 * Creates an new Diagnosis and adds it to the sample
	 * 
	 * @param sample
	 * @param type
	 */
	public void createNewDiagnosis(Sample sample, int type) {
		Diagnosis newDiagnosis = TaskUtil.createNewDiagnosis(sample, type);
		genericDAO.save(newDiagnosis);

		// updateSample(sample);

		genericDAO.save(sample);

		helper.log.info(
				"Neue Diagnose erstellt: SampleID: " + sample.getSampleID() + ", DiagnosisID/Name: "
						+ newDiagnosis.getId() + " - " + TaskUtil.getDiagnosisName(sample, newDiagnosis),
				log, sample.getPatient());
	}

	/******************************************************** Diagnosis ********************************************************/

	/******************************************************** Contact ********************************************************/
	/**
	 * Zeigt einen Dialog zum verwalten der Benachrichtigugen/Kontakte für eine
	 * Aufgabe.
	 * 
	 * @param task
	 * @param surgeon
	 * @param extern
	 * @param other
	 * @param addedContact
	 */
	public void prepareContacts(Task task, boolean surgeon, boolean extern, boolean other, boolean addedContact) {

		genericDAO.refresh(task);

		setAllAvailableContact(new ArrayList<Contact>());

		List<Contact> contacts = task.getContacts();

		List<Physician> databaseContacts = physicianDAO.getPhysicians(surgeon, extern, other);

		if (!addedContact) {
			loop: for (Physician physician : databaseContacts) {
				for (Contact contact : contacts) {
					if (contact.getPhysician().getId() == physician.getId()) {
						contact.setSelected(true);
						getAllAvailableContact().add(contact);
						System.out.println("found continue");
						continue loop;
					}
				}

				getAllAvailableContact().add(new Contact(physician));

			}
			// Nur bereits verwendete Kontakte anzeigen
		} else {
			getAllAvailableContact().addAll(contacts);
		}

		helper.showDialog(HistoSettings.dialog(HistoSettings.DIALOG_WORKLIST_CONTACTS), 1024, 500, false, false, true);
	}

	/**
	 * Aktualisiert die Liste der vorhanden Kontakte.
	 * 
	 * @param contacts
	 * @param task
	 */
	public void updateContactList(List<Contact> contacts, Task task) {
		for (Contact contact : contacts) {
			if (contact.isSelected()) {
				if (contact.getRole() == 0) {
					task.getContacts().remove(contact);
				}
				continue;
			} else if (contact.getRole() != 0)
				task.getContacts().add(contact);
		}

		genericDAO.save(task);
		hideContactsDialog();
	}

	/**
	 * Sobald im Kontaktdialog ein neuer Kontakt ausgewählt wird, wird je nach
	 * Art eine Benachrichtigung vorausgewählt.
	 * 
	 * @param contact
	 */
	public void onContactChangeRole(Contact contact) {
		// contact wurde deselektiert alles auf nicht benutzt setzten
		if (contact.getRole() == Contact.ROLE_NONE) {
			contact.setUseEmail(false);
			contact.setUseFax(false);
			contact.setUsePhone(false);
		} else {
			// es wurde schon etwas ausgewählt, alles so belassen wie es war
			if (contact.isUseEmail() || contact.isUsePhone() || contact.isUseFax())
				return;

			// bei internen operateuren mail bevorzugen
			if (contact.getRole() == Contact.ROLE_SURGEON) {
				contact.setUseEmail(true);
				return;
			}

			// bei externen die eine Faxnummer haben fax bevorzugen
			if (contact.getRole() == Contact.ROLE_EXTERN && contact.getPhysician().getPerson().getFax() != null
					&& !contact.getPhysician().getPerson().getFax().isEmpty()) {
				contact.setUseFax(true);
				return;
			}
			// in allen anderen fällen email setzten
			if (contact.getPhysician().getPerson().getEmail() != null
					&& !contact.getPhysician().getPerson().getEmail().isEmpty())
				contact.setUseEmail(true);
		}
	}

	/**
	 * Schließt den Kontakt Dialog
	 */
	public void hideContactsDialog() {
		helper.hideDialog(HistoSettings.dialog(HistoSettings.DIALOG_WORKLIST_CONTACTS));
	}

	/******************************************************** Contact ********************************************************/

	/******************************************************** Worklistoptions ********************************************************/
	public void prepareWorklistOptions() {
		helper.showDialog(HistoSettings.dialog(HistoSettings.DIALOG_WORKLIST_OPTIONS), 635, 455, false, false, true);
	}

	public void hideWorklistOptions() {
		helper.hideDialog(HistoSettings.dialog(HistoSettings.DIALOG_WORKLIST_OPTIONS));
	}

	public void updateWorklistOptions(SearchOptions searchOptions) {

		log.info("Select worklist");

		Calendar cal = Calendar.getInstance();
		Date currentDate = new Date(System.currentTimeMillis());
		cal.setTime(currentDate);

		log.info("Current Day - " + cal.getTime());

		switch (searchOptions.getSearchIndex()) {
		case SearchOptions.SEARCH_INDEX_STAINING:
			getWorkList().clear();

			if (searchOptions.isStaining_new()) {
				getWorkList().addAll(patientDao.getPatientWithoutTasks(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
						TimeUtil.setDayEnding(cal).getTimeInMillis()));
			}

			if (searchOptions.isStaining_staining() && searchOptions.isStaining_restaining()) {
				getWorkList()
						.addAll(patientDao.getPatientByStainingsBetweenDates(0, System.currentTimeMillis(), false));
			} else {
				List<Patient> paints = patientDao.getPatientByStainingsBetweenDates(0, System.currentTimeMillis(),
						false);
				for (Patient patient : paints) {
					if (searchOptions.isStaining_staining() && patient.isStainingNeeded()) {
						getWorkList().add(patient);
					} else if (searchOptions.isStaining_restaining() && patient.isReStainingNeeded()) {
						getWorkList().add(patient);
					}
				}
			}

			log.info("Staining list");
			break;
		case SearchOptions.SEARCH_INDEX_DIAGNOSIS:
			getWorkList().clear();
			if (searchOptions.isStaining_diagnosis() && searchOptions.isStaining_rediagnosis()) {
				getWorkList().addAll(patientDao.getPatientByDiagnosBetweenDates(0, System.currentTimeMillis(), false));
			} else {
				List<Patient> paints = patientDao.getPatientByDiagnosBetweenDates(0, System.currentTimeMillis(), false);
				for (Patient patient : paints) {
					if (searchOptions.isStaining_diagnosis() && patient.isDiagnosisNeeded()) {
						getWorkList().add(patient);
					} else if (searchOptions.isStaining_rediagnosis() && patient.isReDiagnosisNeeded()) {
						getWorkList().add(patient);
					}
				}
			}
			log.info("Diagnosis list");
			break;
		case SearchOptions.SEARCH_INDEX_TODAY:
			getWorkList().clear();
			getWorkList()
					.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
							TimeUtil.setDayEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			log.info("Worklist today - " + new Date(TimeUtil.setDayBeginning(cal).getTimeInMillis()) + " "
					+ new Date(TimeUtil.setDayEnding(cal).getTimeInMillis()));
			break;
		case SearchOptions.SEARCH_INDEX_YESTERDAY:
			getWorkList().clear();
			cal.add(Calendar.DAY_OF_MONTH, -1);
			getWorkList()
					.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
							TimeUtil.setDayEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			log.info("Worklist yesterday - " + new Date(TimeUtil.setDayBeginning(cal).getTimeInMillis()) + " "
					+ new Date(TimeUtil.setDayEnding(cal).getTimeInMillis()));
			break;
		case SearchOptions.SEARCH_INDEX_CURRENTWEEK:
			getWorkList().clear();
			getWorkList()
					.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setWeekBeginning(cal).getTimeInMillis(),
							TimeUtil.setWeekEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			log.info("Worklist current week - " + new Date(TimeUtil.setWeekBeginning(cal).getTimeInMillis()) + " "
					+ new Date(TimeUtil.setWeekEnding(cal).getTimeInMillis()));
			break;
		case SearchOptions.SEARCH_INDEX_LASTWEEK:
			getWorkList().clear();
			cal.add(Calendar.WEEK_OF_YEAR, -1);
			getWorkList()
					.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setWeekBeginning(cal).getTimeInMillis(),
							TimeUtil.setWeekEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			log.info("Worklist last week - " + new Date(TimeUtil.setWeekBeginning(cal).getTimeInMillis()) + " "
					+ new Date(TimeUtil.setWeekEnding(cal).getTimeInMillis()));
			break;
		case SearchOptions.SEARCH_INDEX_LASTMONTH:
			getWorkList().clear();
			cal.add(Calendar.MONDAY, -1);
			getWorkList()
					.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setMonthBeginning(cal).getTimeInMillis(),
							TimeUtil.setMonthEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			log.info("Worklist last month - " + new Date(TimeUtil.setMonthBeginning(cal).getTimeInMillis()) + " "
					+ new Date(TimeUtil.setMonthEnding(cal).getTimeInMillis()));
			break;
		case SearchOptions.SEARCH_INDEX_DAY:
			cal.setTime(searchOptions.getDay());
			getWorkList().clear();
			getWorkList()
					.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
							TimeUtil.setDayEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			log.info("Day - " + new Date(TimeUtil.setDayBeginning(cal).getTimeInMillis()) + " "
					+ new Date(TimeUtil.setDayEnding(cal).getTimeInMillis()));
			break;
		case SearchOptions.SEARCH_INDEX_MONTH:
			cal.set(Calendar.MONTH, searchOptions.getSearchMonth());
			cal.set(Calendar.YEAR, searchOptions.getYear());
			getWorkList()
					.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setMonthBeginning(cal).getTimeInMillis(),
							TimeUtil.setMonthEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			log.info("Worklist month - " + new Date(TimeUtil.setMonthBeginning(cal).getTimeInMillis()) + " "
					+ new Date(TimeUtil.setMonthEnding(cal).getTimeInMillis()));
			break;
		case SearchOptions.SEARCH_INDEX_TIME:
			cal.setTime(searchOptions.getSearchFrom());
			long fromTime = TimeUtil.setDayBeginning(cal).getTimeInMillis();
			cal.setTime(searchOptions.getSearchTo());
			long toTime = TimeUtil.setDayEnding(cal).getTimeInMillis();
			getWorkList()
					.addAll(patientDao.getWorklistDynamicallyByType(fromTime, toTime, searchOptions.getFilterIndex()));
			log.info("Worklist time - " + new Date(fromTime) + " " + new Date(toTime));
			break;
		default:
			break;
		}

		setRestrictedWorkList(getWorkList());
		setSelectedPatient(null);
	}

	public void sortWorklist(List<Patient> worklist, int mode) {

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

	public void prepareFinalizeDiagnosis(Diagnosis diagnosis) {
		setTmpDiagnosis(diagnosis);

		helper.showDialog("/pages/dialog/task/finalizeConfirm", true, false, true);
		log.info("Shown finalizes diagnosis warning");
	}

	public void finalizeDiagnosis(Task task, Diagnosis diagnosis, boolean finalize) {

		if (finalize) {
			diagnosis.setFinalized(true);
			genericDAO.save(diagnosis);
		}

		for (Sample sample : task.getSamples()) {
			// updateSample(sample);
		}

		genericDAO.save(task);

		log.info("Shown finalizes diagnosis warning");
		helper.hideDialog("/pages/dialog/task/finalizeConfirm");
	}

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

	/**
	 * Saves the given Object to the database
	 * 
	 * @param object
	 */
	public void saveObject(Object object) {
		genericDAO.save(object);
	}

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

	public void saveCurrentPatient() {
		genericDAO.save(getSelectedPatient());
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

	public Patient getSelectedPatient() {
		return selectedPatient;
	}

	public void setSelectedPatient(Patient selectedPatient) {
		this.selectedPatient = selectedPatient;
	}

	public Patient getTmpPatient() {
		return tmpPatient;
	}

	public void setTmpPatient(Patient tmpPatient) {
		this.tmpPatient = tmpPatient;
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

	public Diagnosis getTmpDiagnosis() {
		return tmpDiagnosis;
	}

	public void setTmpDiagnosis(Diagnosis tmpDiagnosis) {
		this.tmpDiagnosis = tmpDiagnosis;
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
	public int getActivePatientDialogIndex() {
		return activePatientDialogIndex;
	}

	public void setActivePatientDialogIndex(int activePatientDialogIndex) {
		this.activePatientDialogIndex = activePatientDialogIndex;
	}

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

	public DiagnosisPrototype getTmpDiagnosisPrototype() {
		return tmpDiagnosisPrototype;
	}

	public void setTmpDiagnosisPrototype(DiagnosisPrototype tmpDiagnosisPrototype) {
		this.tmpDiagnosisPrototype = tmpDiagnosisPrototype;
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

	public List<Contact> getAllAvailableContact() {
		return allAvailableContact;
	}

	public void setAllAvailableContact(List<Contact> allAvailableContact) {
		this.allAvailableContact = allAvailableContact;
	}

	public boolean isPersonSurgeon() {
		return personSurgeon;
	}

	public void setPersonSurgeon(boolean personSurgeon) {
		this.personSurgeon = personSurgeon;
	}

	public boolean isPersonExtern() {
		return personExtern;
	}

	public void setPersonExtern(boolean personExtern) {
		this.personExtern = personExtern;
	}

	public boolean isPersonOther() {
		return personOther;
	}

	public void setPersonOther(boolean personOther) {
		this.personOther = personOther;
	}

	public boolean isAddedContacts() {
		return addedContacts;
	}

	public void setAddedContacts(boolean addedContacts) {
		this.addedContacts = addedContacts;
	}

	public int getWorklistSortOrder() {
		return worklistSortOrder;
	}

	public void setWorklistSortOrder(int worklistSortOrder) {
		this.worklistSortOrder = worklistSortOrder;
	}

	public int getWorklistDisplay() {
		return worklistDisplay;
	}

	public void setWorklistDisplay(int worklistDisplay) {
		this.worklistDisplay = worklistDisplay;
	}

	public String getSearchForPatientPiz() {
		return searchForPatientPiz;
	}

	public void setSearchForPatientPiz(String searchForPatientPiz) {
		this.searchForPatientPiz = searchForPatientPiz;
	}

	public String getSearchForPatientName() {
		return searchForPatientName;
	}

	public void setSearchForPatientName(String searchForPatientName) {
		this.searchForPatientName = searchForPatientName;
	}

	public String getSearchForPatientSurname() {
		return searchForPatientSurname;
	}

	public void setSearchForPatientSurname(String searchForPatientSurname) {
		this.searchForPatientSurname = searchForPatientSurname;
	}

	public Date getSearchForPatientBirthday() {
		return searchForPatientBirthday;
	}

	public void setSearchForPatientBirthday(Date searchForPatientBirthday) {
		this.searchForPatientBirthday = searchForPatientBirthday;
	}

	public List<Patient> getSearchForPatientList() {
		return searchForPatientList;
	}

	public void setSearchForPatientList(List<Patient> searchForPatientList) {
		this.searchForPatientList = searchForPatientList;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
