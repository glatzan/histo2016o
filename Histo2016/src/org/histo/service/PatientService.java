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
	public void addPatient(Patient patient)
			throws JSONException, CustomExceptionToManyEntries, CustomNullPatientExcepetion;

	public void addPatient(Patient patient, boolean update)
			throws JSONException, CustomExceptionToManyEntries, CustomNullPatientExcepetion;

	public void removePatient(Patient patient);

	public void archivePatient(Patient patient);

	public void mergePatient(Patient from, Patient to);

	public void mergePatient(Patient from, Patient to, List<Task> tasksToMerge);

	public Patient findPatientByPiz(String piz, boolean localDatabaseOnly)
			throws HistoDatabaseInconsistentVersionException, JSONException, CustomExceptionToManyEntries,
			CustomNullPatientExcepetion;

	public List<Patient> findPatientListByPiz(String piz) throws JSONException, CustomExceptionToManyEntries,
			CustomNullPatientExcepetion, HistoDatabaseInconsistentVersionException;

	public List<Patient> findPatient(String name, String surname, Date birthday, boolean localDatabaseOnly)
			throws HistoDatabaseInconsistentVersionException, CustomNullPatientExcepetion;

	public List<Patient> findPatient(String name, String surname, Date birthday, boolean localDatabaseOnly,
			AtomicBoolean toManyEntriesInClinicDatabase)
			throws CustomNullPatientExcepetion, HistoDatabaseInconsistentVersionException;
}
