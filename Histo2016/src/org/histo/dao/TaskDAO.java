package org.histo.dao;

import java.io.Serializable;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Task;
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
		return totalResult.intValue();
	}
	
	public void initializePdfData(Task task) {
		getSession().update(task);
		Hibernate.initialize(task.getAttachedPdfs());
	}
	
	public void initializeDiagnosisData(Task task) {
		getSession().update(task);
		Hibernate.initialize(task.getDiagnosisInfo());
	}
	
	public void initializeCouncilData(Task task) {
		getSession().update(task);
		Hibernate.initialize(task.getCouncil());
	}
}
