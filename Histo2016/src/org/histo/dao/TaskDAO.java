package org.histo.dao;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.histo.action.dialog.WorklistSearchDialogHandler.ExtendedSearchData;
import org.histo.config.enums.Eye;
import org.histo.config.enums.Gender;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.Council;
import org.histo.model.FavouriteList;
import org.histo.model.patient.Task;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class TaskDAO extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = 7999598227641226109L;

	@Autowired
	private GenericDAO genericDAO;

	/**
	 * Counts all tasks of the current year
	 * 
	 * @return
	 */
	public int countTasksOfCurrentYear() {

		DetachedCriteria query = DetachedCriteria.forClass(Task.class, "task");
		query.add(Restrictions.ge("creationDate",
				TimeUtil.getDateInUnixTimestamp(TimeUtil.getCurrentYear(), 0, 0, 0, 0, 0)))
				.add(Restrictions.le("creationDate",
						TimeUtil.getDateInUnixTimestamp(TimeUtil.getCurrentYear(), 12, 31, 23, 59, 59)));
		query.setProjection(Projections.rowCount());

		Number result = (Number) query.getExecutableCriteria(getSession()).uniqueResult();

		return result.intValue();
	}

	/**
	 * Counts all tasks
	 * 
	 * @return
	 */
	public int countTotalTasks() {
		DetachedCriteria query = DetachedCriteria.forClass(Task.class, "task");
		query.setProjection(Projections.rowCount());
		Number result = (Number) query.getExecutableCriteria(getSession()).uniqueResult();

		return result.intValue();
	}

	public Task initializeTask(Task task, boolean initialized) throws CustomDatabaseInconsistentVersionException {
		task = genericDAO.refresh(task);

		if (initialized) {
			Hibernate.initialize(task.getCouncils());
			Hibernate.initialize(task.getDiagnosisContainer());
			Hibernate.initialize(task.getAttachedPdfs());
		}

		return task;
	}

	public Task initializeTaskAndPatient(Task task) throws CustomDatabaseInconsistentVersionException {
		task = genericDAO.refresh(task);

		Hibernate.initialize(task.getCouncils());
		Hibernate.initialize(task.getDiagnosisContainer());
		Hibernate.initialize(task.getAttachedPdfs());

		genericDAO.refresh(task.getParent());

		Hibernate.initialize(task.getParent().getTasks());
		Hibernate.initialize(task.getParent().getAttachedPdfs());

		return task;
	}

	public void initializeCouncils(Task task) throws CustomDatabaseInconsistentVersionException {
		for (Council council : task.getCouncils()) {
			genericDAO.refresh(council);
			Hibernate.initialize(council.getAttachedPdfs());
		}
	}

	public Task getTaskAndPatientInitialized(long id) {
		Task task = genericDAO.get(Task.class, id);

		Hibernate.initialize(task.getCouncils());
		Hibernate.initialize(task.getDiagnosisContainer());
		Hibernate.initialize(task.getAttachedPdfs());

		Hibernate.initialize(task.getParent().getTasks());
		Hibernate.initialize(task.getParent().getAttachedPdfs());

		return task;
	}

	/**
	 * Gets a list of task
	 * 
	 * @param count
	 * @param page
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public List<Task> getTasks(int count, int page) {
		Criteria criteria = getSession().createCriteria(Task.class);
		criteria.addOrder(Order.desc("id"));
		criteria.setFirstResult(page * count);
		criteria.setMaxResults(count);

		List<Task> list = criteria.list();

		return list;
	}

	public List<Task> getPatientByCriteria(ExtendedSearchData extendedSearchData) {
		logger.debug("test");

		DetachedCriteria query = DetachedCriteria.forClass(Task.class, "task");

		query.createAlias("task.parent", "patient");
		query.createAlias("patient.person", "person");
		query.createAlias("task.samples", "samples");
		query.createAlias("task.diagnosisContainer", "diagnosisContainer");
		query.createAlias("diagnosisContainer.diagnosisRevisions", "diagnosisRevisions");
		query.createAlias("diagnosisRevisions.diagnoses", "diagnoses");

		if (extendedSearchData.getName() != null && !extendedSearchData.getName().isEmpty()) {
			query.add(Restrictions.ilike("person.name", extendedSearchData.getName(), MatchMode.ANYWHERE));
			logger.debug("search for name: " + extendedSearchData.getName());
		}

		if (extendedSearchData.getSurename() != null && !extendedSearchData.getSurename().isEmpty()) {
			query.add(Restrictions.ilike("person.surename", extendedSearchData.getSurename(), MatchMode.ANYWHERE));
			logger.debug("search for surename: " + extendedSearchData.getSurename());
		}

		if (extendedSearchData.getBirthday() != null) {
			query.add(Restrictions.eq("person.birthday", extendedSearchData.getBirthday()));
			logger.debug("search for birthday: " + extendedSearchData.getBirthday());
		}

		if (extendedSearchData.getGender() != null && extendedSearchData.getGender() != Gender.UNKNOWN) {
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

		List<Task> result = query.getExecutableCriteria(getSession()).list();

		return result;
	}

}
