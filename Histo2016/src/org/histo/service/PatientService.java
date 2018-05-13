package org.histo.service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.primefaces.json.JSONException;

public interface PatientService {
	/**
	 * Adding a patient to the database, if not new save patient will be saved as
	 * well. Compares and updates patient data with the clinic backed.
	 * 
	 * @param patient
	 * @param update
	 * @throws JSONException
	 * @throws CustomExceptionToManyEntries
	 * @throws CustomNullPatientExcepetion
	 */
	void addPatient(Patient patient) throws JSONException, CustomExceptionToManyEntries, CustomNullPatientExcepetion;

	/**
	 * Adding a patient to the database, if not new save patient will be saved as
	 * well. While update is true a data comparison with the pdv will be
	 * initialized.
	 * 
	 * @param patient
	 * @param update
	 * @throws JSONException
	 * @throws CustomExceptionToManyEntries
	 * @throws CustomNullPatientExcepetion
	 */
	void addPatient(Patient patient, boolean update)
			throws JSONException, CustomExceptionToManyEntries, CustomNullPatientExcepetion;

	/**
	 * Removes a patient without tasks from local database. TODO: Remove logs
	 * 
	 * @param patient
	 */
	void removePatient(Patient patient);

	/**
	 * Archives a patient without tasks from local database
	 * 
	 * @param patient
	 */
	void archivePatient(Patient patient);

	/**
	 * Merges two patients. Copies all tasks from one patient to the other.
	 * 
	 * @param from
	 * @param to
	 */
	void mergePatient(Patient from, Patient to);

	/**
	 * Merges two patients. Copies all tasks from one patient to the other. Taks a
	 * lists of tasks to merge.
	 * 
	 * @param from
	 * @param to
	 * @param tasksToMerge
	 */
	void mergePatient(Patient from, Patient to, List<Task> tasksToMerge);

	/**
	 * Returns a Patient by the given piz. If localDatabaseOnly is true no pdv
	 * patient will be displayed. (Notice that data of local patient will be synced
	 * with pdv nevertheless.
	 * 
	 * @param piz
	 * @param localDatabaseOnly
	 * @return
	 * @throws HistoDatabaseInconsistentVersionException
	 * @throws CustomNullPatientExcepetion
	 * @throws CustomExceptionToManyEntries
	 * @throws JSONException
	 */
	Patient findPatientByPiz(String piz, boolean localDatabaseOnly) throws HistoDatabaseInconsistentVersionException,
			JSONException, CustomExceptionToManyEntries, CustomNullPatientExcepetion;

	/**
	 * Searches for a range of not completed pizes 6 to 8 digits, searches only in
	 * histo database, pdv database does not support this. Updates found patients
	 * from pdv database.
	 * 
	 * @param piz
	 * @return
	 * @throws JSONException
	 * @throws CustomExceptionToManyEntries
	 * @throws CustomNullPatientExcepetion
	 * @throws HistoDatabaseInconsistentVersionException
	 */
	List<Patient> listByPiz(String piz) throws JSONException, CustomExceptionToManyEntries,
			CustomNullPatientExcepetion, HistoDatabaseInconsistentVersionException;

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
	 * @throws HistoDatabaseInconsistentVersionException
	 * @throws CustomNullPatientExcepetion
	 */
	List<Patient> list(String name, String surname, Date birthday, boolean localDatabaseOnly)
			throws HistoDatabaseInconsistentVersionException, CustomNullPatientExcepetion;

	/**
	 * Searches for patients in local and clinic database. Does not auto update all
	 * local patient, does save changes if both clinic patien and local patient was
	 * found
	 * 
	 * @param name
	 * @param surname
	 * @param birthday
	 * @param localDatabaseOnly
	 * @param toManyEntriesInClinicDatabase
	 * @return
	 * @throws CustomNullPatientExcepetion
	 * @throws HistoDatabaseInconsistentVersionException
	 */
	List<Patient> list(String name, String surname, Date birthday, boolean localDatabaseOnly,
			AtomicBoolean toManyEntriesInClinicDatabase)
			throws CustomNullPatientExcepetion, HistoDatabaseInconsistentVersionException;

}
