package org.histo.action.dialog.patient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.SearchHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.model.Contact;
import org.histo.model.PDFContainer;
import org.histo.model.Person;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.ui.ListChooser;
import org.primefaces.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class AddPatientDialogHandler extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SearchHandler searchHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	/**
	 * Patient for creating external Patient
	 */
	private Patient patient;

	/**
	 * Patient to search for, piz
	 */
	private String patientPiz;

	/**
	 * Patient to search for, name
	 */
	private String patientName;

	/**
	 * Patient to search for, surname
	 */
	private String patientSurname;

	/**
	 * Patient to search for, birthday
	 */
	private Date patientBirthday;

	/**
	 * True if to many matches have been found in the clinic database, an so the
	 * clinic database did not return any data
	 */
	private boolean toManyMatchesInClinicDatabase;

	/**
	 * List of all found Patients of the patientSearchRequest, PatientList is
	 * used instead of Patient because primefaces needs a unique row collum.
	 */
	private List<ListChooser<Patient>> patientList;

	/**
	 * Selectes PatientList item
	 */
	private ListChooser<Patient> selectedPatientListItem;

	public void initAndPrepareBean() {
		initBean("", "", "", null);
		prepareDialog();
	}

	public void initAndPrepareBeanFromExternal(String name, String surename, String piz, Date date) {
		initBean(name, surename, piz, date);
		searchForClinicPatienes();
		prepareDialog();
	}

	public void initBean(String name, String surename, String piz, Date date) {
		super.initBean(null, Dialog.WORKLIST_ADD_PATIENT);

		setPatientBirthday(date);
		setPatientName(name);
		setPatientPiz(surename);
		setPatientSurname(piz);

		setSelectedPatientListItem(null);
		setPatientList(null);

		setPatient(new Patient(new Person(new Contact())));
		getPatient().setTasks(new ArrayList<Task>());
		getPatient().setAttachedPdfs(new ArrayList<PDFContainer>());

		setToManyMatchesInClinicDatabase(false);
	}

	public void addExternalPatient(boolean addToWorklist) {
		try {
			if (getPatient() != null) {
				searchHandler.addExternalPatient(getPatient());
				if (addToWorklist)
					worklistViewHandlerAction.addPatientToWorkList(getPatient(), true);
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			// TODO: reload patient?
			onDatabaseVersionConflict();
		}
	}

	public void addClinicPatient(boolean addToWorklist) {
		try {
			if (getSelectedPatientListItem() != null) {
				searchHandler.addClinicPatient(getSelectedPatientListItem().getListItem());
				if (addToWorklist)
					worklistViewHandlerAction.addPatientToWorkList(getSelectedPatientListItem().getListItem(), true);
			}
		} catch (JSONException | CustomDatabaseInconsistentVersionException | CustomExceptionToManyEntries
				| CustomNullPatientExcepetion e) {
			// TODO: reload patient?
			onDatabaseVersionConflict();
		}
	}

	public void searchForClinicPatienes() {
		logger.debug("Searching for patients");
		try {
			setToManyMatchesInClinicDatabase(false);
			List<Patient> resultArr = new ArrayList<Patient>();

			if (getPatientPiz() != null && !getPatientPiz().isEmpty()) {
				if (getPatientPiz().matches("^[0-9]{8}$")) { // if full piz
					resultArr.add(searchHandler.serachForPiz(getPatientPiz()));
				} else if (getPatientPiz().matches("^[0-9]{6,8}$")) {
					// 6to 7 digits of piz
					resultArr.addAll(searchHandler.serachForPizRange(getPatientPiz()));
				}
			} else if ((getPatientName() != null && !getPatientName().isEmpty())
					|| (getPatientSurname() != null && !getPatientSurname().isEmpty())
					|| getPatientBirthday() != null) {

				resultArr.addAll(searchHandler.searhcForPatientNameAndBirthday(getPatientName(), getPatientSurname(),
						getPatientBirthday()));
			}

			setPatientList(ListChooser.getListAsIDList(resultArr));
			setSelectedPatientListItem(null);

		} catch (JSONException | CustomExceptionToManyEntries | CustomNullPatientExcepetion e) {
			setToManyMatchesInClinicDatabase(true);
			setPatientList(null);
			setSelectedPatientListItem(null);
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Searches for the given strings and adds the patient to the worklist if
	 * one patient was found. (this method reacts to return clicks)
	 * 
	 * @param addToWorklist
	 */
	public void searchAndAddUniqueItem(boolean addToWorklist) {
		logger.debug("Searching and adding if unique result");
		searchForClinicPatienes();

		// only adding if exactly one result was found
		if (getPatientList() != null && getPatientList().size() == 1) {
			logger.debug("One result found, adding to database");
			setSelectedPatientListItem(getPatientList().get(0));
			addClinicPatient(addToWorklist);

			hideDialog();
		} else {
			logger.debug("No result found, or result not unique");
		}
	}

}
