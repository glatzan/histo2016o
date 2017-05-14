package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.histo.action.WorklistHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.WorklistSearchFilter;
import org.histo.model.interfaces.HasDataList;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.PatientRollbackAble;
import org.histo.model.patient.Patient;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class PatientDao extends AbstractDAO implements Serializable {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	@Lazy
	private WorklistHandlerAction worklistHandlerAction;

	public <C extends HasID & PatientRollbackAble> PatientRollbackAble savePatientAssociatedData(C object) {
		return savePatientAssociatedData(object, null);
	}

	public <C extends HasID & PatientRollbackAble> PatientRollbackAble savePatientAssociatedData(C object,
			String resourcesKey, Object... resourcesKeyInsert) {
		return savePatientAssociatedData(object, object, resourcesKey, resourcesKeyInsert);
	}

	public <C extends HasID> PatientRollbackAble savePatientAssociatedData(C object, PatientRollbackAble hasPatient,
			String resourcesKey, Object... resourcesKeyInsert) {
		try {
			if (resourcesKey != null)
				genericDAO.save(object, resourceBundle.get(resourcesKey, hasPatient.getLogPath(), resourcesKeyInsert),
						hasPatient.getPatient());
			else {
				Session session = getSession();
				// TODO MOVE to generic dao
				session.saveOrUpdate(object);
			}
			getSession().flush();
		} catch (javax.persistence.OptimisticLockException e) {
			logger.debug("----------- Rollback!");
			getSession().getTransaction().rollback();
			getSession().beginTransaction();
			Patient patient = getSession().get(Patient.class, hasPatient.getPatient().getId());
			worklistHandlerAction.replaceInvaliedPatientInCurrentWorklist(patient);
			System.out.println(patient.getTasks().get(0));
			Class<? extends PatientRollbackAble> klass = (Class<? extends PatientRollbackAble>) hasPatient.getClass();
			C result = (C) getSession().get(klass, hasPatient.getId());
			System.out.println(result);
			return (PatientRollbackAble) getSession().get(klass, object.getId());
		}

		return hasPatient;
	}

	public void initializePatientDate(Patient patient) {
		Hibernate.initialize(((Patient)savePatientAssociatedData(patient)).getAttachedPdfs());
	}

	/**
	 * Returns a list of useres with the given piz. At least 6 numbers of the
	 * piz are needed.
	 * 
	 * @param piz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> searchForPatientsByPiz(String piz) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		String regex = "";
		if (piz.length() != 8) {
			regex = "[0-9]{" + (8 - piz.length()) + "}";
		}

		query.add(Restrictions.like("piz", piz + regex));

		List<Patient> result = query.getExecutableCriteria(getSession()).list();
		return result;
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
	public List<Patient> getPatientByStainings(boolean inPhase) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.createAlias("patient.tasks", "_tasks");
		query.add(Restrictions.eq("_tasks.stainingPhase", inPhase));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Returns a list of patients for that the staining had been completed
	 * within the time period. Don't start with zero.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> getPatientByStainingsBetweenDates(long fromDate, long toDate) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.createAlias("patient.tasks", "_tasks");
		query.add(Restrictions.ge("_tasks.stainingCompletionDate", fromDate))
				.add(Restrictions.le("_tasks.stainingCompletionDate", toDate));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Returns a list of patients deepening on the diagnosis phase.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @param completed
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> getPatientByDiagnosis(boolean inPhase) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");
		query.createAlias("patient.tasks", "_tasks");
		query.add(Restrictions.eq("_tasks.diagnosisPhase", inPhase));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Returns a list of patients for that the notification had been completed
	 * within the time period. Don't start with zero.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> getPatientByDiagnosisBetweenDates(long fromDate, long toDate) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.createAlias("patient.tasks", "_tasks");
		query.add(Restrictions.ge("_tasks.diagnosisCompletionDate", fromDate))
				.add(Restrictions.le("_tasks.diagnosisCompletionDate", toDate));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Returns patient deepening on the phase.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @param completed
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> getPatientByNotification(boolean inPhase) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.createAlias("patient.tasks", "_tasks");
		query.add(Restrictions.eq("_tasks.notificationPhase", inPhase));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Returns a list of patients for that the diagnosis had been completed
	 * within the time period. Don't start with zero.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> getPatientByNotificationBetweenDates(long fromDate, long toDate) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.createAlias("patient.tasks", "_tasks");
		query.add(Restrictions.ge("_tasks.notificationCompletionDate", fromDate))
				.add(Restrictions.le("_tasks.notificationCompletionDate", toDate));
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
			return getPatientByStainingsBetweenDates(fromDate, toDate);
		case DIAGNOSIS_COMPLETED:
			logger.debug("Searching for diagnosis completed "
					+ TimeUtil.formatDate(fromDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()) + " to "
					+ TimeUtil.formatDate(toDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()));
			return getPatientByDiagnosisBetweenDates(fromDate, toDate);
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

	/**
	 * Searches for an slideID and returns the patient whom the slide belongs to
	 * 
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
