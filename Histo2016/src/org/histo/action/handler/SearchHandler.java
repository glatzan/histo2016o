package org.histo.action.handler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Patient;
import org.histo.util.StreamUtils;
import org.histo.util.TimeUtil;
import org.primefaces.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class SearchHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private SettingsHandler settingsHandler;

	/**
	 * Saves a patient found in the clinic-backend to the hist-backend or if the
	 * patient is found in the histo-backend the patient data are updated.
	 */
	public void addClinicPatient(Patient patient) throws CustomDatabaseInconsistentVersionException, JSONException,
			CustomExceptionToManyEntries, CustomNullPatientExcepetion {

		// add patient from the clinic-backend, get all data of this
		// patient, piz search is more specific
		if (!patient.getPiz().isEmpty()) {
			Patient clinicPatient;
			clinicPatient = settingsHandler.getClinicJsonHandler().getPatientFromClinicJson("/" + patient.getPiz());
			patient.copyIntoObject(clinicPatient);
		}

		// patient not in database, is new patient from database
		if (patient.getId() == 0) {
			logger.debug("New Patient, saving");
			// set add date
			patient.setCreationDate(System.currentTimeMillis());
			patient.setInDatabase(true);
			patientDao.savePatientAssociatedDataFailSave(patient, "log.patient.search.new");
		} else {
			logger.debug("Patient in database, updating and saving");
			patientDao.savePatientAssociatedDataFailSave(patient, "log.patient.search.update");
		}
	}

	/**
	 * Adds an external Patient to the database.
	 * 
	 * @param patient
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public void addExternalPatient(Patient patient) throws CustomDatabaseInconsistentVersionException {
		// create new external patient
		if (patient.getId() == 0) {
			patient.setExternalPatient(true);
			patient.setInDatabase(true);
			patient.setCreationDate(System.currentTimeMillis());
			patientDao.savePatientAssociatedDataFailSave(patient, "log.patient.extern.new");
		} else {
			patientDao.savePatientAssociatedDataFailSave(patient, "log.patient.edit");
		}
	}

	/**
	 * Searches in clinic and histo database, updates the histo database patient
	 * if found.
	 * 
	 * @param piz
	 * @return
	 * @throws CustomDatabaseInconsistentVersionException
	 * @throws CustomNullPatientExcepetion
	 * @throws CustomExceptionToManyEntries
	 * @throws JSONException
	 */
	public Patient serachForPiz(String piz) throws CustomDatabaseInconsistentVersionException, JSONException,
			CustomExceptionToManyEntries, CustomNullPatientExcepetion {
		// only search if 8 digit are provides
		if (piz != null && piz.matches("^[0-9]{8}$")) {
			Patient patient = patientDao.searchForPatientByPiz(piz);
			Patient clinicPatient;
			clinicPatient = settingsHandler.getClinicJsonHandler().getPatientFromClinicJson("/" + piz);
			if (patient != null) {
				if (patient.copyIntoObject(clinicPatient))
					patientDao.savePatientAssociatedDataFailSave(patient, "log.patient.search.update");
				return patient;
			} else {
				return clinicPatient;
			}
		}
		return null;
	}

	/**
	 * Searches for a range of not completed pizes 6 to 8 digits, searches only
	 * in histo database, clinic database does not support this. Updates found
	 * patients from clinic database.
	 * 
	 * @param piz
	 * @return
	 * @throws CustomNullPatientExcepetion
	 * @throws CustomExceptionToManyEntries
	 * @throws JSONException
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public List<Patient> serachForPizRange(String piz) throws JSONException, CustomExceptionToManyEntries,
			CustomNullPatientExcepetion, CustomDatabaseInconsistentVersionException {
		List<Patient> patients = patientDao.searchForPatientsByPiz(piz);

		// updates all patients from the local database with data from the
		// clinic backend
		for (Patient patient : patients) {
			Patient clinicPatient;

			clinicPatient = settingsHandler.getClinicJsonHandler().getPatientFromClinicJson("/" + patient.getPiz());
			if (patient.copyIntoObject(clinicPatient))
				patientDao.savePatientAssociatedDataFailSave(patient, "log.patient.search.update");

		}
		return patients;
	}

	/**
	 * 
	 * @param name
	 * @param surname
	 * @param birthday
	 * @return
	 * @throws CustomExceptionToManyEntries
	 * @throws CustomNullPatientExcepetion
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public List<Patient> searhcForPatientNameAndBirthday(String name, String surname, Date birthday)
			throws CustomExceptionToManyEntries, CustomNullPatientExcepetion,
			CustomDatabaseInconsistentVersionException {

		ArrayList<Patient> result = new ArrayList<Patient>();

		List<String> foundPiz = new ArrayList<String>();

		// getting all patienties from clinic database
		List<Patient> clinicPatients = settingsHandler.getClinicJsonHandler()
				.getPatientsFromClinicJson("?name=" + name + (surname != null ? ("&vorname=" + surname) : "")
						+ (birthday != null ? "&geburtsdatum=" + TimeUtil.formatDate(birthday, "yyyy-MM-dd") : ""));

		if (!clinicPatients.isEmpty()) {

			logger.trace("Patients in clinic backend found");

			// getting all pizes in one Array
			List<String> toSearchPizes = clinicPatients.stream().map(p -> p.getPiz()).collect(Collectors.toList());

			logger.trace("searching for " + toSearchPizes.size() + " patients in histo database");

			// creating a list of patient from the histo backend pizes
			// which where obtained from the clinic backend
			List<Patient> histoMatchList = patientDao.searchForPatientPizList(toSearchPizes);

			logger.trace("found " + histoMatchList.size() + " patients in histo database");

			// searching for every clinic patien a patientin in the database, if
			// foud the database patient will be updated
			foundPiz = clinicPatients.stream().filter(cP -> {

				try {
					// clinic patient found
					Patient res = histoMatchList.stream().filter(hP -> hP.getPiz().equals(cP.getPiz()))
							.collect(StreamUtils.singletonCollector());

					histoMatchList.remove(res);

					if (res.copyIntoObject(cP)) {
						try {
							patientDao.savePatientAssociatedDataFailSave(res, "log.patient.search.update");
						} catch (Exception e) {
						}
					}

					result.add(res);

					return true;
				} catch (IllegalStateException e) {
					// no clinic patient found
					cP.setInDatabase(false);
					result.add(cP);
					return false;
				}
			}).map(cp -> cp.getPiz()).collect(Collectors.toList());
		}

		// search for external patient in histo database, excluding the
		// already found patients via piz
		List<Patient> histoPatients = patientDao.getPatientsByNameSurnameDateExcludePiz(name, surname, birthday,
				foundPiz);

		result.addAll(histoPatients);

		return result;
	}
}
