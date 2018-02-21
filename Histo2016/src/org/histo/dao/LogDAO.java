package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.histo.model.interfaces.LogAble;
import org.histo.model.log.Log;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.Patient;
import org.histo.model.user.HistoUser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class LogDAO extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = -7164924738003274594L;

	public List<Log> getPatientLog(Patient patient) {
		// Create CriteriaBuilder
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Log> criteria = qb.createQuery(Log.class);
		Root<Log> logRoot = criteria.from(Log.class);
		criteria.select(logRoot);

		criteria.where(qb.equal(logRoot.get("patient"), patient));
		criteria.orderBy(qb.asc(logRoot.get("id")));

		return getSession().createQuery(criteria).getResultList();
	}

	public void deleteUserLogs(HistoUser user) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// create delete
		CriteriaDelete<Log> delete = qb.createCriteriaDelete(Log.class);

		// set the root class
		Root<Log> e = delete.from(Log.class);

		// set where clause
		delete.where(qb.equal(e.get("useracc_id"), user.getId()));

		// perform update
		getSession().createQuery(delete).executeUpdate();
	}

	/**
	 * Removes all Logs for a Patient
	 * 
	 * @param patient
	 */
	public void deletePatientLogs(Patient patient) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// create delete
		CriteriaDelete<Log> delete = qb.createCriteriaDelete(Log.class);

		// set the root class
		Root<Log> e = delete.from(Log.class);

		// set where clause
		delete.where(qb.equal(e.get("patient_id"), patient.getId()));

		// perform update
		getSession().createQuery(delete).executeUpdate();
	}

	public void getDiagnosisRevisions(Diagnosis diagnosis) {
		AuditReader reader = AuditReaderFactory.get(getSession());

		AuditQuery query = reader.createQuery().forRevisionsOfEntity(diagnosis.getClass(), false, true)
				.add(AuditEntity.id().eq(diagnosis.getId()));

		List<Object[]> raw_results = query.getResultList();

		for (Object[] data : raw_results) {
			System.out.println(((Log) data[1]).getLogString());
		}
	}

	public List<Object[]> getRevisionsAndObjects(LogAble object) {
		System.out.println(object.getClass());
		AuditQuery auditQuery = AuditReaderFactory.get(getSession()).createQuery()
				.forRevisionsOfEntity(object.getClass(), false, false).add(AuditEntity.id().eq(object.getId()));

		List<Object[]> l = auditQuery.getResultList();
		return l;
	}

	public List<Log> getRevisions(LogAble object) {
		List<Object[]> objects = getRevisionsAndObjects(object);

		ArrayList<Log> logs = new ArrayList<>(objects.size());

		for (Object[] tmp : objects) {
			logs.add((Log) tmp[1]);
		}

		return logs;
	}

	/**
	 * Counts all log entries
	 * 
	 * @return
	 */
	public int countTotalLogs() {
		DetachedCriteria query = DetachedCriteria.forClass(Log.class, "log");
		query.setProjection(Projections.rowCount());
		Number result = (Number) query.getExecutableCriteria(getSession()).uniqueResult();

		return result.intValue();
	}

	/**
	 * Returns a pages of log entries
	 * 
	 * @param count
	 * @param page
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public List<Log> getLogs(int count, int page) {
		Criteria criteria = getSession().createCriteria(Log.class);
		criteria.addOrder(Order.desc("id"));
		criteria.setFirstResult(page * count);
		criteria.setMaxResults(count);

		List<Log> list = criteria.list();

		return list;
	}

}
