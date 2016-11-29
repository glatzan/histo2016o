package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.WorklistSearchFilter;
import org.histo.model.Person;
import org.histo.model.patient.Patient;
import org.histo.util.TimeUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class PatientDao extends AbstractDAO implements Serializable {

	private static Logger logger = Logger.getLogger("org.histo");

	public void getAllPaitent() {
		Person p = new Person();
		System.out.println(getSession().save(p));
	}

	public void getPatitentByDate(long from, long to) {
		Criteria c = getSession().createCriteria(Person.class, "pat");
		c.createAlias("pat.tasks", "tasks"); // inner join by default
		c.add(Restrictions.gt("tasks.taskOccoured", from)).add(Restrictions.lt("tasks.taskOccoured", to)).list();
	}

	/**
	 * Returns a list of useres with the given piz. At least 6 numbers of the
	 * piz are needed.
	 * 
	 * @param piz
	 * @return
	 */
	public List<Patient> searchForPatientsPiz(String piz) {
		Criteria c = getSession().createCriteria(Patient.class);
		String regex = "";
		if (piz.length() != 8) {
			regex = "[0-9]{" + (8 - piz.length()) + "}";
		}
		c.add(Restrictions.like("piz", piz + regex));
		return c.list();
	}

	/**
	 * Returns a patient object for a specific piz. The piz has to be 8
	 * characters long.
	 * 
	 * @param piz
	 * @return
	 */
	public Patient searchForPatientPiz(String piz) {
		if (piz.length() != 8)
			return null;

		Criteria c = getSession().createCriteria(Patient.class);
		c.add(Restrictions.eq("piz", piz));
		List<Patient> result = c.list();
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
	public List<Patient> searchForPatientPizes(List<String> piz) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");
		query.add(Restrictions.in("piz", piz));
		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Returns a list without of patients without tasks between the two given
	 * dates.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> getPatientWithoutTasks(long fromDate, long toDate) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");
		query.add(Restrictions.ge("patient.creationDate", fromDate))
				.add(Restrictions.le("patient.creationDate", toDate));
		query.add(Restrictions.isEmpty("patient.tasks"));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Returns a list of patients added to the worklist between the two given
	 * dates.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> getPatientByAddDateToWorklist(long fromDate, long toDate) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");
		query.add(Restrictions.ge("patient.creationDate", fromDate))
				.add(Restrictions.le("patient.creationDate", toDate));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Returns a list of patients which had a sample created between the two
	 * given dates
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> getPatientBySampleCreationDateBetweenDates(long fromDate, long toDate) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");
		query.createAlias("patient.tasks", "_tasks");
		query.createAlias("_tasks.samples", "_samples");
		query.add(Restrictions.ge("_samples.creationDate", fromDate))
				.add(Restrictions.le("_samples.creationDate", toDate));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Returns a list of patients which had the staining procedure completed
	 * between the two given dates
	 * 
	 * @param fromDate
	 * @param toDate
	 * @param completed
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> getPatientByStainingsBetweenDates(long fromDate, long toDate, boolean completed) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.createAlias("patient.tasks", "_tasks");
		query.add(Restrictions.ge("_tasks.creationDate", fromDate)).add(Restrictions.le("_tasks.creationDate", toDate));
		query.add(Restrictions.eq("_tasks.stainingCompleted", completed));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Returns a list of patients which had the diagnosis completed between the
	 * two given dates.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @param completed
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> getPatientByDiagnosBetweenDates(long fromDate, long toDate, boolean completed) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.createAlias("patient.tasks", "_tasks");
		query.add(Restrictions.ge("_tasks.stainingCompletionDate", fromDate))
				.add(Restrictions.le("_tasks.stainingCompletionDate", toDate));
		query.add(Restrictions.eq("_tasks.stainingCompleted", true));
		query.add(Restrictions.eq("_tasks.diagnosisCompleted", completed));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Returns a list of patients which had the diagnosis completed between the
	 * two given dats.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @param completed
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> getPatientByNotificationBetweenDates(long fromDate, long toDate, boolean completed) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.createAlias("patient.tasks", "_tasks");
		query.add(Restrictions.ge("_tasks.notificationCompletionDate", fromDate))
				.add(Restrictions.le("_tasks.notificationCompletionDate", toDate));
		query.add(Restrictions.eq("_tasks.diagnosisCompleted", true));
		query.add(Restrictions.eq("_tasks.notificationCompleted", completed));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Returns a list of patients depending on the worklistfilter option.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @param filter
	 * @return
	 */
	public List<Patient> getWorklistDynamicallyByType(long fromDate, long toDate, WorklistSearchFilter filter) {
		switch (filter) {
		case ADDED_TO_WORKLIST:
			logger.debug("Searching for add date from "
					+ TimeUtil.formatDate(fromDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()) + " to "
					+ TimeUtil.formatDate(toDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()));
			return getPatientByAddDateToWorklist(fromDate, toDate);
		case TASK_CREATION:
			logger.debug("Searching for task creation date from "
					+ TimeUtil.formatDate(fromDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()) + " to "
					+ TimeUtil.formatDate(toDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()));
			return getPatientBySampleCreationDateBetweenDates(fromDate, toDate);
		case STAINING_COMPLETED:
			logger.debug("Searching for staining completed "
					+ TimeUtil.formatDate(fromDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()) + " to "
					+ TimeUtil.formatDate(toDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()));
			return getPatientByStainingsBetweenDates(fromDate, toDate, true);
		case DIAGNOSIS_COMPLETED:
			logger.debug("Searching for diagnosis completed "
					+ TimeUtil.formatDate(fromDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()) + " to "
					+ TimeUtil.formatDate(toDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()));
			return getPatientByDiagnosBetweenDates(fromDate, toDate, true);
		default:
			return null;
		}
	}

	public List<Patient> getPatientsByNameSurnameDateExcludePiz(String name, String surname, Date date,
			List<String> pizesToExclude) {

		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");
		
		query.createAlias("patient.person", "_person");

		if (name != null && !name.isEmpty())
			query.add(Restrictions.ilike("_person.name", name, MatchMode.ANYWHERE));
		if (surname != null && !surname.isEmpty())
			query.add(Restrictions.ilike("_person.surname", surname, MatchMode.ANYWHERE));
		if (date != null)
			query.add(Restrictions.eq("_person.birthday", date));
		if (pizesToExclude != null && !pizesToExclude.isEmpty())
			query.add(Restrictions.not(Restrictions.in("piz", pizesToExclude.toArray())));

		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		List<Patient> result = query.getExecutableCriteria(getSession()).list();
		
		return result != null ? result : new ArrayList<>();
	}
	
	/**
	 * Searches for an taskID and returns the patient whom the task belongs to
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
	
	/**
	 * Searches for an slideID and returns the patient whom the slide belongs to
	 * @param slideID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Patient getPatientBySlidID(String slideID) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");
		
		query.createAlias("patient.tasks", "_tasks");
		query.createAlias("_tasks.samples", "_samples");
		query.createAlias("_samples.blocks", "_blocks");
		query.createAlias("_blocks.slides", "_slides");
		query.add(Restrictions.eq("_slides.slideID", slideID));
		

		List<Patient> result = query.getExecutableCriteria(getSession()).list();

		if (result.size() == 1)
			return result.get(0);
		
		return null;
	}
}
