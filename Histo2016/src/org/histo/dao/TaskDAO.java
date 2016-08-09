package org.histo.dao;

import java.io.Serializable;

import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.histo.model.Task;
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
	c.add(Restrictions.ge("creationDate", TimeUtil.getDateInUnixTimestamp(TimeUtil.getCurrentYear(), 0, 0, 0, 0, 0))).add(Restrictions.le("creationDate", TimeUtil.getDateInUnixTimestamp(TimeUtil.getCurrentYear(), 12, 31, 23, 59, 59))).list();
	Integer totalResult = ((Number) c.setProjection(Projections.rowCount()).uniqueResult()).intValue();
	System.out.println("Counting Tasks " + totalResult);
	return totalResult.intValue();
    }
}
