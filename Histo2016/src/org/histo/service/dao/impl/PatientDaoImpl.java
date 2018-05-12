package org.histo.service.dao.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Eye;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.config.exception.HistoDatabaseMergeException;
import org.histo.model.AssociatedContact;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.Signature;
import org.histo.model.StainingPrototype;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.PatientRollbackAble;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.service.dao.PatientDao;
import org.histo.util.HistoUtil;
import org.histo.worklist.search.WorklistSearchExtended;
import org.springframework.stereotype.Repository;

import javassist.tools.reflect.Sample;

@Repository("patientDao")
public class PatientDaoImpl extends HibernateDao<Patient, Long> implements PatientDao {

	/**
	 * Reattaches the patient to the session and initializes tasks and files
	 * 
	 * @param patient
	 * @param init
	 *            loads tasks and files
	 * @return
	 * @throws HistoDatabaseInconsistentVersionException
	 * @throws HistoDatabaseMergeException
	 */
	public Patient initialize(Patient patient, boolean init)
			throws HistoDatabaseInconsistentVersionException, HistoDatabaseMergeException {
		return initialize(patient, init, init);
	}

	/**
	 * Reattaches the patient to the session and initializes tasks and files
	 * 
	 * @param patient
	 *            Patient
	 * @param loadTasks
	 *            loads tasks list
	 * @param loadFiles
	 *            loads file list
	 * @return
	 * @throws HistoDatabaseInconsistentVersionException
	 * @throws HistoDatabaseMergeException
	 */
	public Patient initialize(Patient patient, boolean loadTasks, boolean loadFiles)
			throws HistoDatabaseInconsistentVersionException, HistoDatabaseMergeException {
		reattach(patient);

		if (loadTasks)
			Hibernate.initialize(patient.getTasks());
		if (loadFiles)
			Hibernate.initialize(patient.getAttachedPdfs());

		return patient;
	}

	/**
	 * Gets patient from database. If no patient was found null will be returned.
	 * 
	 * @param id
	 *            ID of Patient
	 * @param init
	 *            loads tasks and files
	 * @return Patient Object
	 */
	public Patient find(Long id, boolean init) {
		return find(id, init, init);
	}

	/**
	 * Gets patient from database. If no patient was found null will be returned.
	 * 
	 * @param id
	 *            ID of Patient
	 * @param loadTasks
	 *            loads tasks list
	 * @param loadFiles
	 *            loads file list
	 * @return Patient Object
	 */
	public Patient find(Long id, boolean loadTasks, boolean loadFiles) {
		CriteriaQuery<Patient> criteria = getBuilder().createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);

		List<Predicate> predicates = new ArrayList<Predicate>();
		predicates.add(getBuilder().equal(root.get("id"), id));

		List<Patient> patients = find(criteria, root, predicates, loadTasks, loadFiles, false);

