package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.histo.action.dialog.WorklistSearchDialogHandler.ExtendedSearchData;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.Eye;
import org.histo.config.enums.WorklistSearchFilter;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.Person;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.PatientRollbackAble;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class PatientDao extends AbstractDAO implements Serializable {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private GenericDAO genericDAO;

	public Patient initilaizeTasksofPatient(Patient patient) throws CustomDatabaseInconsistentVersionException {
		genericDAO.refresh(patient);
		Hibernate.initialize(patient.getTasks());
		return patient;
	}

	public Patient initializePatient(Patient patient, boolean initialize)
			throws CustomDatabaseInconsistentVersionException {
		genericDAO.refresh(patient);

		if (initialize) {
			Hibernate.initialize(patient.getTasks());
			Hibernate.initialize(patient.getAttachedPdfs());
		}

		return patient;
	}

	public Patient getPatient(long id, boolean initialize) {
		Patient patient = genericDAO.get(Patient.class, id);
		if (initialize) {
			Hibernate.initialize(patient.getTasks());
			Hibernate.initialize(patient.getAttachedPdfs());
		}
		return patient;
	}

	public <C extends HasID & PatientRollbackAble> C savePatientAssociatedDataFailSave(C object)
			throws CustomDatabaseInconsistentVersionException {
		return savePatientAssociatedDataFailSave(object, object, null);
	}

	public <C extends HasID & PatientRollbackAble> C savePatientAssociatedDataFailSave(C object, String resourcesKey,
			Object... resourcesKeyInsert) throws CustomDatabaseInconsistentVersionException {
		return savePatientAssociatedDataFailSave(object, object, resourcesKey, resourcesKeyInsert);
	}

	public <C extends HasID> C savePatientAssociatedDataFailSave(C object, PatientRollbackAble hasPatient,
			String resourcesKey, Object... resourcesKeyInsert) throws CustomDatabaseInconsistentVersionException {

		// if failed false will be returned
		return genericDAO.saveDataRollbackSave(object, resourcesKey,
				new Object[] { hasPatient.getLogPath(), resourcesKeyInsert }, hasPatient.getPatient());

	}

	public <C extends HasID & PatientRollbackAble> C deletePatientAssociatedDataFailSave(C object, String resourcesKey,
			Object... resourcesKeyInsert) throws CustomDatabaseInconsistentVersionException {
		return deletePatientAssociatedDataFailSave(object, object, resourcesKey, resourcesKeyInsert);
	}

	public <C extends HasID> C deletePatientAssociatedDataFailSave(C object, PatientRollbackAble hasPatient,
			String resourcesKey, Object... resourcesKeyInsert) throws CustomDatabaseInconsistentVersionException {
		return genericDAO.deleteDataRollbackSave(object, resourcesKey,
				new Object[] { hasPatient.getLogPath(), resourcesKeyInsert }, hasPatient.getPatient());

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
	 * Returns an Array of patients which tasks are associted with the list ids.
	 * 
	 * @param listIds
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Patient> getPatientByTaskList(List<Long> listIds) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.createAlias("patient.tasks", "_tasks");
		query.createAlias("_tasks.favouriteLists", "_flist");

		query.add(Restrictions.in("_flist.id", listIds));

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

	public List<Patient> getPatientsByNameSurnameDateExcludePiz(String name, String firstName, Date date,
			List<String> pizesToExclude) {

		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.createAlias("patient.person", "_person");

		if (name != null && !name.isEmpty())
			query.add(Restrictions.ilike("_person.lastname", name, MatchMode.ANYWHERE));
		if (firstName != null && !firstName.isEmpty())
			query.add(Restrictions.ilike("_person.firstname", firstName, MatchMode.ANYWHERE));
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

	public List<Patient> getPatientByCriteria(ExtendedSearchData extendedSearchData) {
		logger.debug("test");

		DetachedCriteria query = DetachedCriteria.forClass(Task.class, "task");

		query.createAlias("task.parent", "patient");
		query.createAlias("patient.person", "person");
		query.createAlias("task.samples", "samples");
		query.createAlias("task.diagnosisContainer", "diagnosisContainer");
		query.createAlias("diagnosisContainer.diagnosisRevisions", "diagnosisRevisions");
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
