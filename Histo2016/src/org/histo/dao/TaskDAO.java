package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
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
public class TaskDAO extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = 7999598227641226109L;

	@Autowired
	private PatientDao patientDao;

	public Task initializeTaskDate(Task task) {
		task = (Task) patientDao.savePatientAssociatedData(task);
		Hibernate.initialize(task.getAttachedPdfs());
		return task;
	}

	/**
	 * Initializes lazy fetch for council data
	 * 
	 * @param task
	 */
	public Task initializeCouncilData(Task task) {
		task = (Task) patientDao.savePatientAssociatedData(task);
		Hibernate.initialize(task.getCouncils());
		return task;
	}

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

	/**
	 * Initializes diagnosisContainer with all diagnoses
	 * 
	 * @param task
	 */
	public Task initializeDiagnosisData(Task task) {
		task = (Task) patientDao.savePatientAssociatedData(task);
		Hibernate.initialize(task.getDiagnosisContainer());
		task = (Task) patientDao.savePatientAssociatedData(task.getDiagnosisContainer(),task);
		Hibernate.initialize(task.getDiagnosisContainer().getSignatureOne());
		Hibernate.initialize(task.getDiagnosisContainer().getSignatureTwo());
		return task;
	}

}
