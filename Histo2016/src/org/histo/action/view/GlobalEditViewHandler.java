package org.histo.action.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.histo.action.DialogHandlerAction;
import org.histo.action.MainHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.View;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.DiagnosisPreset;
import org.histo.model.ListItem;
import org.histo.model.Physician;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.ui.menu.MenuGenerator;
import org.histo.ui.transformer.DefaultTransformer;
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
	@Lazy
	private WorklistViewHandlerAction worklistViewHandlerAction;

	// ************************ Navigation ************************
	/**
	 * View options, dynamically generated depending on the users role
	 */
	private List<View> navigationPages;

	/**
	 * Selected View in the menu
	 */
	private View selectedView;
	
	/**
	 * Current view which is displayed
	 */
	private View currentView;

	/**
	 * Can be Diagnosis or Receiptlog
	 */
	private View lastDefaultView;

	// ************************ Patient ************************

	// ************************ Search ************************
	/**
	 * Search String for quick search
	 */
	private String quickSearch;

	private boolean searchWorklist;
	// ************************ Search ************************

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

	/**
	 * Method called on postconstruct. Initializes all important variables.
	 */
	@PostConstruct
	public void init() {
		logger.debug("PostConstruct Init program");

		setNavigationPages(new ArrayList<View>());

		// settings views
		setNavigationPages(new ArrayList<View>(userHandlerAction.getCurrentUser().getSettings().getAvailableViews()));

		updateDataOfTask(false);
	}

	public void loadGuiData() {
//		setPhysiciansToSignList(physicianDAO.getPhysicians(ContactRole.SIGNATURE, false));
//		setPhysiciansToSignListTransformer(new DefaultTransformer<Physician>(getPhysiciansToSignList()));
//		
//		setCaseHistoryList(utilDAO.getAllStaticListItems(ListItem.StaticList.CASE_HISTORY));
//		
//		setWardList(utilDAO.getAllStaticListItems(ListItem.StaticList.WARDS));
//		
//		setDiagnosisPresets(utilDAO.getAllDiagnosisPrototypes());
//		setDiagnosisPresetsTransformer(new DefaultTransformer<DiagnosisPreset>(getDiagnosisPresets()));
	}
	
	public String getCenterView() {
		if (getCurrentView() != null)
			return getCurrentView().getPath();
		else
			return View.WORKLIST_BLANK.getPath();
	}
	
	public void updateDataOfTask(boolean updateFavouriteLists) {
		if (selectedTask != null)
			selectedTask.generateTaskStatus();

		updateMenuModel(updateFavouriteLists);
	}

	public void updateMenuModel(boolean updateFavouriteLists) {
		setTaskMenuModel((new MenuGenerator()).generateEditMenu(selectedPatient, selectedTask));
	}

	public void addTaskToFavouriteList(Task task, long id) {
		try {
			favouriteListDAO.addTaskToList(task, id);
			updateDataOfTask(true);
		} catch (CustomDatabaseInconsistentVersionException e) {
			worklistViewHandlerAction.onVersionConflictPatient(task.getPatient(), true);
		}
	}

	public void removeTaskFromFavouriteList(Task task, Long... ids) {
		try {
			favouriteListDAO.removeTaskFromList(task, ArrayUtils.toPrimitive(ids));
			updateDataOfTask(true);
		} catch (CustomDatabaseInconsistentVersionException e) {
			worklistViewHandlerAction.onVersionConflictPatient(task.getPatient(), true);
		}
	}

	public void quickSearch() {
		quickSearch(getQuickSearch(), userHandlerAction.getCurrentUser().getSettings().isAlternatePatientAddMode());
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

						mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.search.patient.task"),
								resourceBundle.get("growl.search.patient.task.text"));

					} else {
						// no task was found
						logger.debug("No task with the given id found");
						mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.search.patient.notFount.task"),
								"", FacesMessage.SEVERITY_ERROR);
					}

				} else if (quickSerach.matches("^\\d{8}$")) { // piz
					// searching for piz (8 digits)
					logger.debug("Search for piz: " + quickSerach);

					Patient patient = patientDao.searchForPatientByPiz(quickSerach);

					if (patient != null) {
						if (globalSettings.getClinicJsonHandler().updatePatientFromClinicJson(patient))
							genericDAO.savePatientData(patient, "log.patient.search.update");

						logger.debug("Found patient " + patient + " and adding to currentworklist");

						worklistViewHandlerAction.addPatientToWorkList(patient, true);

						mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.search.patient.piz"),
								resourceBundle.get("growl.search.patient.piz.text"));

						// if alternate mode the create Task dialog will be
						// shown
						// after the patient is added to the worklist
						if (alternateMode) {
							dialogHandlerAction.getCreateTaskDialog().initAndPrepareBean(patient);
						}

					} else {
						// no patient was found for piz
						mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.search.patient.notFound.piz"), "",
								FacesMessage.SEVERITY_ERROR);

						logger.debug("No Patient found with piz " + quickSerach);
					}
				} else if (quickSerach.matches("^\\d{9}$")) { // slide id
					// searching for slide (9 digits)
					logger.debug("Search for SlideID: " + quickSerach);

					String taskId = quickSerach.substring(0, 6);
					String uniqueSlideIDinTask = quickSerach.substring(6, 9);

					System.out.println(taskId + " " + uniqueSlideIDinTask);

					Task task = taskDAO.getTaskBySlideID(taskId, Integer.parseInt(uniqueSlideIDinTask));

					if (task != null) {
						logger.debug("Slide found");
						mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.search.patient.slide"),
								resourceBundle.get("growl.search.patient.slide"));
						worklistViewHandlerAction.onSelectTaskAndPatient(task.getId());
					} else {
						// no slide was found
						logger.debug("No slide with the given id found");
						mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.search.patient.notFount.slide"),
								"", FacesMessage.SEVERITY_ERROR);
					}

				} else if (quickSerach.matches("^(.+)[, ](.+)$")) {
					// name, surename; name surename
					String[] arr = quickSerach.split("[, ]");

					dialogHandlerAction.getAddPatientDialogHandler().initAndPrepareBeanFromExternal(arr[0], arr[1], "",
							null);

				} else if (quickSerach.matches("^[\\p{Alpha}\\-]+")) {
					dialogHandlerAction.getAddPatientDialogHandler().initAndPrepareBeanFromExternal(quickSerach, "", "",
							null);
				} else {

				}
			}

			setQuickSearch("");
		} catch (

		Exception e) {
			// TODO inform the user
		}
	}

}
