package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.histo.model.Log;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.Patient;
import org.histo.model.util.LogAble;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class LogDAO extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = -7164924738003274594L;

	public List<Log> getPatientLog(Patient patient) {
		DetachedCriteria query = DetachedCriteria.forClass(Log.class)
				.add(Property.forName("patient").eq(patient)).addOrder(Order.desc("id") );

		List<Log> log = query.getExecutableCriteria(getSession()).list();
		return log;
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
	
}
