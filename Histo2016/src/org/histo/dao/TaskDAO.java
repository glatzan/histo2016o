package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Eye;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.AssociatedContact;
import org.histo.model.Council;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.Signature;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.util.HistoUtil;
import org.histo.util.TimeUtil;
import org.histo.worklist.search.WorklistSearchExtended;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javassist.tools.reflect.Sample;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Transactional
@Scope(value = "session")
public class TaskDAO extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = 7999598227641226109L;

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
		task = reattach(task);

		if (initialized) {
			Hibernate.initialize(task.getCouncils());
			Hibernate.initialize(task.getDiagnosisRevisions());
			Hibernate.initialize(task.getAttachedPdfs());
			Hibernate.initialize(task.getContacts());
		}

		return task;
	}

	public Task initializeTaskAndPatient(Task task) throws CustomDatabaseInconsistentVersionException {
		reattach(task);
		reattach(task.getPatient());

		Hibernate.initialize(task.getCouncils());
		Hibernate.initialize(task.getDiagnosisRevisions());
		Hibernate.initialize(task.getAttachedPdfs());
		Hibernate.initialize(task.getContacts());

		Hibernate.initialize(task.getParent().getTasks());
		Hibernate.initialize(task.getParent().getAttachedPdfs());

		return task;
	}

	public void initializeCouncils(Task task) throws CustomDatabaseInconsistentVersionException {
		for (Council council : task.getCouncils()) {
			reattach(council);
			Hibernate.initialize(council.getAttachedPdfs());
		}
	}

	public Task getTaskAndPatientInitialized(long id) {
		Task task = get(Task.class, id);

		if (task != null) {
			getSession().refresh(task.getPatient());
			getSession().refresh(task);

			Hibernate.initialize(task.getCouncils());
			Hibernate.initialize(task.getDiagnosisRevisions());
			Hibernate.initialize(task.getAttachedPdfs());

			Hibernate.initialize(task.getParent().getTasks());
			Hibernate.initialize(task.getParent().getAttachedPdfs());
		}
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
		criteria.addOrder(Order.desc("taskID"));
		criteria.setFirstResult(page * count);
		criteria.setMaxResults(count);

		List<Task> list = criteria.list();

		return list;
	}

	/**
	 * Returns true if task exsists in database
	 * 
	 * @param taskID
	 * @return
	 */
	public boolean isTaskIDPresentInDatabase(String taskID) {
		if (getTaskByTaskID(taskID) != null)
			return true;
		return false;
	}

	public Task getTaskByTaskID(String taskID) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Task> criteria = qb.createQuery(Task.class);
		Root<Task> root = criteria.from(Task.class);
		criteria.select(root);

		criteria.where(qb.like(root.get("taskID"), taskID));
		criteria.distinct(true);

		List<Task> task = getSession().createQuery(criteria).getResultList();

		return task.size() > 0 ? task.get(0) : null;
	}

	public Task getTaskBySlideID(String taskID, int uniqueSlideIDInBlock) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Task> criteria = qb.createQuery(Task.class);
		Root<Task> root = criteria.from(Task.class);
		criteria.select(root);

		Join<Task, Sample> sampleQuery = root.join("samples", JoinType.LEFT);
		Join<Sample, Block> blockQuery = sampleQuery.join("blocks", JoinType.LEFT);
		Join<Block, Slide> slideQuery = blockQuery.join("slides", JoinType.LEFT);

		criteria.where(qb.and(qb.like(root.get("taskID"), taskID),
				qb.equal(slideQuery.get("uniqueIDinTask"), uniqueSlideIDInBlock)));
		criteria.distinct(true);

		List<Task> task = getSession().createQuery(criteria).getResultList();

		return task.size() > 0 ? task.get(0) : null;
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

	public Task getTaskWithLastID(Calendar ofYear) {

		String currentYear = Integer.toString(TimeUtil.getYearAsInt(ofYear) - 2000) + "%";

		// Create CriteriaBuilder
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Task> criteria = qb.createQuery(Task.class);
		Root<Task> taskRoot = criteria.from(Task.class);
		criteria.select(taskRoot);

		Subquery<Task> subquery = criteria.subquery(Task.class);
		Root<Task> subTaskRoot = subquery.from(Task.class);
		subquery.where(qb.like(subTaskRoot.get("taskID"), currentYear));
		subquery.select(qb.max((Expression) subTaskRoot.get("taskID")));

		criteria.where(qb.equal(taskRoot.get("taskID"), subquery));

		List<Task> task = getSession().createQuery(criteria).getResultList();

		if (task.size() == 0)
			return null;
		else
			return task.get(0);
	}

	public List<Task> getTaskByCriteria(WorklistSearchExtended worklistSearchExtended, boolean initParent) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Task> criteria = qb.createQuery(Task.class);
		Root<Task> root = criteria.from(Task.class);
		criteria.select(root);

		if (initParent)
			root.fetch("parent", JoinType.LEFT);
		
		Join<Task, Sample> sampleQuery = root.join("samples", JoinType.LEFT);
		Join<Sample, Block> blockQuery = sampleQuery.join("blocks", JoinType.LEFT);
		Join<Block, Slide> slideQuery = blockQuery.join("slides", JoinType.LEFT);
		Join<Slide, StainingPrototype> prototypeQuery = slideQuery.join("slidePrototype", JoinType.LEFT);

		Join<Task, DiagnosisRevision> diagnosisRevisionQuery = root.join("diagnosisRevisions", JoinType.LEFT);
		Join<DiagnosisRevision, Diagnosis> diagnosesQuery = diagnosisRevisionQuery.join("diagnoses", JoinType.LEFT);
		Join<DiagnosisRevision, Signature> signatureOneQuery = diagnosisRevisionQuery.join("signatureOne",
				JoinType.LEFT);
		Join<Signature, Physician> signatureOnePhysicianQuery = signatureOneQuery.join("physician", JoinType.LEFT);
		Join<DiagnosisRevision, Signature> signatureTwoQuery = diagnosisRevisionQuery.join("signatureTwo",
				JoinType.LEFT);
		Join<Signature, Physician> signatureTwoPhysicianQuery = signatureTwoQuery.join("physician", JoinType.LEFT);

		Join<AssociatedContact, Task> contactQuery = root.join("contacts", JoinType.LEFT);
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
			predicates.add(qb.like(qb.lower(root.get("caseHistory")),
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
			predicates.add(qb.equal(root.get("eye"), worklistSearchExtended.getEye()));

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

		List<Task> tasks = getSession().createQuery(criteria).getResultList();

		return tasks;
	}
}
