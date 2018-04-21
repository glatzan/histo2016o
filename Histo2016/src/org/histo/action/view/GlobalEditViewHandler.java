package org.histo.action.view;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.histo.action.DialogHandlerAction;
import org.histo.action.MainHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.patient.AddPatientDialogHandler;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.View;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.DiagnosisPreset;
import org.histo.model.ListItem;
import org.histo.model.MaterialPreset;
import org.histo.model.PDFContainer;
import org.histo.model.Physician;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.model.user.HistoPermissions;
import org.histo.service.PatientService;
import org.histo.ui.menu.MenuGenerator;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.notification.NotificationContainer;
import org.primefaces.event.SelectEvent;
import org.primefaces.json.JSONException;
import org.primefaces.model.menu.MenuModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Getter
@Setter
public class GlobalEditViewHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private MainHandlerAction mainHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UtilDAO utilDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PhysicianDAO physicianDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Lazy
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Lazy
	private ReceiptlogViewHandlerAction receiptlogViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientService patientService;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private AddPatientDialogHandler addPatientDialogHandler;
	// ************************ Navigation ************************
	/**
	 * View options, dynamically generated depending on the users role
	 */
	private List<View> navigationPages;

	/**
	 * Selected View in the menu
	 */
	private View displayView;

	/**
	 * Current view which is displayed
	 */
	private View currentView;

	/**
	 * Can be Diagnosis or Receiptlog
	 */
	private View lastDefaultView;

	// ************************ Search ************************
	/**
	 * Search String for quick search
	 */
	private String quickSearch;

	/**
	 * TODO: use
	 */
	private boolean searchWorklist;

	// ************************ dynamic lists ************************
	/**
	 * Contains all available case histories
	 */
	private List<ListItem> slideCommentary;

	/**
	 * List of all diagnosis presets
	 */
	private List<DiagnosisPreset> diagnosisPresets;

	/**
	 * List of physicians which have the role signature
	 */
	private List<Physician> physiciansToSignList;

	/**
	 * Transfomer for physiciansToSign
	 */
	private DefaultTransformer<Physician> physiciansToSignListTransformer;

	/**
	 * List of available materials
	 */
	private List<MaterialPreset> materialList;

	/**
	 * Contains all available case histories
	 */
	private List<ListItem> caseHistoryList;

	/**
	 * Contains all available wards
	 */
	private List<ListItem> wardList;

	// ************************ Current Patient/Task ************************

	/**
	 * Currently selectedTask
	 */
	private Patient selectedPatient;

	/**
	 * Currently selectedTask
	 */
	private Task selectedTask;

	/**
	 * MenuModel for task editing
	 */
	private MenuModel taskMenuModel;

	// ************************ Current Patient/Task ************************
	/**
	 * DataTable selection to change a material via overlay panel
	 */
	private MaterialPreset materialPresetToChange;

	/**
	 * Method called on postconstruct. Initializes all important variables.
	 */
	@PostConstruct
	public void init() {
		logger.debug("PostConstruct Init program");

		setNavigationPages(new ArrayList<View>());

		// settings views
		setNavigationPages(new ArrayList<View>(userHandlerAction.getCurrentUser().getSettings().getAvailableViews()));

		updateDataOfTask(true, true, true, true);

		loadGuiData();
	}

	/**
	 * Loads all dynmaic gui lists for displaing in the different views
	 */
	public void loadGuiData() {

		setSlideCommentary(utilDAO.getAllStaticListItems(ListItem.StaticList.SLIDES));
		setCaseHistoryList(utilDAO.getAllStaticListItems(ListItem.StaticList.CASE_HISTORY));
		setWardList(utilDAO.getAllStaticListItems(ListItem.StaticList.WARDS));

		setDiagnosisPresets(utilDAO.getAllDiagnosisPrototypes());

		setPhysiciansToSignList(physicianDAO.getPhysicians(ContactRole.SIGNATURE, false));
		setPhysiciansToSignListTransformer(new DefaultTransformer<Physician>(getPhysiciansToSignList()));

		setMaterialList(utilDAO.getAllMaterialPresets(true));
	}

	public void reloadGuiData() {
		loadGuiData();

		// only task needs reload
		if (selectedTask != null) {
			worklistViewHandlerAction.onSelectTaskAndPatient(selectedTask.getId());
			logger.debug("Reloading task");
		}
	}

	public String getCenterView() {
		if (getDisplayView() != null)
			return getDisplayView().getPath();
		else
			return View.WORKLIST_BLANK.getPath();
	}

	public void updateDataOfTask() {
		updateDataOfTask(true, false, true, true);
	}

	/**
	 * Updates essencial task data, menuModel= Menu for Task, TaskStatus = Task
	 * infos (patientmenu), SlideguiList = receiptlog
	 * 
	 * @param updateMenuModel
	 * @param updateFavouriteLists
	 * @param updateTaskStatus
	 * @param updateSlideGuiList
	 */
	public void updateDataOfTask(boolean updateMenuModel, boolean updateFavouriteLists, boolean updateTaskStatus,
			boolean updateSlideGuiList) {

		logger.debug("Updating Task Data + (menuModel = " + updateMenuModel + " ,TaskStatus = " + updateTaskStatus
				+ " , SlideGuiList = " + updateSlideGuiList + ")");

		if (selectedTask != null && updateTaskStatus)
			selectedTask.generateTaskStatus();

		if (updateMenuModel)
			updateMenuModel(updateFavouriteLists);

		if (selectedTask != null && updateSlideGuiList)
			receiptlogViewHandlerAction.updateSlideGuiList(getSelectedTask(), false);
	}

	public void updateMenuModel(boolean updateFavouriteLists) {
		setTaskMenuModel((new MenuGenerator()).generateEditMenu(selectedPatient, selectedTask));
	}

	public void addTaskToFavouriteList(Task task, long id) {
		try {
			favouriteListDAO.addTaskToList(task, id);
			updateDataOfTask(true, true, true, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			worklistViewHandlerAction.replacePatientInCurrentWorklist(task.getPatient(), true);
		}
	}

	public void removeTaskFromFavouriteList(Task task, Long... ids) {
		try {
			favouriteListDAO.removeTaskFromList(task, ArrayUtils.toPrimitive(ids));
			updateDataOfTask(true, true, true, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			worklistViewHandlerAction.replacePatientInCurrentWorklist(task.getPatient(), true);
		}
	}

	public void quickSearch() {
		quickSearch(getQuickSearch(), userHandlerAction.getCurrentUser().getSettings().isAlternatePatientAddMode());
		setQuickSearch("");
	}

	public void quickSearch(String quickSerach, boolean alternateMode) {
		logger.debug("Search for " + quickSerach + ", AlternateMode: " + alternateMode);
		try {
			// search only in selected worklist
			if (isSearchWorklist()) {
				logger.debug("Search in worklist");
				// TODO: implement
			} else {

				if (quickSerach.matches("^\\d{6}$")) { // task
					// serach for task (6 digits)

					Task task = taskDAO.getTaskByTaskID(quickSerach);

					if (task != null) {
						logger.debug("Task found, adding to worklist");

						worklistViewHandlerAction.onSelectTaskAndPatient(task.getId());

						mainHandlerAction.sendGrowlMessagesAsResource("growl.search.patient.task",
								"growl.search.patient.task.text");

					} else {
						// no task was found
						logger.debug("No task with the given id found");
						mainHandlerAction.sendGrowlMessagesAsResource("growl.search.patient.notFound.task",
								"general.blank", FacesMessage.SEVERITY_ERROR);
					}

				} else if (quickSerach.matches("^\\d{8}$")) { // piz
					// searching for piz (8 digits)
					logger.debug("Search for piz: " + quickSerach);

					// Searching for patient in pdv and local database
					Patient patient = patientService.serachForPiz(quickSerach,
							!userHandlerAction.currentUserHasPermission(HistoPermissions.PATIENT_EDIT_ADD_CLINIC));

					if (patient != null) {

						logger.debug("Found patient " + patient + " and adding to currentworklist");

						worklistViewHandlerAction.addPatientToWorkList(patient, true, true);

						mainHandlerAction.sendGrowlMessagesAsResource("growl.search.patient.piz",
								"growl.search.patient.piz.text");

						// if alternate mode the create Task dialog will be
						// shown
						// after the patient is added to the worklist
						if (alternateMode) {
							dialogHandlerAction.getCreateTaskDialog().initAndPrepareBean(patient);
						}

					} else {
						// no patient was found for piz
						mainHandlerAction.sendGrowlMessagesAsResource("growl.search.patient.notFound.piz",
								"general.blank", FacesMessage.SEVERITY_ERROR);

						logger.debug("No Patient found with piz " + quickSerach);
					}
				} else if (quickSerach.matches("^\\d{9}$")) { // slide id
					// searching for slide (9 digits)
					logger.debug("Search for SlideID: " + quickSerach);

					String taskId = quickSerach.substring(0, 6);
					String uniqueSlideIDinTask = quickSerach.substring(6, 9);

					Task task = taskDAO.getTaskBySlideID(taskId, Integer.parseInt(uniqueSlideIDinTask));

					if (task != null) {
						logger.debug("Slide found");
						mainHandlerAction.sendGrowlMessagesAsResource("growl.search.patient.slide",
								"growl.search.patient.slide");
						worklistViewHandlerAction.onSelectTaskAndPatient(task.getId());
					} else {
						// no slide was found
						logger.debug("No slide with the given id found");
						mainHandlerAction.sendGrowlMessagesAsResource("growl.search.patient.notFount.slide",
								"general.blank", FacesMessage.SEVERITY_ERROR);
					}

				} else if (quickSerach.matches("^(.+)(, )(.+)$")) {
					logger.debug("Search for name, first name");
					// name, surename; name surename
					String[] arr = quickSerach.split(", ");

					addPatientDialogHandler.initAndPrepareBeanFromExternal(arr[0], arr[1], "", null);

				} else if (quickSerach.matches("^(.+) (.+)$")) {
					logger.debug("Search for firstname, name");
					// name, surename; name surename
					String[] arr = quickSerach.split(" ");

					addPatientDialogHandler.initAndPrepareBeanFromExternal(arr[1], arr[0], "", null);

				} else if (quickSerach.matches("^[\\p{Alpha}\\-]+")) {
					logger.debug("Search for name");
					addPatientDialogHandler.initAndPrepareBeanFromExternal(quickSerach, "", "", null);
				} else {
					logger.debug("No search match found");
					mainHandlerAction.sendGrowlMessagesAsResource("growl.search.patient.notFount.general",
							"general.blank", FacesMessage.SEVERITY_ERROR);
				}
			}

		} catch (Exception e) {
			// TODO inform the user
		}
	}

	/**
	 * Adds an external or clinic patient to the database
	 */
	public void onAddClinicPatient(SelectEvent event) {
		if (event.getObject() != null && event.getObject() instanceof Patient) {
			try {
				logger.debug("Adding patient to database");
				patientService.addPatient((Patient) event.getObject(), true);
				worklistViewHandlerAction.addPatientToWorkList((Patient) event.getObject(), true, true);
			} catch (JSONException | CustomExceptionToManyEntries | CustomNullPatientExcepetion e) {
				worklistViewHandlerAction.replacePatientInCurrentWorklist((Patient) event.getObject());
			}

		}
	}

}
