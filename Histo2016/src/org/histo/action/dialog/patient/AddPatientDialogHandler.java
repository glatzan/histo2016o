package org.histo.action.dialog.patient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractTabDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.model.Contact;
import org.histo.model.Person;
import org.histo.model.patient.Patient;
import org.histo.model.user.HistoPermissions;
import org.histo.service.PatientService;
import org.histo.ui.ListChooser;
import org.primefaces.event.SelectEvent;
import org.primefaces.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
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

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	private ClinicSearchTab clinicSearchTab;

	private ExternalPatientTab externalPatientTab;

	private boolean showExternPatientTab;

	public AddPatientDialogHandler() {
		setClinicSearchTab(new ClinicSearchTab());
		setExternalPatientTab(new ExternalPatientTab());

		tabs = new AbstractTab[] { clinicSearchTab, externalPatientTab };
	}

	public void initAndPrepareBean() {
		initBean("", "", "", null, true);
		prepareDialog();
	}

	public void initAndPrepareBean(boolean showExternPatientTab) {
		initBean("", "", "", null, showExternPatientTab);
		prepareDialog();
	}

	public void initAndPrepareBeanFromExternal(String name, String surename, String piz, Date date) {
		initBean(name, surename, piz, date, true);
		clinicSearchTab.searchForClinicPatienes();
		prepareDialog();
	}

	public void initBean(String name, String surename, String piz, Date date, boolean showExternPatientTab) {
		super.initBean(null, Dialog.PATIENT_ADD);
		this.showExternPatientTab = showExternPatientTab;

		for (int i = 0; i < tabs.length; i++) {
			tabs[i].initTab();
		}

		clinicSearchTab.setPatientName(name);
		clinicSearchTab.setPatientSurname(surename);
		clinicSearchTab.setPatientPiz(piz);
		clinicSearchTab.setPatientBirthday(date);

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
		 * True if to many matches have been found in the clinic database, an so the
		 * clinic database did not return any data
		 */
		private boolean toManyMatchesInClinicDatabase;

		/**
		 * List of all found Patients of the patientSearchRequest, PatientList is used
		 * instead of Patient because primefaces needs a unique row collum.
		 */
		private List<ListChooser<Patient>> patientList;

		/**
		 * Selectes PatientList item
		 */
		private ListChooser<Patient> selectedPatientListItem;

		/**
		 * If the user has not the permission to search the pdv only the local database
		 * will be searched for.
		 */
		private boolean searchLocalDatabaseOnly;

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
			setSearchLocalDatabaseOnly(
					!userHandlerAction.currentUserHasPermission(HistoPermissions.PATIENT_EDIT_ADD_CLINIC));
			return true;
		}

		public void updateData() {
		}

		public void selectPatientAndHideDialog() {
			hideDialog(getSelectedPatientListItem().getListItem());
		}

		/**
		 * Search for pizes or given namen, firstname and birthday. Prefers pizes if not
		 * null. Considers search only in local database if the user has not the
		 * matching rights to add new clinic patients to the local database
		 */
		public void searchForClinicPatienes() {
			logger.debug("Searching for patients");
			try {
				setToManyMatchesInClinicDatabase(false);
				List<Patient> resultArr = new ArrayList<Patient>();

				if (getPatientPiz() != null && !getPatientPiz().isEmpty()) {
					if (getPatientPiz().matches("^[0-9]{8}$")) { // if full piz
						resultArr.add(patientService.serachForPiz(getPatientPiz(), isSearchLocalDatabaseOnly()));
					} else if (getPatientPiz().matches("^[0-9]{6,8}$")) {
						// 6to 7 digits of piz
						// isSearchLocalDatabaseOnly() can be ignored because this function is only
						// supported by local database
						resultArr.addAll(patientService.serachForPizRange(getPatientPiz()));
					}

				} else if ((getPatientName() != null && !getPatientName().isEmpty())
						|| (getPatientSurname() != null && !getPatientSurname().isEmpty())
						|| getPatientBirthday() != null) {

					AtomicBoolean toManyEntries = new AtomicBoolean(false);

					resultArr.addAll(patientService.searchForPatient(getPatientName(), getPatientSurname(),
							getPatientBirthday(), isSearchLocalDatabaseOnly(), toManyEntries));

					setToManyMatchesInClinicDatabase(toManyEntries.get());
					logger.debug(isToManyMatchesInClinicDatabase());
				}

				logger.debug(isToManyMatchesInClinicDatabase());

				setPatientList(ListChooser.getListAsIDList(resultArr));
				setSelectedPatientListItem(null);

			} catch (JSONException | CustomExceptionToManyEntries | CustomNullPatientExcepetion e) {
				setToManyMatchesInClinicDatabase(true);
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}

			logger.debug(isToManyMatchesInClinicDatabase());
		}

		/**
		 * Searches for the given strings and adds the patient to the worklist if one
		 * patient was found. (this method reacts to return clicks)
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
				selectPatientAndHideDialog();
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
			setDisabled(!userHandlerAction.currentUserHasPermission(HistoPermissions.PATIENT_EDIT_ADD_EXTERN)
					|| !showExternPatientTab);
			setPatient(new Patient(new Person(new Contact())));
			getPatient().getPerson().setGender(null);
			return true;
		}

		public void updateData() {
		}

		
		/**
		 * Closes the dialog in order to add the patient
		 * 
		 * @param event
		 */
		public void onConfirmExternalPatientDialog(SelectEvent event) {
			if (event.getObject() != null && event.getObject() instanceof Patient) {
				hideDialog(event.getObject());
			}
		}
	}
}
