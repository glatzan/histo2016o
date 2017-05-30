package org.histo.action;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;

import org.apache.log4j.Logger;
import org.histo.action.dialog.task.CreateTaskDialog;
import org.histo.action.handler.SearchHandler;
import org.histo.action.handler.SettingsHandler;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.QuickSearchOptions;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.ui.PatientList;
import org.histo.util.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class SearchHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private PatientHandlerAction patientHandlerAction;

	@Autowired
	private WorklistHandlerAction worklistHandlerAction;

	@Autowired
	private CreateTaskDialog createTaskDialog;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private SearchHandler searchHandler;

	private String searchString;

	private boolean searchWorklist;

	public void quickSearch(boolean alternateMode) {
		quickSearch(getSearchString(), alternateMode);
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
					worklistHandlerAction.addPatientToWorkList(patientOfTask.getPatient(), true);

					Task task = patientOfTask.getTasks().stream().filter(p -> p.getTaskID().equals(searchString))
							.collect(StreamUtils.singletonCollector());

					worklistHandlerAction.onSelectTaskAndPatient(task);

					mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.search.patient.task"),
							resourceBundle.get("growl.search.patient.task.text"));

				} else {
					// no task was found
					logger.debug("No task with the given id found");
					mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.search.patient.notFount.task"), "",
							FacesMessage.SEVERITY_ERROR);
				}

			} else if (searchString.matches("^\\d{8}$")) { // piz
				// searching for piz (8 digits)
				logger.debug("Search for piz: " + searchString);

				Patient patient = searchHandler.serachForPiz(searchString);

				if (patient != null) {
					logger.debug("Found patient " + patient + " and adding to current worklist");
					patientHandlerAction.addNewInternalPatient(patient);
					worklistHandlerAction.addPatientToWorkList(patient, true);

					mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.search.patient.piz"),
							resourceBundle.get("growl.search.patient.piz.text"));

					// if alternate mode the create Task dialog will be shown
					// after the patient is added to the worklist
					if (alternateMode) {
						createTaskDialog.initAndPrepareBean(patient);
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
					worklistHandlerAction.addPatientToWorkList(searchResultSlide.getPatient(), true);
				} else {
					// no slide was found
					logger.debug("No slide with the given id found");
					mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.search.patient.notFount.slide"), "",
							FacesMessage.SEVERITY_ERROR);
				}
			} else if (searchString.matches("^(.+)[, ](.+)$")) {
				// name, surename; name surename
				String arr = searchString.split("[, ]");
				List<PatientList> listItem = searchHandler.searhcForPatientNameAndBirthday(arr[0], arr[1], null);

				if(listItem.size() == 1){ // one found, adding to worklist
					 logger.debug("Found one matching patient " + listItem.get(0).getPatient() + ", adding to worklist");
				}else if(listItem.size() > 1){ // more then one found
					
					 patientHandlerAction.initAddPatientDialog();
					
					 patientHandlerAction.setToManyMatchesInClinicDatabase(toMany);
					 patientHandlerAction.setActivePatientDialogIndex(0);
					 patientHandlerAction.setSearchForPatientName(resultArr[0]);
					 patientHandlerAction.setSearchForPatientSurname(resultArr[1]);
					
					 patientHandlerAction.setSearchForPatientList(result);
					
					 patientHandlerAction.showAddPatientDialog();
				}else{ // none found
					
				}
				
			} else if (searchString.matches("^[\\p{Alpha}\\-]+")) {
				// name

			} else {

			}

			
			 switch (search) {
			 case NAME_AND_SURNAME:
			 logger.debug("Searching for name (" + resultArr[0] + ") and
			 suranme (" + resultArr[1] + ")");
			
			 // overwrites the toManyPatiensInClinicDatabse flag, so perform
			 // method before search
			
			 result = patientHandlerAction.searchForPatientList("",
			 resultArr[0], resultArr[1], null);
			
			 if (result.size() == 1) {
			
			 patientHandlerAction.addNewInternalPatient(result.get(0).getPatient());
			 worklistHandlerAction.addPatientToWorkList(result.get(0).getPatient(),
			 true);
			
			 } else {
			 logger.debug("To many results found in clinic database, open

	addPatient dialog (" + resultArr[0]
			 + "," + resultArr[1] + ")");
			
			 boolean toMany = false;
			
			 if (patientHandlerAction.isToManyMatchesInClinicDatabase())
			 toMany = true;
			
			 patientHandlerAction.initAddPatientDialog();
			
			 patientHandlerAction.setToManyMatchesInClinicDatabase(toMany);
			 patientHandlerAction.setActivePatientDialogIndex(0);
			 patientHandlerAction.setSearchForPatientName(resultArr[0]);
			 patientHandlerAction.setSearchForPatientSurname(resultArr[1]);
			
			 patientHandlerAction.setSearchForPatientList(result);
			
			 patientHandlerAction.showAddPatientDialog();
			
			 }
	// break;
	// case NAME:
	// logger.debug("Searching for name, open addPatient dialog");
	//
	// result = patientHandlerAction.searchForPatientList("",
	// resultArr[0], null, null);
	//
	// boolean toMany = false;
	// if (patientHandlerAction.isToManyMatchesInClinicDatabase())
	// toMany = true;
	//
	// patientHandlerAction.initAddPatientDialog();
	//
	// patientHandlerAction.setToManyMatchesInClinicDatabase(toMany);
	// patientHandlerAction.setActivePatientDialogIndex(0);
	// patientHandlerAction.setSearchForPatientName(resultArr[0]);
	// patientHandlerAction.setSearchForPatientList(result);
	// patientHandlerAction.showAddPatientDialog();
	//
	// break;

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
