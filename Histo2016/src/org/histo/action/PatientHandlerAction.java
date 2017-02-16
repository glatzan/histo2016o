package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.dao.GenericDAO;
import org.histo.dao.PatientDao;
import org.histo.model.PDFContainer;
import org.histo.model.Person;
import org.histo.model.patient.Patient;
import org.histo.ui.PatientList;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.prism.paint.Paint;

@Component
@Scope(value = "session")
public class PatientHandlerAction implements Serializable {

	private static final long serialVersionUID = -7781752890620696154L;

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private WorklistHandlerAction worklistHandlerAction;

	/**
	 * Tabindex of the addPatient dialog
	 */
	private int activePatientDialogIndex = 0;

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

	/**
	 * True if to many matches have been found in the clinic database, an so the
	 * clinic database did not return any data
	 */
	private boolean toManyMatchesInClinicDatabase;



	public void showAndInitAddPatientDialog() {
		initAddPatientDialog();
		showAddPatientDialog();
	}

	public void showAddPatientDialog() {
		mainHandlerAction.showDialog(Dialog.WORKLIST_ADD_PATIENT);
	}

	public void initAddPatientDialog() {
		setTmpPatient(new Patient());
		getTmpPatient().setPerson(new Person());

		setSearchForPatientBirthday(null);
		setSearchForPatientName("");
		setSearchForPatientSurname("");
		setSearchForPatientPiz("");

		setSelectedPatientFromSearchList(null);
		setSearchForPatientList(null);

		setToManyMatchesInClinicDatabase(false);
	}

	/**
	 * Adds an external Patient to the database.
	 * 
	 * @param patient
	 *            Patient to save in the database and add to worklist
	 */
	public void addNewExternalPatient(Patient patient) {
		// marks the patient as externally
		patient.setExternalPatient(true);
		patient.setCreationDate(System.currentTimeMillis());

		genericDAO.save(patient, resourceBundle.get("log.patient.extern.new", patient.getPerson().getName(),
				patient.getPerson().getSurname()), patient);
	}

	/**
	 * Saves a patient found in the clinic-backend to the hist-backend or if the
	 * patient is found in the histo-backend the patient data are updated.
	 */
	public void addNewInternalPatient(Patient patient) {
		if (patient != null) {

			// add patient from the clinic-backend, get all data of this
			// patient, piz search is more specific
			if (!patient.getPiz().isEmpty()) {
				Patient clinicPatient = mainHandlerAction.getSettings().getClinicJsonHandler()
						.getPatientFromClinicJson("/" + patient.getPiz());
				patient.copyIntoObject(clinicPatient);
			}

			// patient not in database, is new patient from database
			if (patient.getId() == 0) {
				// set add date
				patient.setCreationDate(System.currentTimeMillis());
				genericDAO.save(patient, resourceBundle.get("log.patient.search.new", patient.toString()), patient);
			} else
				genericDAO.save(patient, resourceBundle.get("log.patient.search.update", patient.toString()), patient);

		}
	}

	public void addNewInternalPatientFromGui(Patient patient) {
		if (patient != null) {
			addNewInternalPatient(patient);
			worklistHandlerAction.addPatientToWorkList(patient, true);
			mainHandlerAction.hideDialog(Dialog.WORKLIST_ADD_PATIENT);
		}
	}

	/**
	 * Sets the tmpPatient to bull
	 */
	public void clearTmpPatient() {
		setTmpPatient(null);
	}

	public void searchPatients() {
		setSearchForPatientList(searchForPatientList(getSearchForPatientPiz(), getSearchForPatientName(),
				getSearchForPatientSurname(), getSearchForPatientBirthday()));
	}

