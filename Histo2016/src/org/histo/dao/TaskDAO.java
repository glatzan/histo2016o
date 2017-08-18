package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.BioBank;
import org.histo.model.Council;
import org.histo.model.patient.Task;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Transactional
@Scope(value = "session")
public class TaskDAO extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = 7999598227641226109L;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private BioBankDAO bioBankDAO;

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

	/**
	 * Returns a list of task revisions
	 * 
	 * @param taskID
	 * @return
	 */
	public List<Task> getTasksRevisions(long taskID) {
		return AuditReaderFactory.get(getSession()).createQuery().forRevisionsOfEntity(Task.class, false, false)
				.add(AuditEntity.id().eq(taskID)).addOrder(AuditEntity.revisionNumber().asc()).getResultList();
	}

	public void deleteTask(Task task) {
		// LockOptions(LockMode.OPTIMISTIC_FORCE_INCREMENT)).lock(task.getParent());

		task = genericDAO.refresh(task);
		
		// removing from favouriteLists
		while (task.getFavouriteLists().size() > 0) {
			favouriteListDAO.removeTaskFromList(task,
					favouriteListDAO.initFavouriteList(task.getFavouriteLists().get(0)));
		}

		logger.debug("Deleting Task " + task.getTaskID());

		BioBank bioBank = bioBankDAO.getAssociatedBioBankObject(task);

		bioBankDAO.delete(bioBank);

		genericDAO.deletePatientData(task, "log.patient.task.remove", task.toString());
		
		task.getPatient().getTasks().remove(task);
		
		genericDAO.save(task.getPatient());
		
		System.out.println(task.getPatient().getVersion());
	}
}
