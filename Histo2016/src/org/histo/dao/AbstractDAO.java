package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.histo.config.ResourceBundle;
import org.histo.config.SecurityContextHolderUtil;
import org.histo.config.exception.CustomDatabaseConstraintViolationException;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.hibernate.RootAware;
import org.histo.config.log.LogListener;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogInfo;
import org.histo.model.patient.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.orm.hibernate5.HibernateOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

@Configurable
@Transactional
public abstract class AbstractDAO implements Serializable {

	private static final long serialVersionUID = 8566919900494360311L;

	protected static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	protected SessionFactory sessionFactory;

	@Autowired
	protected ResourceBundle resourceBundle;

	private Session session;

	public Session getSession() {
		try {
			return sessionFactory.getCurrentSession();
		} catch (HibernateException hibernateException) {
			hibernateException.printStackTrace();
			if (session == null || !session.isOpen()) {
				session = sessionFactory.openSession();
				logger.trace("Open Hibernate Session");
			}
		}
		return session;
	}

	public static void printStats(Statistics stats) {
		System.out.println("Fetch Count=" + stats.getEntityFetchCount());
		System.out.println("Second Level Hit Count=" + stats.getSecondLevelCacheHitCount());
		System.out.println("Second Level Miss Count=" + stats.getSecondLevelCacheMissCount());
		System.out.println("Second Level Put Count=" + stats.getSecondLevelCachePutCount());
	}

	@SuppressWarnings("unchecked")
	public <C> C get(Class<C> clazz, Serializable serializable) {
		return (C) getSession().get(clazz, serializable);
	}

	public <C extends HasID> C save(C object) throws CustomDatabaseInconsistentVersionException {
		return save(object, null, null, null);
	}

	public <C extends HasID> C save(C object, String resourcesKey) throws CustomDatabaseInconsistentVersionException {
		return save(object, resourcesKey, null, null);
	}

	public <C extends HasID> C save(C object, String resourcesKey, Object[] resourcesKeyInsert)
			throws CustomDatabaseInconsistentVersionException {
		return save(object, resourcesKey, resourcesKeyInsert, null);
	}

	public <C extends HasID> C save(C object, String resourcesKey, Object[] resourcesKeyInsert, Patient patient)
			throws CustomDatabaseInconsistentVersionException {

		try {
			if (resourcesKey != null) {
				LogInfo logInfo = new LogInfo(resourceBundle.get(resourcesKey, resourcesKeyInsert), patient);
				SecurityContextHolderUtil.setObjectToSecurityContext(LogListener.LOG_KEY_INFO, logInfo);
			}
			getSession().saveOrUpdate(object);
			getSession().flush();
			return object;
		} catch (HibernateException hibernateException) {
			object = (C) getSession().merge(object);
			logger.error("Mergin Objects!");
			return object;
		} catch (OptimisticLockException | HibernateOptimisticLockingFailureException e) {
			getSession().getTransaction().rollback();
			logger.error("Version conflict, rolling back!");
			logger.error(e);
			throw new CustomDatabaseInconsistentVersionException(object);
		} catch (Exception e) {
			getSession().getTransaction().rollback();
			logger.error("Error, rolling back!");
			logger.error(e);
			e.printStackTrace();
			throw new CustomDatabaseInconsistentVersionException(object);
		}
	}

	public <C extends HasID> void saveCollection(List<C> objects, String resourcesKey) {
		saveCollection(objects, resourcesKey, null);
	}

	public <C extends HasID> void saveCollection(List<C> objects, String resourcesKey, Object[] resourcesKeyInsert) {
		saveCollection(objects, resourcesKey, resourcesKeyInsert, null);
	}

	public <C extends HasID> void saveCollection(List<C> objects, String resourcesKey, Object[] resourcesKeyInsert,
			Patient patient) {
		for (C object : objects) {
			save(object, resourcesKey, resourcesKeyInsert, patient);
		}
	}

	public <C extends HasID> C delete(C object) {
		return delete(object, null);
	}

	public <C extends HasID> C delete(C object, String resourcesKey) {
		return delete(object, resourcesKey, null);
	}

	public <C extends HasID> C delete(C object, String resourcesKey, Object[] resourcesKeyInsert) {
		return delete(object, resourcesKey, resourcesKeyInsert, null);
	}

	public <C extends HasID> C delete(C object, String resourcesKey, Object[] resourcesKeyInsert, Patient patient) {

		if (resourcesKey != null) {
			LogInfo logInfo = new LogInfo(resourceBundle.get(resourcesKey, resourcesKeyInsert), patient);
			SecurityContextHolderUtil.setObjectToSecurityContext(LogListener.LOG_KEY_INFO, logInfo);
		}

		try {
			getSession().delete(object);
			getSession().flush();
		} catch (HibernateException hibernateException) {
			session.delete(session.merge(object));
		} catch (javax.persistence.OptimisticLockException e) {
			getSession().getTransaction().rollback();
			throw new CustomDatabaseInconsistentVersionException(object);
		} catch (PersistenceException e) {
			getSession().getTransaction().rollback();
			throw new CustomDatabaseConstraintViolationException(object);
		} catch (Exception e) {
			getSession().getTransaction().rollback();
			logger.error("Error, rolling back!");
			logger.error(e);
			e.printStackTrace();
			throw new CustomDatabaseInconsistentVersionException(object);
		}

		return object;
	}

	@SuppressWarnings("unchecked")
	public <C extends HasID> C reattach(C object) throws CustomDatabaseInconsistentVersionException {
		try {
			getSession().saveOrUpdate(object);
			getSession().flush();
		} catch (javax.persistence.OptimisticLockException e) {
			getSession().getTransaction().rollback();
			getSession().beginTransaction();

			// Class<? extends HasID> klass = (Class<? extends HasID>)
			// object.getClass();
			throw new CustomDatabaseInconsistentVersionException(object);
		} catch (HibernateException hibernateException) {
			object = (C) getSession().merge(object);
			logger.debug("Error: Merging objects");
			hibernateException.printStackTrace();
		}
		return object;
	}

	public void refresh(Object object) {
		getSession().refresh(object);
	}

	public void commit() {
		getSession().getTransaction().commit();
	}

	public void lockParent(RootAware<?> rootAware) {
		lock(rootAware.root());
	}

	public void lock(Object object) {
		getSession().lock(object, LockMode.OPTIMISTIC_FORCE_INCREMENT);
		getSession().flush();
	}
}
