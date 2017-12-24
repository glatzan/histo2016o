package org.histo.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.dao.GenericDAO;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Patient;
import org.histo.util.StreamUtils;
import org.histo.util.TimeUtil;
import org.primefaces.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Service
@Getter
@Setter
public class PatientService {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	/**
	 * Creates and adds an external patient to the database TODO: obtain piz
	 * from pdv
	 * 
	 * @param patient
	 */
	public void createExternalPatient(Patient patient) {
		if (patient.getId() == 0) {
			patient.setExternalPatient(true);
			patient.setInDatabase(true);
			patient.setCreationDate(System.currentTimeMillis());
			genericDAO.savePatientData(patient, "log.patient.extern.new");
		} else {
			genericDAO.savePatientData(patient, "log.patient.edit");
		}
	}

	/**
	 * Saves a patient in the database (obtained from pdv).
	 * 
	 * @throws CustomNullPatientExcepetion
	 * @throws CustomExceptionToManyEntries
	 * @throws JSONException
	 */
	public void addPatient(Patient patient)
			throws JSONException, CustomExceptionToManyEntries, CustomNullPatientExcepetion {

		// add patient from the clinic-backend, get all data of this
		// patient, piz search is more specific
		if (!patient.getPiz().isEmpty()) {
			logger.debug("Getting data from pdv for patient " + patient.getPiz());
			Patient clinicPatient;
			clinicPatient = globalSettings.getClinicJsonHandler().getPatientFromClinicJson(patient.getPiz());
			patient.copyIntoObject(clinicPatient);
		}

		// patient not in database, is new patient from database
		if (patient.getId() == 0) {
			logger.debug("Adding patient (" + patient.getPiz() + ") to database");
			// set add date
			patient.setCreationDate(System.currentTimeMillis());
			patient.setInDatabase(true);
			genericDAO.savePatientData(patient, "log.patient.search.new");
		} else {
			logger.debug("Patient (\" + patient.getPiz() + \") in database, updating and saving");
			genericDAO.savePatientData(patient, "log.patient.search.update");
		}
	}

	/**
	 * Searches for a piz in the pdv as well as in the programm's database. If
	 * found in the programm's database the patient will be updated with found
	 * pdv data.
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
			Patient pdvPatient = globalSettings.getClinicJsonHandler().getPatientFromClinicJson(piz);
			if (patient != null) {
				if (patient.copyIntoObject(pdvPatient)) {
					logger.debug("Patient found in database, updating with pdv data");
					genericDAO.savePatientData(patient, "log.patient.search.update");
				}
				return patient;
			} else {
				logger.debug("Patient not in database, returning pdv data");
				return pdvPatient;
			}
		}
		return null;
	}

	/**
	 * Searches for a range of not completed pizes 6 to 8 digits, searches only
	 * in histo database, pdv database does not support this. Updates found
	 * patients from pdv database.
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
			Patient pdvPatient = globalSettings.getClinicJsonHandler().getPatientFromClinicJson(patient.getPiz());

			logger.debug("Patient (" + piz + ") found in database, updating with pdv data");

			if (patient.copyIntoObject(pdvPatient))
				genericDAO.savePatientData(patient, "log.patient.search.update");

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
	public List<Patient> searhcForPatient(String name, String surname, Date birthday)
			throws CustomExceptionToManyEntries, CustomNullPatientExcepetion,
			CustomDatabaseInconsistentVersionException {

		ArrayList<Patient> result = new ArrayList<Patient>();

		List<String> foundPiz = new ArrayList<String>();

		// getting all patienties from clinic database
		List<Patient> clinicPatients = globalSettings.getClinicJsonHandler()
				.getPatientsFromClinicJson("?name=" + name + (surname != null ? ("&vorname=" + surname) : "")
						+ (birthday != null ? "&geburtsdatum=" + TimeUtil.formatDate(birthday, "yyyy-MM-dd") : ""));

		if (!clinicPatients.isEmpty()) {

			logger.trace("Found " + clinicPatients.size() + " patients in pdv ");

			// getting all pizes in one Array
			List<String> toSearchPizes = clinicPatients.stream().map(p -> p.getPiz()).collect(Collectors.toList());

			logger.trace("Searching for " + toSearchPizes.size() + " patients in database");

			// creating a list of patient from the histo backend pizes
			// which where obtained from the clinic backend
			List<Patient> histoMatchList = patientDao.searchForPatientPizList(toSearchPizes);

			logger.trace("Found " + histoMatchList.size() + " patients in database");

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
							genericDAO.savePatientData(res, "log.patient.search.update");
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
		// TODO Remove if piz generation is a thing
		List<Patient> histoPatients = patientDao.getPatientsByNameSurnameDateExcludePiz(name, surname, birthday,
				foundPiz);

		result.addAll(histoPatients);

		return result;
	}
}
