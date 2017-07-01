package org.histo.dao;

import java.io.Serializable;

import javax.persistence.OptimisticLockException;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.stat.Statistics;
import org.histo.config.ResourceBundle;
import org.histo.config.SecurityContextHolderUtil;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogInfo;
import org.histo.model.patient.Patient;
import org.histo.model.util.LogListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Scope;
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

	public <C extends HasID> C saveDataRollbackSave(C object) throws CustomDatabaseInconsistentVersionException {
		return saveDataRollbackSave(object, null, null, null);
	}

	public <C extends HasID> C saveDataRollbackSave(C object, String resourcesKey)
			throws CustomDatabaseInconsistentVersionException {
		return saveDataRollbackSave(object, resourcesKey, null, null);
	}

	public <C extends HasID> C saveDataRollbackSave(C object, String resourcesKey, Object[] resourcesKeyInsert)
			throws CustomDatabaseInconsistentVersionException {
		return saveDataRollbackSave(object, resourcesKey, resourcesKeyInsert, null);
	}

	public <C extends HasID> C saveDataRollbackSave(C object, String resourcesKey, Object[] resourcesKeyInsert,
			Patient patient) throws CustomDatabaseInconsistentVersionException {
		try {
			if (resourcesKey != null) {
				LogInfo logInfo = new LogInfo(resourceBundle.get(resourcesKey, resourcesKeyInsert), patient);
				SecurityContextHolderUtil.setObjectToSecurityContext(LogListener.LOG_KEY_INFO,
						logInfo.getInfo() + " for "
								+ (logInfo.getPatient() != null ? logInfo.getPatient().getPerson().getFullName() : ""));
			}
			session.saveOrUpdate(object);
			session.flush();
			return object;
		} catch (HibernateException hibernateException) {
			object = (C) session.merge(object);
			logger.error("Mergin Objects!");
			return object;
		} catch (OptimisticLockException | HibernateOptimisticLockingFailureException e) {
			getSession().getTransaction().rollback();
			throw new CustomDatabaseInconsistentVersionException(object);
		} catch (Exception e) {
			getSession().getTransaction().rollback();
			throw new CustomDatabaseInconsistentVersionException(object);
		}
	}
}