		return !patients.isEmpty() ? patients.get(0) : null;
	}

	/**
	 * Returns a list of patients containing the given ids
	 * 
	 * @param ids
	 *            List of patients ids.
	 * @return Returns a list of patients containing the given ids
	 */
	public List<Patient> findList(List<Long> ids) {
		return findList(ids, false, false, false);
	}

	/**
	 * Returns a list of patients containing the given ids
	 * 
	 * @param ids
	 *            List of patients ids.
	 * @param loadTasks
	 *            Tasks will be loaded as well.
	 * @param loadFiles
	 *            Files will be loaded as well.
	 * @param irgnoreArchived
	 *            Archived Patiens won't be loaded.
	 * @return Returns a list of patients containing the given ids
	 */
	public List<Patient> findList(List<Long> ids, boolean loadTasks, boolean loadFiles, boolean irgnoreArchived) {
		CriteriaQuery<Patient> criteria = getBuilder().createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);

		List<Predicate> predicates = new ArrayList<Predicate>();
		predicates.add(root.get("id").in(ids));

		return find(criteria, root, predicates, loadTasks, loadFiles, irgnoreArchived);
	}

	/**
	 * Searches for a patient via given piz.Piz does not need 8 digest. The missing
	 * ones will be match by %.
	 * 
	 * @param piz
	 *            Piz of the patient
	 * @return Null if no patient was found
	 */
	public Patient findByPiz(String piz) {
		return findByPiz(piz, false, false, true);
	}

	/**
	 * Searches for a patient via given piz.Piz does not need 8 digest. The missing
	 * ones will be match by %.
	 * 
	 * @param piz
	 *            Piz of the patient.
	 * @param loadTasks
	 *            Tasks will be loaded as well.
	 * @param loadFiles
	 *            Files will be loaded as well.
	 * @param irgnoreArchived
	 *            Archived Patiens won't be loaded.
	 * @return Null if no patient was found
	 */
	public Patient findByPiz(String piz, boolean loadTasks, boolean loadFiles, boolean irgnoreArchived) {
		List<Patient> patients = findListByPiz(piz, loadTasks, loadFiles, irgnoreArchived);
		return !patients.isEmpty() ? patients.get(0) : null;
	}

	/**
	 * List of patients with the given PIZ. Piz does not need 8 digest. The missing
	 * ones will be match by %.
	 * 
	 * @param piz
	 *            Piz of the patient
	 * @return List of patients with the given piz or the given part of the piz
	 */
	public List<Patient> findListByPiz(String piz) {
		return findListByPiz(piz, false, false, true);
	}

	/**
	 * List of Patients with the given PIZ. Piz does not need 8 digest. The missing
	 * ones will be match by %.
	 * 
	 * @param piz
	 *            Piz of the patient.
	 * @param loadTasks
	 *            Tasks will be loaded as well.
	 * @param loadFiles
	 *            Files will be loaded as well.
	 * @param irgnoreArchived
	 *            Archived Patiens won't be loaded.
	 * @return List of patients with the given piz or the given part of the piz
	 */
	public List<Patient> findListByPiz(String piz, boolean loadTasks, boolean loadFiles, boolean irgnoreArchived) {
		CriteriaQuery<Patient> criteria = getBuilder().createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);

		List<Predicate> predicates = new ArrayList<Predicate>();
		predicates.add(getBuilder().like(root.get("piz"), piz + "%"));

		return find(criteria, root, predicates, loadTasks, loadFiles, irgnoreArchived);
	}

	/**
	 * Returns a list of patients which do not have any task associated.
	 * 
	 * @param fromDate
	 *            date of creation
	 * @param toDate
	 *            date of creation
	 * @param loadFiles
	 *            Files will be loaded as well.
	 * @param irgnoreArchived
	 *            Archived Patiens won't be loaded.
	 * @return List of patients
	 */
	public List<Patient> findListWithoutTasks(long fromDate, long toDate, boolean loadFiles, boolean irgnoreArchived) {
		CriteriaQuery<Patient> criteria = getBuilder().createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);

		List<Predicate> predicates = new ArrayList<Predicate>();
		predicates.add(getBuilder().ge(root.get("creationDate"), toDate));
		predicates.add(getBuilder().le(root.get("creationDate"), toDate));
		predicates.add(getBuilder().isEmpty(root.get("tasks")));

		return find(criteria, root, predicates, false, loadFiles, irgnoreArchived);
	}

	/**
	 * Returns a list of task which contains task created within the given timespan
	 * 
	 * @param fromDate
	 *            date of creation
	 * @param toDate
	 *            date of creation
	 * @param loadTasks
	 *            Task will be loaded as well.
	 * @param loadFiles
	 *            Files will be loaded as well.
	 * @param irgnoreArchived
	 *            Archived patients won't be loaded.
	 * @return List of patients
	 */
	public List<Patient> findListByTaskCreation(long fromDate, long toDate, boolean loadTasks, boolean loadFiles,
			boolean irgnoreArchived) {
		CriteriaQuery<Patient> criteria = getBuilder().createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);

		Join<Patient, Task> taskQuery = root.join("tasks", JoinType.LEFT);

		List<Predicate> predicates = new ArrayList<Predicate>();
		predicates.add(getBuilder().ge(taskQuery.get("creationDate"), fromDate));
		predicates.add(getBuilder().le(taskQuery.get("creationDate"), toDate));

		return find(criteria, root, predicates, loadTasks, loadFiles, irgnoreArchived);
	}

	/**
	 * Returns a list of task which contains slides that are completed within the
	 * given timespan
	 * 
	 * @param fromDate
	 *            date of creation
	 * @param toDate
	 *            date of creation
	 * @param loadTasks
	 *            Task will be loaded as well.
	 * @param loadFiles
	 *            Files will be loaded as well.
	 * @param irgnoreArchived
	 *            Archived patients won't be loaded.
	 * @return List of patients
	 */
	public List<Patient> findListBySlideCompleted(long fromDate, long toDate, boolean loadTasks, boolean loadFiles,
			boolean irgnoreArchived) {
		CriteriaQuery<Patient> criteria = getBuilder().createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);

		Join<Patient, Task> taskQuery = root.join("tasks", JoinType.LEFT);

		List<Predicate> predicates = new ArrayList<Predicate>();
		predicates.add(getBuilder().ge(taskQuery.get("stainingCompletionDate"), fromDate));
		predicates.add(getBuilder().le(taskQuery.get("stainingCompletionDate"), toDate));

		return find(criteria, root, predicates, loadTasks, loadFiles, irgnoreArchived);
	}

	/**
	 * Returns a list of task which contains diagnoses that are completed within the
	 * given timespan
	 * 
	 * @param fromDate
	 *            date of creation
	 * @param toDate
	 *            date of creation
	 * @param loadTasks
	 *            Task will be loaded as well.
	 * @param loadFiles
	 *            Files will be loaded as well.
	 * @param irgnoreArchived
	 *            Archived patients won't be loaded.
	 * @return List of patients
	 */
	public List<Patient> findListByDiagnosisCompleted(long fromDate, long toDate, boolean loadTasks, boolean loadFiles,
			boolean irgnoreArchived) {
		CriteriaQuery<Patient> criteria = getBuilder().createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);

		Join<Patient, Task> taskQuery = root.join("tasks", JoinType.LEFT);

		List<Predicate> predicates = new ArrayList<Predicate>();
		predicates.add(getBuilder().ge(taskQuery.get("diagnosisCompletionDate"), fromDate));
		predicates.add(getBuilder().le(taskQuery.get("diagnosisCompletionDate"), toDate));

		return find(criteria, root, predicates, loadTasks, loadFiles, irgnoreArchived);
	}

	/**
	 * Returns a list of task which contains notifications that are completed within
	 * the given timespan
	 * 
	 * @param fromDate
	 *            date of creation
	 * @param toDate
	 *            date of creation
	 * @param loadTasks
	 *            Task will be loaded as well.
	 * @param loadFiles
	 *            Files will be loaded as well.
	 * @param irgnoreArchived
	 *            Archived patients won't be loaded.
	 * @return List of patients
	 */
	public List<Patient> findListByNotificationCompleted(long fromDate, long toDate, boolean loadTasks,
			boolean loadFiles, boolean irgnoreArchived) {
		CriteriaQuery<Patient> criteria = getBuilder().createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);

		Join<Patient, Task> taskQuery = root.join("tasks", JoinType.LEFT);

		List<Predicate> predicates = new ArrayList<Predicate>();
		predicates.add(getBuilder().ge(taskQuery.get("notificationCompletionDate"), fromDate));
		predicates.add(getBuilder().le(taskQuery.get("notificationCompletionDate"), toDate));

		return find(criteria, root, predicates, loadTasks, loadFiles, irgnoreArchived);
	}

	/**
	 * Returns a list of task which are completed within the given timespan
	 * 
	 * @param fromDate
	 *            date of creation
	 * @param toDate
	 *            date of creation
	 * @param loadTasks
	 *            Task will be loaded as well.
	 * @param loadFiles
	 *            Files will be loaded as well.
	 * @param irgnoreArchived
	 *            Archived patients won't be loaded.
	 * @return List of patients
	 */
	public List<Patient> findListByTaskCompleted(long fromDate, long toDate, boolean loadTasks, boolean loadFiles,
			boolean irgnoreArchived) {
		CriteriaQuery<Patient> criteria = getBuilder().createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);

		Join<Patient, Task> taskQuery = root.join("tasks", JoinType.LEFT);

		List<Predicate> predicates = new ArrayList<Predicate>();
		predicates.add(getBuilder().ge(taskQuery.get("notificationCompletionDate"), fromDate));
		predicates.add(getBuilder().le(taskQuery.get("notificationCompletionDate"), toDate));

		return find(criteria, root, predicates, loadTasks, loadFiles, irgnoreArchived);
	}

	/**
	 * Returns a list of patients using name, surname and birthday
	 * 
	 * @param name
	 *            Name of the patient
	 * @param firstName
	 *            Surname of the patient
	 * @param date
	 *            Birthday
	 * @param loadTasks
	 *            Task will be loaded as well.
	 * @param loadFiles
	 *            Files will be loaded as well.
	 * @param irgnoreArchived
	 *            Archived patients won't be loaded.
	 * @return
	 */
	public List<Patient> find(String name, String firstName, Date date, boolean loadTasks, boolean loadFiles,
			boolean irgnoreArchived) {
		CriteriaQuery<Patient> criteria = getBuilder().createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);

		Join<Patient, Person> personQuery = root.join("person", JoinType.LEFT);

		List<Predicate> predicates = new ArrayList<Predicate>();
		if (HistoUtil.isNotNullOrEmpty(name))
			predicates.add(
					getBuilder().like(getBuilder().lower(personQuery.get("lastName")), "%" + name.toLowerCase() + "%"));
		if (HistoUtil.isNotNullOrEmpty(firstName))
			predicates.add(getBuilder().like(getBuilder().lower(personQuery.get("firstName")),
					"%" + firstName.toLowerCase() + "%"));
		if (date != null)
			predicates.add(getBuilder().equal(personQuery.get("birthday"), date));

		return find(criteria, root, predicates, loadTasks, loadFiles, irgnoreArchived);
	}

	/**
	 * Complex Patient search
	 * 
	 * @param worklistSearchExtended
	 * @param loadTasks
	 *            Task will be loaded as well.
	 * @param loadFiles
	 *            Files will be loaded as well.
	 * @param irgnoreArchived
	 *            Archived patients won't be loaded.
	 * @return
	 */
	public List<Patient> findComplex(WorklistSearchExtended worklistSearchExtended, boolean loadTasks,
			boolean loadFiles, boolean irgnoreArchived) {
		CriteriaQuery<Patient> criteria = getBuilder().createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);

		Join<Patient, Task> taskQuery = root.join("tasks", JoinType.LEFT);

		Join<Task, Sample> sampleQuery = taskQuery.join("samples", JoinType.LEFT);
		Join<Sample, Block> blockQuery = sampleQuery.join("blocks", JoinType.LEFT);
		Join<Block, Slide> slideQuery = blockQuery.join("slides", JoinType.LEFT);
		Join<Slide, StainingPrototype> prototypeQuery = slideQuery.join("slidePrototype", JoinType.LEFT);

		Join<Task, DiagnosisRevision> diagnosisRevisionQuery = taskQuery.join("diagnosisRevisions", JoinType.LEFT);
		Join<DiagnosisRevision, Diagnosis> diagnosesQuery = diagnosisRevisionQuery.join("diagnoses", JoinType.LEFT);
		Join<DiagnosisRevision, Signature> signatureOneQuery = diagnosisRevisionQuery.join("signatureOne",
				JoinType.LEFT);
		Join<Signature, Physician> signatureOnePhysicianQuery = signatureOneQuery.join("physician", JoinType.LEFT);
		Join<DiagnosisRevision, Signature> signatureTwoQuery = diagnosisRevisionQuery.join("signatureTwo",
				JoinType.LEFT);
		Join<Signature, Physician> signatureTwoPhysicianQuery = signatureTwoQuery.join("physician", JoinType.LEFT);

		Join<AssociatedContact, Task> contactQuery = taskQuery.join("contacts", JoinType.LEFT);
		Join<Person, AssociatedContact> personContactQuery = contactQuery.join("person", JoinType.LEFT);

		List<Predicate> predicates = new ArrayList<Predicate>();
		// searching for material
		if (HistoUtil.isNotNullOrEmpty(worklistSearchExtended.getMaterial())) {
			predicates.add(getBuilder().like(getBuilder().lower(sampleQuery.get("material")),
					"%" + worklistSearchExtended.getMaterial().toLowerCase() + "%"));

			logger.debug("Selecting material " + worklistSearchExtended.getMaterial());
		}

		// getting surgeon
		if (HistoUtil.isNotNullOrEmpty(worklistSearchExtended.getSurgeons())) {
			Expression<Long> exp = personContactQuery.get("id");
			predicates.add(getBuilder().and(
					exp.in(Arrays.asList(worklistSearchExtended.getSurgeons()).stream().map(p -> p.getPerson().getId())
							.collect(Collectors.toList())),
					getBuilder().equal(contactQuery.get("role"), ContactRole.SURGEON)));
			logger.debug("Selecting surgeon");
		}

		// getting signature
		if (HistoUtil.isNotNullOrEmpty(worklistSearchExtended.getSignature())) {
			Expression<Long> expphysicianOne = signatureOnePhysicianQuery.get("id");
			Predicate physicianOne = expphysicianOne.in(Arrays.asList(worklistSearchExtended.getSignature()).stream()
					.map(p -> p.getId()).collect(Collectors.toList()));

			Expression<Long> expphysicianTwo = signatureTwoPhysicianQuery.get("id");
			Predicate physicianTwo = expphysicianTwo.in(Arrays.asList(worklistSearchExtended.getSignature()).stream()
					.map(p -> p.getId()).collect(Collectors.toList()));

			predicates.add(getBuilder().or(physicianOne, physicianTwo));

			logger.debug("Selecting signature");
		}

		// getting history
		if (HistoUtil.isNotNullOrEmpty(worklistSearchExtended.getCaseHistory())) {
			predicates.add(getBuilder().like(getBuilder().lower(taskQuery.get("caseHistory")),
					"%" + worklistSearchExtended.getCaseHistory().toLowerCase() + "%"));

			logger.debug("Selecting history");
		}

		// getting diagnosis text
		if (HistoUtil.isNotNullOrEmpty(worklistSearchExtended.getDiagnosisText())) {
			predicates.add(getBuilder().like(getBuilder().lower(diagnosisRevisionQuery.get("text")),
					"%" + worklistSearchExtended.getDiagnosisText().toLowerCase() + "%"));

			logger.debug("Selecting diagnosis text");
		}

		// getting diagnosis
		if (HistoUtil.isNotNullOrEmpty(worklistSearchExtended.getDiagnosis())) {
			predicates.add(getBuilder().like(getBuilder().lower(diagnosesQuery.get("diagnosis")),
					"%" + worklistSearchExtended.getDiagnosis().toLowerCase() + "%"));

			logger.debug("Selecting diagnosis");
		}

		// checking malign, 0 = not selected, 1 = true, 2 = false
		if (!worklistSearchExtended.getMalign().equals("0")) {
			predicates.add(getBuilder().equal(diagnosesQuery.get("malign"),
					worklistSearchExtended.getMalign().equals("1") ? true : false));

			logger.debug("Selecting malign");
		}

		// getting eye
		if (worklistSearchExtended.getEye() != Eye.UNKNOWN) {
			predicates.add(getBuilder().equal(taskQuery.get("eye"), worklistSearchExtended.getEye()));

			logger.debug("Selecting eye");
		}

		// getting ward
		if (HistoUtil.isNotNullOrEmpty(worklistSearchExtended.getWard())) {
			predicates.add(getBuilder().like(getBuilder().lower(diagnosesQuery.get("ward")),
					"%" + worklistSearchExtended.getWard().toLowerCase() + "%"));

			logger.debug("Selecting ward");
		}

		if (worklistSearchExtended.getStainings() != null && !worklistSearchExtended.getStainings().isEmpty()) {
			Expression<Long> prototypeID = prototypeQuery.get("id");
			Predicate stainings = prototypeID.in(
					worklistSearchExtended.getStainings().stream().map(p -> p.getId()).collect(Collectors.toList()));

			predicates.add(stainings);
		}

		return find(criteria, root, predicates, loadTasks, loadFiles, irgnoreArchived);
	};

	/**
	 * Generic method for search patients by criteria
	 * 
	 * @param criteria
	 *            List of criteria for the entity, criteria are joined by and
	 * @param root
	 *            Root of the entity
	 * @param and
	 *            Predicate to search for
	 * @param loadTasks
	 *            Task will be loaded as well.
	 * @param loadFiles
	 *            Files will be loaded as well.
	 * @param irgnoreArchived
	 *            Archived patients won't be loaded.
	 * @return List of patients
	 */
	public List<Patient> find(CriteriaQuery<Patient> criteria, Root<Patient> root, List<Predicate> predicates,
			boolean loadTasks, boolean loadFiles, boolean irgnoreArchived) {
		criteria.select(root);

		if (loadTasks)
			root.fetch("tasks", JoinType.LEFT);

		if (loadFiles)
			root.fetch("attachedPdfs", JoinType.LEFT);

		if (irgnoreArchived)
			predicates.add(getBuilder().equal(root.get("archived"), false));

		criteria.where(getBuilder().and(predicates.toArray(new Predicate[predicates.size()])));
		criteria.distinct(true);

		return currentSession().createQuery(criteria).getResultList();
	}

	/**
	 * Saves patient associated data, first wildcard for logging is replaced by the
	 * patient log path
	 */
	public <C extends PatientRollbackAble<?>> void savePatientData(C object, String resourcesKey,
			Object... resourcesKeyInsert) {
		savePatientData(object, object, resourcesKey, resourcesKeyInsert);
	}

	/**
	 * Saves patient associated data, first wildcard for logging is replaced by the
	 * patient log path
	 */
	public void savePatientData(Object object, PatientRollbackAble<?> hasPatient, String resourcesKey,
			Object... resourcesKeyInsert) {
		Object[] keyArr = new Object[resourcesKeyInsert.length + 1];
		keyArr[0] = hasPatient.getLogPath();
		System.arraycopy(resourcesKeyInsert, 0, keyArr, 1, resourcesKeyInsert.length);
		save(object, resourcesKey, keyArr, hasPatient.getPatient());
	}

	/**
	 * Deletes patient associated data, first wildcard for logging is replaced by
	 * the patient log path
	 */
	public <C extends PatientRollbackAble<?>> void deletePatientData(C object, String resourcesKey,
			Object... resourcesKeyInsert) {
		deletePatientData(object, object, resourcesKey, resourcesKeyInsert);
	}

	/**
	 * Deletes patient associated data, first wildcard for logging is replaced by
	 * the patient log path
	 */
	public void deletePatientData(Object object, PatientRollbackAble<?> hasPatient, String resourcesKey,
			Object... resourcesKeyInsert) {
		Object[] keyArr = new Object[resourcesKeyInsert.length + 1];
		keyArr[0] = hasPatient.getLogPath();
		System.arraycopy(resourcesKeyInsert, 0, keyArr, 1, resourcesKeyInsert.length);
		delete(object, resourcesKey, keyArr, hasPatient.getPatient());
	}
}
