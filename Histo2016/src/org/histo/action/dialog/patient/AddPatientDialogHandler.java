package org.histo.action.dialog.patient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.dialog.AbstractTabDialog;
import org.histo.action.dialog.AbstractTabDialog.AbstractTab;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.model.Contact;
import org.histo.model.Person;
import org.histo.model.patient.Patient;
import org.histo.service.PatientService;
import org.histo.ui.ListChooser;
import org.histo.worklist.search.WorklistSimpleSearch;
import org.primefaces.event.SelectEvent;
import org.primefaces.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class AddPatientDialogHandler extends AbstractTabDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientService patientService;

	private ClinicSearchTab clinicSearchTab;

	private ExternalPatientTab externalPatientTab;

	public AddPatientDialogHandler() {
		setClinicSearchTab(new ClinicSearchTab());
		setExternalPatientTab(new ExternalPatientTab());

		tabs = new AbstractTab[] { clinicSearchTab, externalPatientTab };
	}

	public void initAndPrepareBean() {
		initBean("", "", "", null);
		prepareDialog();
	}

	public void initAndPrepareBeanFromExternal(String name, String surename, String piz, Date date) {
		initBean(name, surename, piz, date);
		clinicSearchTab.searchForClinicPatienes();
		prepareDialog();
	}

	public void initBean(String name, String surename, String piz, Date date) {
		super.initBean(null, Dialog.PATIENT_ADD);

		for (int i = 0; i < tabs.length; i++) {
			tabs[i].initTab();
		}

		onTabChange(tabs[0]);
	}

	@Getter
	@Setter
	public class ClinicSearchTab extends AbstractTab {

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
		 * True if to many matches have been found in the clinic database, an so
		 * the clinic database did not return any data
		 */
		private boolean toManyMatchesInClinicDatabase;

		/**
		 * List of all found Patients of the patientSearchRequest, PatientList
		 * is used instead of Patient because primefaces needs a unique row
		 * collum.
		 */
		private List<ListChooser<Patient>> patientList;

		/**
		 * Selectes PatientList item
		 */
		private ListChooser<Patient> selectedPatientListItem;

		public ClinicSearchTab() {
			setTabName("ClinicSearchTab");
			setName("dialog.addPatient.search");
			setViewID("clinicSearch");
			setCenterInclude("include/clinicSearch.xhtml");
		}

		public boolean initTab() {
			return initTab("", "", "", null);
		}
		
		public boolean initTab(String name, String surename, String piz, Date date) {
			setPatientBirthday(date);
			setPatientName(name);
			setPatientPiz(surename);
			setPatientSurname(piz);
			setSelectedPatientListItem(null);
			setPatientList(null);
			setToManyMatchesInClinicDatabase(false);
			return true;
		}

		public void updateData() {
		}

		public void addClinicPatient() {
			try {
				if (getSelectedPatientListItem() != null) {
					patientService.addPatient(getSelectedPatientListItem().getListItem());
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
						resultArr.add(patientService.serachForPiz(getPatientPiz()));
					} else if (getPatientPiz().matches("^[0-9]{6,8}$")) {
						// 6to 7 digits of piz
						resultArr.addAll(patientService.serachForPizRange(getPatientPiz()));
					}
				} else if ((getPatientName() != null && !getPatientName().isEmpty())
						|| (getPatientSurname() != null && !getPatientSurname().isEmpty())
						|| getPatientBirthday() != null) {

					resultArr.addAll(patientService.searhcForPatient(getPatientName(), getPatientSurname(),
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
		 * Searches for the given strings and adds the patient to the worklist
		 * if one patient was found. (this method reacts to return clicks)
		 * 
		 * @param addToWorklist
		 */
		public void onQuickSubmit() {
			logger.debug("Quicksubmit, search for result and adding result to worklist if unique result");
			searchForClinicPatienes();

			// only adding if exactly one result was found
			if (getPatientList() != null && getPatientList().size() == 1) {
				logger.debug("One result found, adding to database");
				setSelectedPatientListItem(getPatientList().get(0));
				addClinicPatient();

				hideDialog();
			} else {
				logger.debug("No result found, or result not unique");
			}
		}
	}

	@Getter
	@Setter
	public class ExternalPatientTab extends AbstractTab {

		/**
		 * Patient for creating external Patient
		 */
		private Patient patient;

		public ExternalPatientTab() {
			setTabName("ExternalPatientTab");
			setName("dialog.addPatient.add");
			setViewID("externalPatient");
			setCenterInclude("include/externalPatient.xhtml");
		}

		public boolean initTab() {
			setPatient(new Patient(new Person(new Contact())));
			getPatient().getPerson().setGender(null);

			return true;
		}

		public void updateData() {
		}

		/**
		 * Closes the dialog if patient was added
		 * 
		 * @param event
		 */
		public void onConfirmExternalPatientDialog(SelectEvent event) {
			if (event.getObject() instanceof Boolean && ((Boolean) event.getObject()).booleanValue()) {
				hideDialog();
			}
		}
	}
}
