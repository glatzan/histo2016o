package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.histo.action.dialog.worklist.WorklistSearchDialog.ExtendedSearchData;
import org.histo.config.enums.Eye;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.Person;
import org.histo.model.patient.Block;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
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
	 * Returns a list of patients with the given piz. At least 6 numbers of the
	 * piz are needed.
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
	 * Returns a patient object for a specific piz. The piz has to be 8
	 * characters long.
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
	 * Returns a list of patients added to the worklist between the two given
	 * dates.
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
	 * Returns a list of patients which had a sample created between the two
	 * given dates
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
	 * Returns a list of patients for that the staining had been completed
	 * within the time period. Don't start with zero.
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
	 * Returns a list of patients for that the diagnosis had been completed
	 * within the time period. Don't start with zero.
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
	 * Returns a lists with patients of whom the tasks had been finalized
	 * inbetween the two given dates.
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

	public List<Patient> getPatientByCriteria(ExtendedSearchData extendedSearchData) {
		logger.debug("test");

		DetachedCriteria query = DetachedCriteria.forClass(Task.class, "task");

		query.createAlias("task.parent", "patient");
		query.createAlias("patient.person", "person");
		query.createAlias("task.samples", "samples");
		query.createAlias("task.diagnosisRevisions", "diagnosisRevisions");
		query.createAlias("diagnosisRevisions.diagnoses", "diagnoses");

		if (extendedSearchData.getName() != null && !extendedSearchData.getName().isEmpty()) {
			query.add(Restrictions.ilike("person.lastname", extendedSearchData.getName(), MatchMode.ANYWHERE));
			logger.debug("search for name: " + extendedSearchData.getName());
		}

		if (extendedSearchData.getSurename() != null && !extendedSearchData.getSurename().isEmpty()) {
			query.add(Restrictions.ilike("person.firstname", extendedSearchData.getSurename(), MatchMode.ANYWHERE));
			logger.debug("search for surename: " + extendedSearchData.getSurename());
		}

		if (extendedSearchData.getBirthday() != null) {
			query.add(Restrictions.eq("person.birthday", extendedSearchData.getBirthday()));
			logger.debug("search for birthday: " + extendedSearchData.getBirthday());
		}

		if (extendedSearchData.getGender() != null && extendedSearchData.getGender() != Person.Gender.UNKNOWN) {
			query.add(Restrictions.eq("person.gender", extendedSearchData.getGender()));
			logger.debug("search for gender: " + extendedSearchData.getGender());
		}

		if (extendedSearchData.getMaterial() != null && !extendedSearchData.getMaterial().isEmpty()) {
			query.add(Restrictions.ilike("samples.material", extendedSearchData.getMaterial(), MatchMode.ANYWHERE));

			logger.debug("search for material: " + extendedSearchData.getMaterial());
		}

		if (extendedSearchData.getCaseHistory() != null && !extendedSearchData.getCaseHistory().isEmpty()) {
			query.add(Restrictions.ilike("task.caseHistory", extendedSearchData.getCaseHistory(), MatchMode.ANYWHERE));

			logger.debug("search for case history: " + extendedSearchData.getCaseHistory());
		}

		if (extendedSearchData.getEye() != null && extendedSearchData.getEye() != Eye.UNKNOWN) {
			query.add(Restrictions.eq("task.eye", extendedSearchData.getEye()));
			logger.debug("search for eye: " + extendedSearchData.getEye());
		}

		if (extendedSearchData.getDiagnosis() != null && !extendedSearchData.getDiagnosis().isEmpty()) {
			query.add(Restrictions.ilike("diagnoses.diagnosis", extendedSearchData.getDiagnosis(), MatchMode.ANYWHERE));
			logger.debug("search for diagnosis: " + extendedSearchData.getDiagnosis());
		}

		if (extendedSearchData.getMalign() != null && !extendedSearchData.getMalign().equals("0"))

		{
			query.add(Restrictions.eq("diagnoses.malign", extendedSearchData.getMalign().equals("1")));
			logger.debug("search for malign: " + extendedSearchData.getMalign().equals("1"));
		}

		query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

		List<Task> tasks = query.getExecutableCriteria(getSession()).list();

		List<Patient> result = new ArrayList<Patient>(tasks.size());

		for (Task task : tasks) {
			try {
				task.setActive(true);
				initilaizeTasksofPatient(task.getPatient());
				result.add(task.getPatient());
			} catch (CustomDatabaseInconsistentVersionException e) {
				e.printStackTrace();
			}
		}

		return result;
	}

}
