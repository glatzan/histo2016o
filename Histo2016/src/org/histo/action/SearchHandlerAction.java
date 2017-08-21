package org.histo.action;

import java.util.Date;

import javax.faces.application.FacesMessage;

import org.apache.log4j.Logger;
import org.histo.action.dialog.patient.AddPatientDialogHandler;
import org.histo.action.dialog.patient.CreateTaskDialog;
import org.histo.action.handler.SearchHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.util.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class SearchHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private SearchHandler searchHandler;

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	private DialogHandlerAction dialogHandlerAction;

	private String searchString;

	private boolean searchWorklist;

	public void quickSearch() {
		quickSearch(getSearchString(), userHandlerAction.getCurrentUser().isAlternatePatientAddMode());
	}

	/**
	 * 6 \d = task id
	 * 
	 * @param searchString
	 * @param alt
	 * @param strg
	 */
	public void quickSearch(String searchString, boolean alternateMode) {
		logger.debug("Search for " + searchString + ", AlternateMode: " + alternateMode);
		try {
			// search only in selected worklist
			if (isSearchWorklist()) {
				logger.debug("Search in worklist");
				// TODO: implement
			} else {

				if (searchString.matches("^\\d{6}$")) { // task
					// serach for task (6 digits)
					Patient patientOfTask = patientDao.getPatientByTaskID(searchString);

					if (patientOfTask != null) {
						logger.debug("Task found, adding to worklist");
						worklistViewHandlerAction.addPatientToWorkList(patientOfTask.getPatient(), true);

						Task task = patientOfTask.getTasks().stream().filter(p -> p.getTaskID().equals(searchString))
								.collect(StreamUtils.singletonCollector());

						worklistViewHandlerAction.onSelectTaskAndPatient(task);

						mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.search.patient.task"),
								resourceBundle.get("growl.search.patient.task.text"));

					} else {
						// no task was found
						logger.debug("No task with the given id found");
						mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.search.patient.notFount.task"),
								"", FacesMessage.SEVERITY_ERROR);
					}

				} else if (searchString.matches("^\\d{8}$")) { // piz
					// searching for piz (8 digits)
					logger.debug("Search for piz: " + searchString);

					Patient patient = searchHandler.serachForPiz(searchString);

					if (patient != null) {
						logger.debug("Found patient " + patient + " and adding to currentworklist");
						searchHandler.addClinicPatient(patient);
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

						logger.debug("No Patient found with piz " + searchString);
					}
				} else if (searchString.matches("^\\d{9}$")) { // slide id
					// searching for slide (8 digits)
					logger.debug("Search for SlideID: " + searchString);

					// TODO find correct id

					Patient searchResultSlide = patientDao.getPatientBySlidID(searchString);

					if (searchResultSlide != null) {
						logger.debug("Slide found");
						mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.search.patient.slide"),
								resourceBundle.get("growl.search.patient.slide"));
						worklistViewHandlerAction.addPatientToWorkList(searchResultSlide.getPatient(), true);
					} else {
						// no slide was found
						logger.debug("No slide with the given id found");
						mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.search.patient.notFount.slide"),
								"", FacesMessage.SEVERITY_ERROR);
					}
				} else if (searchString.matches("^(.+)[, ](.+)$")) {
					// name, surename; name surename
					String[] arr = searchString.split("[, ]");

					dialogHandlerAction.getAddPatientDialogHandler().initAndPrepareBeanFromExternal(arr[0], arr[1], "", new Date());

				} else if (searchString.matches("^[\\p{Alpha}\\-]+")) {
					dialogHandlerAction.getAddPatientDialogHandler().initAndPrepareBeanFromExternal(searchString, "", "", new Date());
				} else {

				}
			}

			setSearchString("");
		} catch (Exception e) {
			// TODO inform the user
		}
	}

	// ************************ Getter/Setter ************************
	public String getSearchString() {
		return searchString;
	}

	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}

	public boolean isSearchWorklist() {
		return searchWorklist;
	}

	public void setSearchWorklist(boolean searchWorklist) {
		this.searchWorklist = searchWorklist;
	}

}