	/**
	 * Searches for a patient with the given paramenters in the clinic and in
	 * the histo backend.
	 * http://auginfo/piz?name=xx&vorname=xx&geburtsdatum=2000-01-01
	 * 
	 * @param piz
	 */
	public List<PatientList> searchForPatientList(String piz, String name, String surname, Date birthday) {
		ArrayList<PatientList> result = new ArrayList<>();

		// id for patientList, used by primefaces to get the selected row
		int id = 0;

		// if piz is given ignore other parameters
		if (piz != null && piz.matches("^[0-9]{6,8}$")) {
			List<Patient> patients = patientDao.searchForPatientsPiz(piz);

			// updates all patients from the local database with data from the
			// clinic backend
			for (Patient patient : patients) {
				Patient clinicPatient = mainHandlerAction.getSettings().getClinicJsonHandler()
						.getPatientFromClinicJson("/" + patient.getPiz());
				patient.copyIntoObject(clinicPatient);
				result.add(new PatientList(id++, patient));
			}

			// saves the results
			genericDAO.saveCollection(patients);

			// only get patient from clinic backend if piz is completely
			// provided and was not added to the local database before
			if (piz.matches("^[0-9]{8}$") && patients.isEmpty()) {
				Patient clinicPatient = mainHandlerAction.getSettings().getClinicJsonHandler()
						.getPatientFromClinicJson("/" + piz);
				PatientList patient = new PatientList(id++, clinicPatient);
				result.add(patient);
			}

			// TODO: remove or write to an other method
			// if (!result.isEmpty())
			// // only one match is expected, set this as selected patient
			// setSelectedPatientFromSearchList(result.isEmpty() ? null :
			// result.get(0));

		} else if ((name != null && !name.isEmpty()) || (surname != null && !surname.isEmpty()) || birthday != null) {

			// getting all patients with given parameters from the clinic
			// backend

			List<Patient> clinicPatients;

			// list for excluding results for the histo database search
			ArrayList<String> foundPiz = new ArrayList<String>();

			try {
				clinicPatients = mainHandlerAction.getSettings().getClinicJsonHandler().getPatientsFromClinicJson(
						"?name=" + name + (surname != null ? ("&vorname=" + surname) : "") + (birthday != null
								? "&geburtsdatum=" + TimeUtil.formatDate(birthday, "yyyy-MM-dd") : ""));

				// list of pizes to serach in the histo database
				ArrayList<String> toSearchPizes = new ArrayList<String>(clinicPatients.size());

				if (!clinicPatients.isEmpty()) {

					logger.trace("Patients in clinic backend found");

					// getting all pizes in one Array
					for (Patient cPatient : clinicPatients) {
						toSearchPizes.add(cPatient.getPiz());
					}

					List<Patient> histoMatchList = new ArrayList<Patient>(0);

					// creating a list of patient from the histo backend pizes
					// wich
					// where obtaind from the clinic backend
					histoMatchList = patientDao.searchForPatientPizes(toSearchPizes);

					for (Patient cPatient : clinicPatients) {

						PatientList patientList = null;

						// search if already added to the histo backend
						for (Patient hPatient : histoMatchList) {
							if (cPatient.getPiz().equals(hPatient.getPatient().getPiz())) {
								patientList = new PatientList(id++, hPatient);
								histoMatchList.remove(hPatient);
								foundPiz.add(hPatient.getPiz());
								// TODO update the patient in histo database
								break;
							}
						}

						// was not added add to normal list
						if (patientList == null) {
							patientList = new PatientList(id++, cPatient);
							patientList.setNotHistoDatabase(true);
						}
						result.add(patientList);
					}
				}

				setToManyMatchesInClinicDatabase(false);

			} catch (CustomExceptionToManyEntries e) {
				logger.debug("To many patiens were found in the clinc database");
				setToManyMatchesInClinicDatabase(true);
			}

			// search for external patient in histo database, excluding the
			// already found patients via piz
			List<Patient> histoPatients = patientDao.getPatientsByNameSurnameDateExcludePiz(name, surname, birthday,
					foundPiz);

			for (Patient patient : histoPatients) {
				result.add(new PatientList(id++, patient));
			}

		}

		return result;
	}

	/**
	 * Shows a dialog for editing patients which are only stored in the histo
	 * database (external patients)
	 * 
	 * @param patient
	 */
	public void prepareEditExternalPatientDialog(Patient patient) {
		setTmpPatient(patient);
		mainHandlerAction.showDialog(Dialog.PATIENT_EDIT);
	}

	/**
	 * Saves the edited patient data and closes the dialog
	 * 
	 * @param patient
	 */
	public void saveEditedExternalPatient(Patient patient) {
		genericDAO.save(patient, resourceBundle.get("log.patient.extern.edit"), patient);
		mainHandlerAction.hideDialog(Dialog.PATIENT_EDIT);
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
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

	public int getActivePatientDialogIndex() {
		return activePatientDialogIndex;
	}

	public void setActivePatientDialogIndex(int activePatientDialogIndex) {
		this.activePatientDialogIndex = activePatientDialogIndex;
	}

	public Patient getTmpPatient() {
		return tmpPatient;
	}

	public void setTmpPatient(Patient tmpPatient) {
		this.tmpPatient = tmpPatient;
	}

	public boolean isToManyMatchesInClinicDatabase() {
		return toManyMatchesInClinicDatabase;
	}

	public void setToManyMatchesInClinicDatabase(boolean toManyMatchesInClinicDatabase) {
		this.toManyMatchesInClinicDatabase = toManyMatchesInClinicDatabase;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
