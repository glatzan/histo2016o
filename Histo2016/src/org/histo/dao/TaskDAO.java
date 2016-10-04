package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.histo.model.Log;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.Task;
import org.histo.model.util.LogAble;
import org.histo.util.TimeUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class TaskDAO extends AbstractDAO implements Serializable {

	public int countSamplesOfCurrentYear() {
		Criteria c = getSession().createCriteria(Task.class);
		c.add(Restrictions.ge("creationDate",
				TimeUtil.getDateInUnixTimestamp(TimeUtil.getCurrentYear(), 0, 0, 0, 0, 0)))
				.add(Restrictions.le("creationDate",
						TimeUtil.getDateInUnixTimestamp(TimeUtil.getCurrentYear(), 12, 31, 23, 59, 59)))
				.list();
		Integer totalResult = ((Number) c.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		System.out.println("Counting Tasks " + totalResult);
		return totalResult.intValue();
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

	public List<Object[]> getDiangosisRevisions(Diagnosis diagnosis) {
		System.out.println(diagnosis);
		AuditQuery auditQuery = AuditReaderFactory.get(getSession()).createQuery()
				.forRevisionsOfEntity(Diagnosis.class, false, false).add(AuditEntity.id().eq(diagnosis.getId()));

		List<Object[]> l = auditQuery.getResultList();
		return l;
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
			logs.add((Log)tmp[1]);
		}
		
		return logs;
	}
	
	public void initializeTask(Task task){
		getSession().update(task);
		Hibernate.initialize(task.getPdfs());
	}
}
