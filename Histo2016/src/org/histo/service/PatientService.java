package org.histo.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.histo.action.handler.GlobalSettings;
import org.histo.adaptors.patientid.PatientIDGenerator;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.dao.GenericDAO;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.util.StreamUtils;
import org.histo.util.TimeUtil;
import org.primefaces.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Service
@Scope("session")
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

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TransactionTemplate transactionTemplate;

	/**
	 * Saves a patient in the database (obtained from pdv).
	 * 
	 * @throws CustomNullPatientExcepetion
	 * @throws CustomExceptionToManyEntries
	 * @throws JSONException
	 */
	public void addPatient(Patient patient, boolean update)
			throws JSONException, CustomExceptionToManyEntries, CustomNullPatientExcepetion {

		// add patient from the clinic-backend, get all data of this
		// patient, piz search is more specific
		if (!patient.getPiz().isEmpty() && !globalSettings.getProgramSettings().isOffline() && update) {
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
			patient.setExternalPatient(patient.getPiz() == null ? true : false);
			genericDAO.savePatientData(patient,
					patient.isExternalPatient() ? "log.patient.extern.new" : "log.patient.search.new");
		} else {
			logger.debug("Patient (\" + patient.getPiz() + \") in database, updating and saving");
			genericDAO.savePatientData(patient, "log.patient.search.update");
		}
	}

	/**
	 * Removes a patient without tasks from local database
	 * 
	 * @param patient
	 */
	public void removePatient(Patient patient) {
		if (patient.getTasks().isEmpty()) {
			genericDAO.deletePatientData(patient, "log.patient.remove", patient);
		}
	}

	/**
	 * Merges to patients. Copies all tasks from one patient to the other. TODO: all
	 * actions in one transaction
	 * 
	 * @param from
	 * @param to
	 */
	public void mergePatient(Patient from, Patient to) {

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {

			public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				List<Task> tasksFrom = from.getTasks();

				if (tasksFrom == null)
					return;

				for (Task task : tasksFrom) {
					task.setParent(to);
					genericDAO.savePatientData(task);
				}
				
//				to.getTasks().addAll(tasksFrom);
//				System.out.println(2);
//				genericDAO.savePatientData(to, "log.patient.merge.addTasks", from.getPatient().toString());
//
//				from.setTasks(new ArrayList<Task>());
//				genericDAO.savePatientData(from, "log.patient.merge.removeTasks");
//				System.out.println("1");
//
//				if (to.getTasks() == null)
//					to.setTasks(new ArrayList<Task>());
			}
		});

	}

	/**
	 * Returns a Patient by the given piz. If localDatabaseOnly is true no pdv
	 * patient will be displayed. (Notice that data of local patient will be syncet
	 * with pdv nevertheless.
	 * 
	 * @param piz
	 * @param localDatabaseOnly
	 * @return
	 * @throws CustomDatabaseInconsistentVersionException
	 * @throws CustomNullPatientExcepetion
	 * @throws CustomExceptionToManyEntries
	 * @throws JSONException
	 */
	public Patient serachForPiz(String piz, boolean localDatabaseOnly)
			throws CustomDatabaseInconsistentVersionException, JSONException, CustomExceptionToManyEntries,
			CustomNullPatientExcepetion {
		// only search if 8 digit are provides
		if (piz != null && piz.matches("^[0-9]{8}$")) {
			Patient patient = patientDao.searchForPatientByPiz(piz);

			// abort search, not found in local database
			if (patient == null && localDatabaseOnly)
				return null;

			if (!globalSettings.getProgramSettings().isOffline()) {
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
			} else
				return patient;
		}
		return null;
	}

	/**
	 * Searches for a range of not completed pizes 6 to 8 digits, searches only in
	 * histo database, pdv database does not support this. Updates found patients
	 * from pdv database.
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

	public List<Patient> searchForPatient(String name, String surname, Date birthday, boolean localDatabaseOnly)
			throws CustomDatabaseInconsistentVersionException, CustomNullPatientExcepetion {
		return searchForPatient(name, surname, birthday, localDatabaseOnly, new AtomicBoolean(false));
	}

	/**
	 * Searches for patients in local and clinic database. Does not auto update all
	 * local patient, does save changes if both clinic patien and local patient was
	 * found
	 * 
	 * @param name
	 * @param surname
	 * @param birthday
	 * @param localDatabaseOnly
	 * @return
	 * @throws CustomExceptionToManyEntries
	 * @throws CustomNullPatientExcepetion
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public List<Patient> searchForPatient(String name, String surname, Date birthday, boolean localDatabaseOnly,
			AtomicBoolean toManyEntriesInClinicDatabase)
			throws CustomNullPatientExcepetion, CustomDatabaseInconsistentVersionException {

		ArrayList<Patient> result = new ArrayList<Patient>();

		List<Patient> histoPatients = patientDao.getPatientsByNameSurnameDate(name, surname, birthday);

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
							genericDAO.savePatientData(hPatient, "log.patient.search.update");
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
