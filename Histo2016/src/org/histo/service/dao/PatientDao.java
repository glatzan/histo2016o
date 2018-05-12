package org.histo.service.dao;

import java.util.Date;
import java.util.List;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.model.interfaces.PatientRollbackAble;
import org.histo.model.patient.Patient;
import org.histo.worklist.search.WorklistSearchExtended;

public interface PatientDao extends GenericDao<Patient, Long> {

	public Patient initialize(Patient patient, boolean init);

	public Patient initialize(Patient patient, boolean loadTasks, boolean loadFiles);

	public Patient find(Long id, boolean init);

	public Patient find(Long id, boolean loadTasks, boolean loadFiles);

	public List<Patient> findList(List<Long> ids);

	public List<Patient> findList(List<Long> ids, boolean loadTasks, boolean loadFiles, boolean irgnoreArchived);

	public Patient findByPiz(String piz);

	public Patient findByPiz(String piz, boolean loadTasks, boolean loadFiles, boolean irgnoreArchived);

	public List<Patient> findListByPiz(String piz);

	public List<Patient> findListByPiz(String piz, boolean loadTasks, boolean loadFiles, boolean irgnoreArchived);

	public List<Patient> findListWithoutTasks(long fromDate, long toDate, boolean loadFiles, boolean irgnoreArchived);

	public List<Patient> findListByTaskCreation(long fromDate, long toDate, boolean loadTasks, boolean loadFiles,
			boolean irgnoreArchived);

	public List<Patient> findListBySlideCompleted(long fromDate, long toDate, boolean loadTasks, boolean loadFiles,
			boolean irgnoreArchived);

	public List<Patient> findListByDiagnosisCompleted(long fromDate, long toDate, boolean loadTasks, boolean loadFiles,
			boolean irgnoreArchived);

	public List<Patient> findListByNotificationCompleted(long fromDate, long toDate, boolean loadTasks,
			boolean loadFiles, boolean irgnoreArchived);

	public List<Patient> findListByTaskCompleted(long fromDate, long toDate, boolean loadTasks, boolean loadFiles,
			boolean irgnoreArchived);

	public List<Patient> find(String name, String firstName, Date date, boolean loadTasks, boolean loadFiles,
			boolean irgnoreArchived);

	public List<Patient> findComplex(WorklistSearchExtended worklistSearchExtended, boolean loadTasks,
			boolean loadFiles, boolean irgnoreArchived);

	public List<Patient> find(CriteriaQuery<Patient> criteria, Root<Patient> root, List<Predicate> predicates,
			boolean loadTasks, boolean loadFiles, boolean irgnoreArchived);

	public <C extends PatientRollbackAble<?>> void savePatientData(C object, String resourcesKey,
			Object... resourcesKeyInsert);

	public void savePatientData(Object object, PatientRollbackAble<?> hasPatient, String resourcesKey,
			Object... resourcesKeyInsert);

	public <C extends PatientRollbackAble<?>> void deletePatientData(C object, String resourcesKey,
			Object... resourcesKeyInsert);

	public void deletePatientData(Object object, PatientRollbackAble<?> hasPatient, String resourcesKey,
			Object... resourcesKeyInsert);
}
