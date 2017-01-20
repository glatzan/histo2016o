package org.histo.dao;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.histo.config.SecurityContextHolderUtil;
import org.histo.model.interfaces.LogInfo;
import org.histo.model.patient.Patient;
import org.histo.model.util.LogListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The DAO class for generic entities
 *
 * @author Thomas Hemprich
 */

@Component
@Transactional
@Scope("session")
public class GenericDAO extends AbstractDAO {

	private static Logger logger = Logger.getLogger("org.histo");

	@SuppressWarnings("unchecked")
	public <C> C get(Class<C> clazz, Serializable serializable) {
		return (C) getSession().get(clazz, serializable);
	}

	@SuppressWarnings("unchecked")
	public <C> List<C> findAllByNamedWildcardParameter(Class<C> clazz, String parameterName, Object parameterValue) {
		// Added to avoid possible duplicates introduced by eager fetching
		return getSession().createCriteria(clazz).add(Restrictions.like(parameterName, parameterValue))
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
	}

	public <C> List<C> findAllByNamedParameter(Class<C> clazz, String parameterName, Object parameterValue) {
		return findAllByNamedParameter(clazz, parameterName, parameterValue, null, true);
	}

	@SuppressWarnings("unchecked")
	public <C> List<C> findAllByNamedParameter(Class<C> clazz, String parameterName, Object parameterValue,
			String orderByColumnName, boolean ascending) {
		Criteria criteria = getSession().createCriteria(clazz).add(Restrictions.eq(parameterName, parameterValue));

		if (orderByColumnName != null) {
			criteria.addOrder(ascending ? Order.asc(orderByColumnName) : Order.desc(orderByColumnName));
		}
		// Added to avoid possible duplicates introduced by eager fetching
		return criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
	}

	public <C> Integer count(Class<C> clazz) {
		return ((Long) getSession().createCriteria(clazz).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
	}

	public <C> Integer countByNamedParameter(Class<C> clazz, String parameterName, Object parameterValue) {
		return ((Long) getSession().createCriteria(clazz).add(Restrictions.eq(parameterName, parameterValue))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
	}

	public <C> C save(C object) {
		return save(object, (LogInfo) null);
	}

	public <C> C save(C object, String logMessage) {
		return save(object, new LogInfo(logMessage));
	}

	public <C> C save(C object, String logMessage, Patient patient) {
		return save(object, new LogInfo(logMessage, patient));
	}

	@SuppressWarnings("unchecked")
	public <C> C save(C object, LogInfo logInfo) {

		logger.info("Saving " + object.toString());
		// sets a logMessage to the securityContext, this is a workaround for
		// passing variables to the revisionListener
		if (logInfo != null) {
			logger.info("Loginfo: " + logInfo.getInfo() + (logInfo.getPatient() != null
					? ", Patient " + logInfo.getPatient().getPerson().getFullName() : ""));
			SecurityContextHolderUtil.setObjectToSecurityContext(LogListener.LOG_KEY_INFO, logInfo.getInfo() + " for "
					+ (logInfo.getPatient() != null ? logInfo.getPatient().getPerson().getFullName() : ""));
		}

		Session session = getSession();

		// Statistics stats = sessionFactory.getStatistics();
		// stats.setStatisticsEnabled(true);
		// printStats(stats);

		try {
			session.saveOrUpdate(object);
		} catch (HibernateException hibernateException) {
			object = (C) session.merge(object);
			hibernateException.printStackTrace();
		}

		logger.info("Saved " + object.toString());

		return object;
	}

	public void saveCollection(Collection<?> objects) {
		for (Object object : objects) {
			save(object);
		}
	}
	
	public void save(Collection<?> objects, String logMessage) {
		for (Object object : objects) {
			save(object, logMessage);
		}
	}

	public <C> C delete(C object) {
		return save(object, (LogInfo) null);
	}

	public <C> C delete(C object, String logMessage, Patient patient) {
		return save(object, new LogInfo(logMessage, patient));
	}

	public void delete(Object object, LogInfo logInfo) {

		// sets a logMessage to the securityContext, this is a workaround for
		// passing variables to the revisionListener
		if (logInfo != null) {
			SecurityContextHolderUtil.setObjectToSecurityContext(LogListener.LOG_KEY_INFO, logInfo);
		}

		Session session = getSession();
		try {
			session.delete(object);
		} catch (HibernateException hibernateException) {
			session.delete(session.merge(object));
		}
	}

	public void delete(Collection<?> objects) {
		Session session = getSession();
		for (Object object : objects) {
			try {
				session.delete(object);
			} catch (HibernateException hibernateException) {
				session.delete(session.merge(object));
			}
		}
	}

	public <C> boolean isEntityWithNamedParameterExistent(Class<C> clazz, String parameterName, String parameterValue) {
		return (Long) getSession().createCriteria(clazz).add(Restrictions.eq(parameterName, parameterValue))
				.setProjection(Projections.rowCount()).uniqueResult() > 0;
	}

	public <C> boolean isEntityWithNamedParameterExistent(Class<C> clazz, String parameterName, Long parameterValue) {
		return (Long) getSession().createCriteria(clazz).add(Restrictions.eq(parameterName, parameterValue))
				.setProjection(Projections.rowCount()).uniqueResult() > 0;
	}

	public void refresh(Object object) {
		getSession().refresh(object);
	}

}