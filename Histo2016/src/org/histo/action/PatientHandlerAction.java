package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.histo.config.HistoSettings;
import org.histo.dao.GenericDAO;
import org.histo.dao.PatientDao;
import org.histo.model.Patient;
import org.histo.model.Person;
import org.histo.ui.PatientList;
import org.histo.util.PersonAdministration;
import org.histo.util.ResourceBundle;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class PatientHandlerAction implements Serializable {

	private static final long serialVersionUID = -7781752890620696154L;

	@Autowired
	private HelperHandlerAction helper;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	PatientDao patientDao;

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
	 * Shows dialog for adding and creating new patients.
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

		helper.showDialog(HistoSettings.DIALOG_PATIENT_ADD, 1024, 600, false, false, true);
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
		patient.setAddDate(new Date(System.currentTimeMillis()));

		genericDAO.save(patient, resourceBundle.get("log.patient.extern.new"), patient);

		hidePatientDialog();
	}

	/**
	 * Saves a patient found in the clinic-backend to the hist-backend or if the
	 * patient is found in the histo-backend the patient data are updated.
	 */
	public void addNewInterlPatient(Patient patient) {
		if (patient != null) {

			// add patient from the clinic-backend, get all data of this patient
			if (!patient.getPiz().isEmpty()) {
				PersonAdministration admim = new PersonAdministration();
				String userResult = admim.getRequest(HistoSettings.PATIENT_GET_URL + "/" + patient.getPiz());
				admim.updatePatientFromClinicJson(patient, userResult);
			}

			// patient not in database, is new patient from database
			if (patient.getId() == 0) {
				// set add date
				patient.setAddDate(new Date(System.currentTimeMillis()));
				genericDAO.save(patient, resourceBundle.get("log.patient.search.new"), patient);
			} else
				genericDAO.save(patient, resourceBundle.get("log.patient.search.update"), patient);

			hidePatientDialog();
		}
	}

	/**
	 * Hides the "/pages/dialog/patient/addPatient" dialog
	 */
	public void hidePatientDialog() {
		helper.hideDialog(HistoSettings.DIALOG_PATIENT_ADD);
	}

	/**
	 * Sets the tmpPatient to bull
	 */
	public void clearTmpPatient() {
		setTmpPatient(null);
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

			ArrayList<String> notFoundPiz = new ArrayList<String>(clinicPatients.size());
			ArrayList<String> foundPiz = new ArrayList<String>();

			if (!clinicPatients.isEmpty()) {
				// getting all pizes in one Array
				for (Patient cPatient : clinicPatients) {
					notFoundPiz.add(cPatient.getPiz());
				}

				List<Patient> histoMatchList = new ArrayList<Patient>(0);

				// creating a list of patient from the histo backend pizes wich where obtaind from the clinic backend
				histoMatchList = patientDao.searchForPatientPizes(notFoundPiz);

				for (Patient cPatient : clinicPatients) {

					PatientList patientList = null;

					// search if already added to the histo backend
					for (Patient hPatient : histoMatchList) {
						if (cPatient.getPiz().equals(hPatient.getPatient().getPiz())) {
							patientList = new PatientList(id++, hPatient);
							histoMatchList.remove(hPatient);
							foundPiz.add(hPatient.getPiz());
							System.out.println("found");
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

			// search for external patient in histo database, excluding the already found patients via piz
			List<Patient> histoPatients = patientDao.getPatientsByNameSurnameDateExcludePiz(name, surname, birthday,
					foundPiz);

			for (Patient patient : histoPatients) {
				result.add(new PatientList(id++, patient));
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
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
