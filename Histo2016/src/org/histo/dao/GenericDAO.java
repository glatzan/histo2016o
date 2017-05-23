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
import org.histo.config.ResourceBundle;
import org.histo.config.SecurityContextHolderUtil;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogInfo;
import org.histo.model.interfaces.PatientRollbackAble;
import org.histo.model.patient.Patient;
import org.histo.model.util.LogListener;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	private ResourceBundle resourceBundle;

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

	// ************************ Save ************************

	public <C extends HasID> boolean saveListRollbackSave(Collection<? extends HasID> objects, String resourcesKey) {
		for (HasID object : objects) {
			if (!saveDataRollbackSave(object, resourcesKey, null, null))
				return false;
		}
		return true;
	}

	// ************************ Save data RollbackSave ************************

	public <C extends HasID> boolean saveDataRollbackSave(C object) {
		return saveDataRollbackSave(object, null, null, null);
	}

	public <C extends HasID> boolean saveDataRollbackSave(C object, String resourcesKey) {
		return saveDataRollbackSave(object, resourcesKey, null, null);
	}

	public <C extends HasID> boolean saveDataRollbackSave(C object, String resourcesKey, Object[] resourcesKeyInsert) {
		return saveDataRollbackSave(object, resourcesKey, resourcesKeyInsert, null);
	}

	public <C extends HasID> boolean saveDataRollbackSave(C object, String resourcesKey, Object[] resourcesKeyInsert,
			Patient patient) {
		try {
			if (resourcesKey != null)
				save(object, resourceBundle.get(resourcesKey, resourcesKeyInsert), patient);
			else {
				save(object);
			}
			getSession().flush();
		} catch (javax.persistence.OptimisticLockException e) {
			getSession().getTransaction().rollback();
			return false;
		}
		return true;
	}

	// ************************ SimpleSave ************************

	@Deprecated
	public <C> C save(C object) {
		return save(object, (LogInfo) null);
	}

	@Deprecated
	public <C> C save(C object, String logMessage) {
		return save(object, new LogInfo(logMessage));
	}

	@Deprecated
	public <C> C save(C object, String logMessage, Patient patient) {
		return save(object, new LogInfo(logMessage, patient));
	}

	/**
	 * Saves an object to the database. No locking save.
	 * 
	 * @param object
	 * @param logInfo
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	public <C> C save(C object, LogInfo logInfo) {

		// sets a logMessage to the securityContext, this is a workaround for
		// passing variables to the revisionListener
		if (logInfo != null) {
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

		return object;
	}

	public void saveCollection(Collection<?> objects) {
		for (Object object : objects) {
			save(object);
		}
	}

	// ************************ Delete ************************
	public void deleteDate(Object toSave, String resourcesKey, Object... arr) {
		if (toSave instanceof PatientRollbackAble) {
			delete(toSave, resourceBundle.get(resourcesKey, ((PatientRollbackAble) toSave).getLogPath(), arr),
					((PatientRollbackAble) toSave).getPatient());
		} else {
			delete(toSave, resourceBundle.get(resourcesKey, arr));
		}
	}

	@Deprecated
	public <C> C delete(C object) {
		return delete(object, (LogInfo) null);
	}

	@Deprecated
	public <C> C delete(C object, String logMessage) {
		return delete(object, new LogInfo(logMessage));
	}

	@Deprecated
	public <C> C delete(C object, String logMessage, Patient patient) {
		return delete(object, new LogInfo(logMessage, patient));
	}

	@Deprecated
	public <C> C delete(C object, LogInfo logInfo) {

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

		return object;
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

	@SuppressWarnings("unchecked")
	public <C extends HasID> C refresh(C object) {
		try {
			object = save(object);
			getSession().flush();
		} catch (javax.persistence.OptimisticLockException e) {
			getSession().getTransaction().rollback();

			getSession().beginTransaction();
			Class<? extends HasID> klass = (Class<? extends HasID>) object.getClass();
			return (C) getSession().get(klass, object.getId());
		}
		return object;
	}

	@SuppressWarnings("unchecked")
	public <C> C refresh(C object) {
		try {
			object = save(object);
			getSession().flush();
		} catch (javax.persistence.OptimisticLockException e) {
			getSession().getTransaction().rollback();
		}
		return object;
	}

	public void reset(Object object) {
		getSession().refresh(object);
	}
}