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
import org.histo.model.Person;
import org.histo.model.patient.Patient;
import org.histo.ui.ListChooser;
import org.primefaces.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
public class AddPatientDialogHandler extends AbstractDialog {

	@Autowired
	private SearchHandler searchHandler;

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

	/**
	 * Patient for creating external Patient
	 */
	@Getter
	@Setter
	private Patient patient;

	/**
	 * Patient to search for, piz
	 */
	@Getter
	@Setter
	private String patientPiz;

	/**
	 * Patient to search for, name
	 */
	@Getter
	@Setter
	private String patientName;

	/**
	 * Patient to search for, surname
	 */
	@Getter
	@Setter
	private String patientSurname;

	/**
	 * Patient to search for, birthday
	 */
	@Getter
	@Setter
	private Date patientBirthday;

	/**
	 * True if to many matches have been found in the clinic database, an so the
	 * clinic database did not return any data
	 */
	@Getter
	@Setter
	private boolean toManyMatchesInClinicDatabase;

	/**
	 * List of all found Patients of the patientSearchRequest, PatientList is
	 * used instead of Patient because primefaces needs a unique row collum.
	 */
	@Getter
	@Setter
	private List<ListChooser<Patient>> patientList;

	/**
	 * Selectes PatientList item
	 */
	@Getter
	@Setter
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

		setPatient(new Patient(new Person()));

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
