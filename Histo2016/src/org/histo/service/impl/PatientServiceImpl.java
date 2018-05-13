package org.histo.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.histo.action.handler.GlobalSettings;
import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.service.PatientService;
import org.histo.service.dao.PatientDao;
import org.histo.util.HistoUtil;
import org.primefaces.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Class for manipulating patients and updating them via the pdv backend
 * 
 * @author andi
 *
 */
@Service("patientService")
@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
public class PatientServiceImpl extends AbstractService implements PatientService {

	@Autowired
	private GlobalSettings globalSettings;

	@Autowired
	private PatientDao patientDao;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.PatientService#addPatient(org.histo.model.patient.
	 * Patient)
	 */
	@Override
	public void addPatient(Patient patient)
			throws JSONException, CustomExceptionToManyEntries, CustomNullPatientExcepetion {
		addPatient(patient, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.PatientService#addPatient(org.histo.model.patient.
	 * Patient, boolean)
	 */
	@Override
	public void addPatient(Patient patient, boolean update)
			throws JSONException, CustomExceptionToManyEntries, CustomNullPatientExcepetion {

		// add patient from the clinic-backend, get all data of this
		// patient, piz search is more specific
		if (HistoUtil.isNotNullOrEmpty(patient.getPiz()) && !globalSettings.getProgramSettings().isOffline()
				&& update) {
			logger.debug("Getting data from pdv for patient " + patient.getPiz());
			globalSettings.getClinicJsonHandler().updatePatientFromClinicJson(patient);
		}

		// patient not in database, is new patient from database
		if (patient.getId() == 0) {
			logger.debug("Adding patient (" + patient.getPiz() + ") to database");
			// set add date
			patient.setCreationDate(System.currentTimeMillis());
			patient.setInDatabase(true);
			// setting external patient if piz is null
			patient.setExternalPatient(HistoUtil.isNullOrEmpty(patient.getPiz()));
			patientDao.savePatientData(patient,
					patient.isExternalPatient() ? "log.patient.extern.new" : "log.patient.search.new");
		} else {
			logger.debug("Patient (" + patient.getPiz() + ") in database, updating and saving");
			patientDao.savePatientData(patient, "log.patient.search.update");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.PatientService#removePatient(org.histo.model.patient.
	 * Patient)
	 */
	@Override
	public void removePatient(Patient patient) {
		if (patient.getTasks().isEmpty()) {
			patientDao.deletePatientData(patient, "log.patient.remove", patient);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.PatientService#archivePatient(org.histo.model.patient.
	 * Patient)
	 */
	@Override
	public void archivePatient(Patient patient) {
		if (patient.getTasks().isEmpty()) {
			patient.setArchived(true);
			patientDao.savePatientData(patient, "log.patient.archived", patient);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.PatientService#mergePatient(org.histo.model.patient.
	 * Patient, org.histo.model.patient.Patient)
	 */
	@Override
	public void mergePatient(Patient from, Patient to) {
		mergePatient(from, to, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.PatientService#mergePatient(org.histo.model.patient.
	 * Patient, org.histo.model.patient.Patient, java.util.List)
	 */
	@Override
	public void mergePatient(Patient from, Patient to, List<Task> tasksToMerge) {
		List<Task> tasksFrom = tasksToMerge == null ? from.getTasks() : tasksToMerge;

		if (tasksFrom == null)
			return;

		for (Task task : tasksFrom) {
			task.setParent(to);
			patientDao.savePatientData(task, "log.patient.merge", from.getPerson().getFullName());
		}

		from.setTasks(new ArrayList<Task>());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.PatientService#findPatientByPiz(java.lang.String,
	 * boolean)
	 */
	@Override
	public Patient findPatientByPiz(String piz, boolean localDatabaseOnly)
			throws HistoDatabaseInconsistentVersionException, JSONException, CustomExceptionToManyEntries,
			CustomNullPatientExcepetion {
		// only search if 8 digit are provides
		if (piz != null && piz.matches("^[0-9]{8}$")) {
			Patient patient = patientDao.findByPiz(piz);

			// abort search, not found in local database
			if (patient == null && localDatabaseOnly)
				return null;

			if (!globalSettings.getProgramSettings().isOffline()) {
				Patient pdvPatient = globalSettings.getClinicJsonHandler().getPatientFromClinicJson(piz);

				if (patient != null) {
					if (patient.copyIntoObject(pdvPatient)) {
						logger.debug("Patient found in database, updating with pdv data");
						patientDao.savePatientData(patient, "log.patient.search.update");
					}
					return patient;
				} else {
					logger.debug("Patient not in database, returning pdv data");
					return pdvPatient;
				}
			} else
				return patient;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.PatientService#findPatientListByPiz(java.lang.String)
	 */
	@Override
	public List<Patient> listByPiz(String piz) throws JSONException, CustomExceptionToManyEntries,
			CustomNullPatientExcepetion, HistoDatabaseInconsistentVersionException {

		List<Patient> patients = patientDao.findListByPiz(piz);

		// updates all patients from the local database with data from the
		// clinic backend
		for (Patient patient : patients) {
			Patient pdvPatient = globalSettings.getClinicJsonHandler().getPatientFromClinicJson(patient.getPiz());

			logger.debug("Patient (" + piz + ") found in database, updating with pdv data");

			if (patient.copyIntoObject(pdvPatient))
				patientDao.savePatientData(patient, "log.patient.search.update");

		}
		return patients;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.PatientService#findPatient(java.lang.String,
	 * java.lang.String, java.util.Date, boolean)
	 */
	@Override
	public List<Patient> list(String name, String surname, Date birthday, boolean localDatabaseOnly)
			throws HistoDatabaseInconsistentVersionException, CustomNullPatientExcepetion {
		return list(name, surname, birthday, localDatabaseOnly, new AtomicBoolean(false));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.PatientService#findPatient(java.lang.String,
	 * java.lang.String, java.util.Date, boolean,
	 * java.util.concurrent.atomic.AtomicBoolean)
	 */
	@Override
	public List<Patient> list(String name, String surname, Date birthday, boolean localDatabaseOnly,
			AtomicBoolean toManyEntriesInClinicDatabase)
			throws CustomNullPatientExcepetion, HistoDatabaseInconsistentVersionException {

		ArrayList<Patient> result = new ArrayList<Patient>();

		List<Patient> histoPatients = patientDao.find(name, surname, birthday, false, false, true);

		List<Patient> clinicPatients = new ArrayList<Patient>();

		if (!localDatabaseOnly && !globalSettings.getProgramSettings().isOffline()) {
			try {
				clinicPatients = globalSettings.getClinicJsonHandler().getPatientsFromClinicJson(name, surname,
						birthday);
			} catch (CustomExceptionToManyEntries e) {
				toManyEntriesInClinicDatabase.set(true);
				clinicPatients = new ArrayList<Patient>();
			}
		}

		for (Patient hPatient : histoPatients) {
			result.add(hPatient);
			hPatient.setInDatabase(true);

			Iterator<Patient> i = clinicPatients.iterator();
			while (i.hasNext()) {
				Patient cPatient = i.next();
				if (hPatient.getPiz() != null && hPatient.getPiz().equals(cPatient.getPiz())) {
					logger.debug("found in local database " + cPatient.getPerson().getFullNameAndTitle());
					i.remove();
					// only save if update is performed
					if (hPatient.copyIntoObject(cPatient)) {
						try {
							logger.debug("Patient update, saving patient data");
							patientDao.savePatientData(hPatient, "log.patient.search.update");
						} catch (Exception e) {
						}
					}
					break;
				}
			}

		}

		clinicPatients.stream().forEach(p -> p.setInDatabase(false));
		// adding other patients which are not in local database
		result.addAll(clinicPatients);

		return result;

	}

}
