package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Eye;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.AssociatedContact;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.Signature;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.util.HistoUtil;
import org.histo.worklist.search.WorklistSearchExtended;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javassist.tools.reflect.Sample;

@Component
@Transactional
@Scope(value = "session")
public class PatientDao extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = 2730948440994018437L;

	private static Logger logger = Logger.getLogger("org.histo");

	public Patient initilaizeTasksofPatient(Patient patient) throws CustomDatabaseInconsistentVersionException {
		reattach(patient);
		Hibernate.initialize(patient.getTasks());
		return patient;
	}

	public Patient initializePatient(Patient patient, boolean initialize)
			throws CustomDatabaseInconsistentVersionException {
		reattach(patient);

		if (initialize) {
			Hibernate.initialize(patient.getTasks());
			Hibernate.initialize(patient.getAttachedPdfs());
		}

		return patient;
	}

	public Patient getPatient(long id, boolean initialize) {

		Patient patient = get(Patient.class, id);
		getSession().refresh(patient);

		if (initialize) {
			Hibernate.initialize(patient.getTasks());
			Hibernate.initialize(patient.getAttachedPdfs());
		}
		return patient;
	}

	/**
	 * Returns a list of patients with the given piz. At least 6 numbers of the piz
	 * are needed.
	 * 
	 * @param piz
	 * @return
	 */
	public List<Patient> searchForPatientsByPiz(String piz) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Patient> criteria = qb.createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);
		criteria.select(root);

		String regex = "";
		if (piz.length() != 8) {
			regex = "[0-9]{" + (8 - piz.length()) + "}";
		}

		criteria.where(qb.like(root.get("piz"), piz + regex));

		criteria.distinct(true);

		List<Patient> patients = getSession().createQuery(criteria).getResultList();

		return patients;
	}

	/**
	 * Returns a patient object for a specific piz. The piz has to be 8 characters
	 * long.
	 * 
	 * @param piz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Patient searchForPatientByPiz(String piz) {
		if (piz.length() != 8)
			return null;

		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.add(Restrictions.eq("piz", piz));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		List<Patient> result = query.getExecutableCriteria(getSession()).list();

		if (result != null && result.size() == 1)
			return result.get(0);

		return null;
	}

	/**
	 * Returns a list of patients with matching pizes.
	 * 
	 * @param piz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> searchForPatientPizList(List<String> piz) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");
		query.add(Restrictions.in("piz", piz));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Returns a list of patients with matching ides.
	 * 
	 * @param piz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> searchForPatientIDsList(List<Long> ids) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");
		query.add(Restrictions.in("id", ids));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}

	public List<Patient> getPatientWithoutTasks(long fromDate, long toDate, boolean initTasks) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Patient> criteria = qb.createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);
		criteria.select(root);

		if (initTasks)
			root.fetch("tasks", JoinType.LEFT);

		Predicate and = qb.and(qb.ge(root.get("creationDate"), fromDate), qb.le(root.get("creationDate"), toDate),
				qb.isEmpty(root.get("tasks")));

		criteria.where(and);

		criteria.distinct(true);

		List<Patient> patients = getSession().createQuery(criteria).getResultList();

		return patients;
	}

	/**
	 * Returns a list of patients added to the worklist between the two given dates.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public List<Patient> getPatientByAddDateToDatabaseDate(long fromDate, long toDate, boolean initTasks) {

		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Patient> criteria = qb.createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);
		criteria.select(root);

		if (initTasks)
			root.fetch("tasks", JoinType.LEFT);

		Predicate and = qb.and(qb.ge(root.get("creationDate"), fromDate), qb.le(root.get("creationDate"), toDate));

		criteria.where(and);

		criteria.distinct(true);

		List<Patient> patients = getSession().createQuery(criteria).getResultList();

		return patients;
	}

	/**
	 * Returns a list of patients which had a sample created between the two given
	 * dates
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public List<Patient> getPatientByTaskCreationDate(long fromDate, long toDate, boolean initTasks) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Patient> criteria = qb.createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);
		criteria.select(root);

		if (initTasks)
			root.fetch("tasks", JoinType.LEFT);

		Join<Patient, Task> taskQuery = root.join("tasks", JoinType.LEFT);

		Predicate and = qb.and(qb.ge(taskQuery.get("creationDate"), fromDate),
				qb.le(taskQuery.get("creationDate"), toDate));

		criteria.where(and);

		criteria.distinct(true);

		List<Patient> patients = getSession().createQuery(criteria).getResultList();

		return patients;
	}

	/**
	 * Returns a list of patients for that the staining had been completed within
	 * the time period. Don't start with zero.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public List<Patient> getPatientByStainingsCompletionDate(long fromDate, long toDate, boolean initTasks) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Patient> criteria = qb.createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);
		criteria.select(root);

		if (initTasks)
			root.fetch("tasks", JoinType.LEFT);

		Join<Patient, Task> taskQuery = root.join("tasks", JoinType.LEFT);

		Predicate and = qb.and(qb.ge(taskQuery.get("stainingCompletionDate"), fromDate),
				qb.le(taskQuery.get("stainingCompletionDate"), toDate));

		criteria.where(and);

		criteria.distinct(true);

		List<Patient> patients = getSession().createQuery(criteria).getResultList();

		return patients;
	}

	/**
	 * Returns a list of patients for that the diagnosis had been completed within
	 * the time period. Don't start with zero.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public List<Patient> getPatientByDiagnosisCompletionDate(long fromDate, long toDate, boolean initTasks) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Patient> criteria = qb.createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);
		criteria.select(root);

		if (initTasks)
			root.fetch("tasks", JoinType.LEFT);

		Join<Patient, Task> taskQuery = root.join("tasks", JoinType.LEFT);

		Predicate and = qb.and(qb.ge(taskQuery.get("diagnosisCompletionDate"), fromDate),
				qb.le(taskQuery.get("diagnosisCompletionDate"), toDate));

		criteria.where(and);

		criteria.distinct(true);

		List<Patient> patients = getSession().createQuery(criteria).getResultList();

		return patients;
	}

	/**
	 * Returns a list of patients for that the notification had been completed
	 * within the time period. Don't start with zero.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public List<Patient> getPatientByNotificationCompletionDate(long fromDate, long toDate, boolean initTasks) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Patient> criteria = qb.createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);
		criteria.select(root);

		if (initTasks)
			root.fetch("tasks", JoinType.LEFT);

		Join<Patient, Task> taskQuery = root.join("tasks", JoinType.LEFT);

		Predicate and = qb.and(qb.ge(taskQuery.get("notificationCompletionDate"), fromDate),
				qb.le(taskQuery.get("notificationCompletionDate"), toDate));

		criteria.where(and);

		criteria.distinct(true);

		List<Patient> patients = getSession().createQuery(criteria).getResultList();

		return patients;
	}

	/**
	 * Returns a lists with patients of whom the tasks had been finalized inbetween
	 * the two given dates.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @param initTasks
	 * @return
	 */
	public List<Patient> getPatientByFinalizedTaskDate(long fromDate, long toDate, boolean initTasks) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Patient> criteria = qb.createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);
		criteria.select(root);

		if (initTasks)
			root.fetch("tasks", JoinType.LEFT);

		Join<Patient, Task> taskQuery = root.join("tasks", JoinType.LEFT);

		Predicate and = qb.and(qb.ge(taskQuery.get("notificationCompletionDate"), fromDate),
				qb.le(taskQuery.get("notificationCompletionDate"), toDate), qb.equal(taskQuery.get("finalized"), true));

		criteria.where(and);

		criteria.distinct(true);

		List<Patient> patients = getSession().createQuery(criteria).getResultList();

		return patients;
	}

	public List<Patient> getPatientsByNameSurnameDateExcludePiz(String name, String firstName, Date date,
			List<String> pizesToExclude) {

		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.createAlias("patient.person", "_person");

		if (name != null && !name.isEmpty())
			query.add(Restrictions.ilike("_person.lastName", name, MatchMode.ANYWHERE));
		if (firstName != null && !firstName.isEmpty())
			query.add(Restrictions.ilike("_person.firstName", firstName, MatchMode.ANYWHERE));
		if (date != null)
			query.add(Restrictions.eq("_person.birthday", date));
		if (pizesToExclude != null && !pizesToExclude.isEmpty())
			query.add(Restrictions.not(Restrictions.in("piz", pizesToExclude.toArray())));

		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		List<Patient> result = query.getExecutableCriteria(getSession()).list();

		return result != null ? result : new ArrayList<>();
	}

	public List<Patient> getPatientsByNameSurnameDate(String name, String firstName, Date date) {
		return getPatientsByNameSurnameDateExcludePiz(name, firstName, date, null);
	}

	/**
	 * Searches for an taskID and returns the patient whom the task belongs to
	 * 
	 * @param taskID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Patient getPatientByTaskID(String taskID) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.createAlias("patient.tasks", "_tasks");
		query.add(Restrictions.eq("_tasks.taskID", taskID));

		List<Patient> result = query.getExecutableCriteria(getSession()).list();

		if (result.size() == 1)
			return result.get(0);

		return null;
	}

	public List<Patient> getPatientByCriteria(WorklistSearchExtended worklistSearchExtended, boolean initTasks) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Patient> criteria = qb.createQuery(Patient.class);
		Root<Patient> root = criteria.from(Patient.class);
		criteria.select(root);

		if (initTasks)
			root.fetch("tasks", JoinType.LEFT);

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
			predicates.add(qb.like(qb.lower(sampleQuery.get("material")),
					"%" + worklistSearchExtended.getMaterial().toLowerCase() + "%"));

			logger.debug("Selecting material " + worklistSearchExtended.getMaterial());
		}

		// getting surgeon
		if (HistoUtil.isNotNullOrEmpty(worklistSearchExtended.getSurgeons())) {
			Expression<Long> exp = personContactQuery.get("id");
			predicates
					.add(qb.and(
							exp.in(Arrays.asList(worklistSearchExtended.getSurgeons()).stream()
									.map(p -> p.getPerson().getId()).collect(Collectors.toList())),
							qb.equal(contactQuery.get("role"), ContactRole.SURGEON)));
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

			predicates.add(qb.or(physicianOne, physicianTwo));

			logger.debug("Selecting signature");
		}

		// getting history
		if (HistoUtil.isNotNullOrEmpty(worklistSearchExtended.getCaseHistory())) {
			predicates.add(qb.like(qb.lower(taskQuery.get("caseHistory")),
					"%" + worklistSearchExtended.getCaseHistory().toLowerCase() + "%"));

			logger.debug("Selecting history");
		}

		// getting diagnosis text
		if (HistoUtil.isNotNullOrEmpty(worklistSearchExtended.getDiagnosisText())) {
			predicates.add(qb.like(qb.lower(diagnosisRevisionQuery.get("text")),
					"%" + worklistSearchExtended.getDiagnosisText().toLowerCase() + "%"));

			logger.debug("Selecting diagnosis text");
		}

		// getting diagnosis
		if (HistoUtil.isNotNullOrEmpty(worklistSearchExtended.getDiagnosis())) {
			predicates.add(qb.like(qb.lower(diagnosesQuery.get("diagnosis")),
					"%" + worklistSearchExtended.getDiagnosis().toLowerCase() + "%"));

			logger.debug("Selecting diagnosis");
		}

		// checking malign, 0 = not selected, 1 = true, 2 = false
		if (!worklistSearchExtended.getMalign().equals("0")) {
			predicates.add(qb.equal(diagnosesQuery.get("malign"),
					worklistSearchExtended.getMalign().equals("1") ? true : false));

			logger.debug("Selecting malign");
		}

		// getting eye
		if (worklistSearchExtended.getEye() != Eye.UNKNOWN) {
			predicates.add(qb.equal(taskQuery.get("eye"), worklistSearchExtended.getEye()));

			logger.debug("Selecting eye");
		}

		// getting ward
		if (HistoUtil.isNotNullOrEmpty(worklistSearchExtended.getWard())) {
			predicates.add(qb.like(qb.lower(diagnosesQuery.get("ward")),
					"%" + worklistSearchExtended.getWard().toLowerCase() + "%"));

			logger.debug("Selecting ward");
		}

		if (worklistSearchExtended.getStainings() != null && !worklistSearchExtended.getStainings().isEmpty()) {
			Expression<Long> prototypeID = prototypeQuery.get("id");
			Predicate stainings = prototypeID.in(
					worklistSearchExtended.getStainings().stream().map(p -> p.getId()).collect(Collectors.toList()));

			predicates.add(stainings);
		}

		criteria.where(qb.and(predicates.toArray(new Predicate[predicates.size()])));

		criteria.distinct(true);

		List<Patient> patients = getSession().createQuery(criteria).getResultList();

		return patients;
	}
}
