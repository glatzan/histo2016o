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
import org.histo.ui.PatientList;
import org.histo.ui.StainingListTransformer;
import org.histo.util.Log;
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

	public final static int DISPLAY_PATIENT = 0;
	public final static int DISPLAY_RECEIPTLOG = 1;
	public final static int DISPLAY_DIAGNOSIS_INTERN = 2;
	public final static int DISPLAY_DIAGNOSIS_INTERN_EXTENDED = 3;
	public final static int DISPLAY_DIAGNOSIS_EXTERNAL = 4;
	public final static int DISPLAY_DIAGNOSIS_EXTERNAL_EXTENDED = 5;

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

	@Autowired
	private Log log;
	
	@Autowired
	private org.histo.util.ResourceBundle resourceBundle;

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

	/**
	 * List of all found Patients of the patientSearchRequest, PatientList is
	 * used instead of Patient because primefaces needs a unique row collum.
	 */
	private List<PatientList> searchForPatientList;

	/**
	 * Selected Patient, is used to add a patient to the worklist
	 */
	private PatientList selectedPatientFromSearchList;

	/**
	 * Patient to search for, piz
	 */
	private String searchForPatientPiz;

	/**
	 * Patient to search for, name
	 */
	private String searchForPatientName;

	/**
	 * Patient to search for, surname
	 */
	private String searchForPatientSurname;

	/**
	 * Patient to search for, birthday
	 */
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
		log.debug("Login erfolgreich");
		goToWorkList();
	}

	public String goToWorkList() {
		if (getWorkList() == null) {
			log.debug("Standard Arbeitsliste geladen");
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
			else if (getSelectedPatient() != null) {
				if (getSelectedPatient().getTasks() != null && getSelectedPatient().getTasks().size() > 0) {
					getSelectedPatient().setSelectedTask(TaskUtil.getLastTask(getSelectedPatient().getTasks()));
					return HistoSettings.CENTER_INCLUDE_RECEIPTLOG;
				} else
					return HistoSettings.CENTER_INCLUDE_PATIENT;
			} else
				return HistoSettings.CENTER_INCLUDE_BLANK;
		case DISPLAY_DIAGNOSIS_INTERN:
			if (getSelectedPatient() != null && getSelectedPatient().getSelectedTask() != null)
				return HistoSettings.CENTER_INCLUDE_DIAGNOSIS_INTERN;
			else if (getSelectedPatient() != null)
				return HistoSettings.CENTER_INCLUDE_PATIENT;
			else
				return HistoSettings.CENTER_INCLUDE_BLANK;

		case DISPLAY_DIAGNOSIS_INTERN_EXTENDED:
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

	/******************************************************** Patient ********************************************************/

	/**
	 * Shows the "/pages/dialog/patient/addPatient" dialog and creates an new
	 * empty patient.
	 */
	public void prepareAddPatient() {
		setTmpPatient(new Patient());
		getTmpPatient().setPerson(new Person());

		// updating search list
		if (getSearchForPatientPiz() != null || getSearchForPatientName() != null
				|| getSearchForPatientSurname() != null || getSearchForPatientBirthday() != null)
			searchPatient(getSearchForPatientPiz(), getSearchForPatientName(), getSearchForPatientSurname(),
					getSearchForPatientBirthday());

		setSelectedPatientFromSearchList(null);

		helper.showDialog(HistoSettings.dialog(HistoSettings.DIALOG_PATIENT_ADD), 1024, 500, true, false, false);
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

		genericDAO.save(patient);
		getWorkList().add(patient);
		setSelectedPatient(patient);

		log.info("Neuer externer Patient erstellt, " + patient.asGson(), patient);

		hidePatientDialog();
	}

	/**
	 * Adds an Patient found in the clinic-backend or in the histo-backend to
	 * the worklist.
	 */
	// TODO not add the same patient twice
	public void addNewPatient(Patient patient) {
		if (patient != null) {

			PersonAdministration admim = new PersonAdministration();

			// if patient is new and was not added to the histo database before
			if (patient.getAddDate() == null)
				patient.setAddDate(new Date(System.currentTimeMillis()));

			// add patient from the clinic-backend, get all data of this patient
			if (!patient.getPiz().isEmpty()) {
				String userResult = admim.getRequest(HistoSettings.PATIENT_GET_URL + "/" + patient.getPiz());
				admim.updatePatientFromClinicJson(patient, userResult);
			}

			genericDAO.save(patient);

			// add patient to worklist
			getWorkList().add(patient);

			hidePatientDialog();
		}
	}

	/**
	 * Hides the "/pages/dialog/patient/addPatient" dialog
	 */
	public void hidePatientDialog() {
		setTmpPatient(null);
		helper.hideDialog("/pages/dialog/patient/addPatient");
	}

	/**
	 * Searches for a patient with the given paramenters in the clinic and in
	 * the histo backend.
	 * http://auginfo/piz?name=xx&vorname=xx&geburtsdatum=2000-01-01
	 * 
	 * @param piz
	 */
	public void searchPatient(String piz, String name, String surname, Date birthday) {

		PersonAdministration admim = new PersonAdministration();

		// id for patientList, used by primefaces to get the selected row
		int id = 0;

		// if piz is given ignore other parameters
		if (piz != null && piz.matches("^[0-9]{6,8}$")) {
			ArrayList<PatientList> result = new ArrayList<PatientList>();
			List<Patient> patients = patientDao.searchForPatientsPiz(piz);

			// updates all patients from the local database with data from the
			// clinic backend
			for (Patient patient : patients) {
				String userResult = admim.getRequest(HistoSettings.PATIENT_GET_URL + "/" + patient.getPiz());
				admim.updatePatientFromClinicJson(patient, userResult);
				result.add(new PatientList(id++, patient));
			}

			// saves the results
			genericDAO.save(patients);

			// only get patient from clinic backend if piz is completely
			// provided and was not added to the local database before
			if (piz.matches("^[0-9]{8}$") && patients.isEmpty()) {
				String userResult = admim.getRequest(HistoSettings.PATIENT_GET_URL + "/" + piz);
				result.add(new PatientList(id++, admim.getPatientFromClinicJson(userResult)));
			}

			setSearchForPatientList(result);
		} else if ((name != null && !name.isEmpty()) || (surname != null && !surname.isEmpty()) || birthday != null) {
			List<PatientList> result = new ArrayList<>();

			// getting all patients with given parameters from the clinic
			// backend
			String requestURl = HistoSettings.PATIENT_GET_URL + "?name=" + name + "&vorname=" + surname
					+ (birthday != null ? "&geburtsdatum=" + TimeUtil.formatDate(birthday, "yyyy-MM-dd") : "");

			String userResult = admim.getRequest(requestURl);
			List<Patient> clinicPatients = admim.getPatientsFromClinicJson(userResult);

			List<Patient> histoPatients = patientDao.getPatientsByParameter(name, surname, birthday);

			for (Patient cPatient : clinicPatients) {
				PatientList patientList = null;

				boolean found = false;

				for (Patient hPatient : histoPatients) {
					if (cPatient.getPiz().equals(hPatient.getPatient().getPiz())) {
						patientList = new PatientList(id++, hPatient);
						// removing histo patient from list
						histoPatients.remove(hPatient);
						// TODO update the patient in histo database
						found = true;
						break;
					}
				}

				if (!found) {
					// checking explicitly for the piz
					Patient histoDatabase = patientDao.searchForPatientPiz(cPatient.getPiz());

					if (histoDatabase != null) {
						patientList = new PatientList(id++, histoDatabase);
					} else {
						patientList = new PatientList(id++, cPatient);
						patientList.setNotHistoDatabase(true);
					}

				}
				// checking if a patient

				result.add(patientList);
			}

			// adding external patient to the list, these patients are not in
			// the clinic backend
			for (Patient hPatient : histoPatients) {
				result.add(new PatientList(id++, hPatient));
			}

			setSearchForPatientList(result);
		}
	}

	/**
	 * Shows a dialog for editing patients which are only stored in the histo
	 * database (external patients)
	 * 
	 * @param patient
	 */
	public void editExternalPatient(Patient patient) {
		setTmpPatient(patient);
		helper.showDialog(HistoSettings.dialog(HistoSettings.DIALOG_PATIENT_EDIT), 1024, 500, true, false, false);
	}

	/******************************************************** Patient ********************************************************/

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
		// set patient.selectedTask is performed by the gui
		// sets this task as active, so it will be show in the navigation column
		// whether there is an action to perform or not
		task.setCurrentlyActive(true);

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
		setWorklistDisplay(DISPLAY_PATIENT);
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

		log.info("Neuer Auftrag erstell: TaskID:" + task.getTaskID(), patient);

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

		log.info("Neue Probe erstellt: TaskID: " + task.getTaskID() + ", SampleID: " + newSample.getSampleID(),
				getSelectedPatient());

		
		genericDAO.save(newSample);
		
		// creating first default diagnosis
		createNewDiagnosis(newSample, Diagnosis.TYPE_DIAGNOSIS);
			
		// creating needed blocks
		createNewBlock(newSample, stainingPrototypeList);

	}

	/******************************************************** Sample ********************************************************/

	/******************************************************** Block ********************************************************/
	public void createNewBlock(Sample sample, StainingPrototypeList stainingPrototypeList) {
		Block block = TaskUtil.createNewBlock(sample);

		log.info("Neuen Block erstellt: SampleID: " + sample.getSampleID() + ", BlockID: " + block.getBlockID(),
				getSelectedPatient());

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

	public void createNewDiagnosisTypSelectAuto(Sample sample) {
		List<Diagnosis> diagnoses = sample.getDiagnoses();
		
		// create new diagnosis
		if (diagnoses.isEmpty()) {
			createNewDiagnosis(sample, Diagnosis.TYPE_DIAGNOSIS);
		// create revision 
		} else if (diagnoses.size() == 1) {
			createNewDiagnosis(sample, Diagnosis.TYPE_DIAGNOSIS_REVISION);
		// if diagnosis and revision 
		}else{
			
			for (Diagnosis diagnosis : diagnoses) {
				if(!diagnosis.isFinalized())
					return;
			}
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

		log.info(
				"Neue Diagnose erstellt: SampleID: " + sample.getSampleID() + ", DiagnosisID/Name: "
						+ newDiagnosis.getId() + " - " + TaskUtil.getDiagnosisName(sample, newDiagnosis),
				sample.getPatient());
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
			if (contact.getRole() == Contact.ROLE_EXTERN && contact.getPhysician().getFax() != null
					&& !contact.getPhysician().getFax().isEmpty()) {
				contact.setUseFax(true);
				return;
			}
			// in allen anderen fällen email setzten
			if (contact.getPhysician().getEmail() != null && !contact.getPhysician().getEmail().isEmpty())
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
		helper.showDialog(HistoSettings.dialog(HistoSettings.DIALOG_WORKLIST_OPTIONS), 650, 470, true, false, false);
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

			log.info("Staining list");
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
			log.info("Diagnosis list");
			break;
		case SearchOptions.SEARCH_INDEX_TODAY:
			getWorkList().clear();
			getWorkList().addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTime(),
					TimeUtil.setDayEnding(cal).getTime(), searchOptions.getFilterIndex()));
			log.info("Worklist today - " + TimeUtil.setDayBeginning(cal).getTime() + " "
					+ TimeUtil.setDayEnding(cal).getTime());
			break;
		case SearchOptions.SEARCH_INDEX_YESTERDAY:
			getWorkList().clear();
			cal.add(Calendar.DAY_OF_MONTH, -1);
			getWorkList().addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTime(),
					TimeUtil.setDayEnding(cal).getTime(), searchOptions.getFilterIndex()));
			log.info("Worklist yesterday - " + TimeUtil.setDayBeginning(cal).getTime() + " "
					+ TimeUtil.setDayEnding(cal).getTime());
			break;
		case SearchOptions.SEARCH_INDEX_CURRENTWEEK:
			getWorkList().clear();
			getWorkList().addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setWeekBeginning(cal).getTime(),
					TimeUtil.setWeekEnding(cal).getTime(), searchOptions.getFilterIndex()));
			log.info("Worklist current week - " + TimeUtil.setWeekBeginning(cal).getTime() + " "
					+ TimeUtil.setWeekEnding(cal).getTime());
			break;
		case SearchOptions.SEARCH_INDEX_LASTWEEK:
			getWorkList().clear();
			cal.add(Calendar.WEEK_OF_YEAR, -1);
			getWorkList().addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setWeekBeginning(cal).getTime(),
					TimeUtil.setWeekEnding(cal).getTime(), searchOptions.getFilterIndex()));
			log.info("Worklist last week - " + TimeUtil.setWeekBeginning(cal).getTime() + " "
					+ TimeUtil.setWeekEnding(cal).getTime());
			break;
		case SearchOptions.SEARCH_INDEX_LASTMONTH:
			getWorkList().clear();
			cal.add(Calendar.MONDAY, -1);
			getWorkList().addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setMonthBeginning(cal).getTime(),
					TimeUtil.setMonthEnding(cal).getTime(), searchOptions.getFilterIndex()));
			log.info("Worklist last month - " + TimeUtil.setMonthBeginning(cal).getTime() + " "
					+ TimeUtil.setMonthEnding(cal).getTime());
			break;
		case SearchOptions.SEARCH_INDEX_DAY:
			cal.setTime(searchOptions.getDay());
			getWorkList().clear();
			getWorkList().addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTime(),
					TimeUtil.setDayEnding(cal).getTime(), searchOptions.getFilterIndex()));
			log.info("Day - " + TimeUtil.setDayBeginning(cal).getTime() + " " + TimeUtil.setDayEnding(cal).getTime());
			break;
		case SearchOptions.SEARCH_INDEX_MONTH:
			cal.set(Calendar.MONTH, searchOptions.getSearchMonth());
			cal.set(Calendar.YEAR, searchOptions.getYear());
			getWorkList().addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setMonthBeginning(cal).getTime(),
					TimeUtil.setMonthEnding(cal).getTime(), searchOptions.getFilterIndex()));
			log.info("Worklist month - " + TimeUtil.setMonthBeginning(cal).getTime() + " "
					+ TimeUtil.setMonthEnding(cal).getTime());
			break;
		case SearchOptions.SEARCH_INDEX_TIME:
			cal.setTime(searchOptions.getSearchFrom());
			Date fromTime = TimeUtil.setDayBeginning(cal).getTime();
			cal.setTime(searchOptions.getSearchTo());
			Date toTime = TimeUtil.setDayEnding(cal).getTime();
			getWorkList()
					.addAll(patientDao.getWorklistDynamicallyByType(fromTime, toTime, searchOptions.getFilterIndex()));
			log.info("Worklist time - " + fromTime + " " + toTime);
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

	public List<PatientList> getSearchForPatientList() {
		return searchForPatientList;
	}

	public void setSearchForPatientList(List<PatientList> searchForPatientList) {
		this.searchForPatientList = searchForPatientList;
	}

	public PatientList getSelectedPatientFromSearchList() {
		return selectedPatientFromSearchList;
	}

	public void setSelectedPatientFromSearchList(PatientList selectedPatientFromSearchList) {
		this.selectedPatientFromSearchList = selectedPatientFromSearchList;
	}

	public boolean isWorklistSortOrderAcs() {
		return worklistSortOrderAcs;
	}

	public void setWorklistSortOrderAcs(boolean worklistSortOrderAcs) {
		this.worklistSortOrderAcs = worklistSortOrderAcs;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
