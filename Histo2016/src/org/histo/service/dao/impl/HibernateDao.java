package org.histo.service.dao.impl;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.histo.config.ResourceBundle;
import org.histo.config.SecurityContextHolderUtil;
import org.histo.config.exception.HistoDatabaseConstraintViolationException;
import org.histo.config.exception.HistoDatabaseException;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.config.exception.HistoDatabaseMergeException;
import org.histo.config.log.LogListener;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogInfo;
import org.histo.model.patient.Patient;
import org.histo.service.dao.GenericDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Basic DAO operations dependent with Hibernate's specific classes
 * 
 * @see SessionFactory
 */

@Getter
@Setter
@Transactional
public class HibernateDao<E, K extends Serializable> implements GenericDao<E, K> {

	protected static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	protected SessionFactory sessionFactory;

	@Autowired
	protected ResourceBundle resourceBundle;

	protected Class<? extends E> daoType;

	public HibernateDao() {
		daoType = (Class<E>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	protected Session currentSession() {
		return sessionFactory.getCurrentSession();
	}

	protected CriteriaBuilder getBuilder() {
		return currentSession().getCriteriaBuilder();
	}

	@Override
	public void add(E entity) {
		currentSession().save(entity);
	}

	@Override
	public void update(E entity) {
		currentSession().saveOrUpdate(entity);
	}

	/**
	 * Reattaches entity
	 * 
	 * @param entity
	 * @throws HistoDatabaseInconsistentVersionException
	 */
	public void reattach(E entity) throws HistoDatabaseInconsistentVersionException, HistoDatabaseMergeException {
		try {
			update(entity);
		} catch (javax.persistence.OptimisticLockException e) {
			currentSession().getTransaction().rollback();
			currentSession().beginTransaction();
			logger.error("Error: Database Locking exception (" + entity.toString() + ")");
			throw new HistoDatabaseInconsistentVersionException(entity);
		} catch (HibernateException hibernateException) {
			logger.error("Error: Merging objects");
			throw new HistoDatabaseMergeException(entity);
		}
	}

	@Override
	public void remove(E entity) {
		currentSession().delete(entity);
	}

	@Override
	public E find(K key) {
		return (E) currentSession().get(daoType, key);
	}

	@Override
	public List<E> list() {
		return currentSession().createCriteria(daoType).list();
	}

	/**
	 * Saves an entity
	 * 
	 * @param entity
	 */
	public void save(Object entity) {
		save(entity, null);
	}

	/**
	 * Saves an entity
	 * 
	 * @param entity
	 * @param resourcesKey
	 * @param resourcesKeyInsert
	 */
	public void save(Object entity, String resourcesKey, Object... resourcesKeyInsert) {
		save(entity, null, resourcesKey, resourcesKeyInsert);
	}

	/**
	 * Saves an entity
	 * 
	 * @param entity
	 * @param resourcesKey
	 * @param resourcesKeyInsert
	 * @param patient
	 */
	public void save(Object entity, Patient patient, String resourcesKey, Object... resourcesKeyInsert) {
		try {
			if (resourcesKey != null) {
				LogInfo logInfo = new LogInfo(resourceBundle.get(resourcesKey, resourcesKeyInsert), patient);
				SecurityContextHolderUtil.setObjectToSecurityContext(LogListener.LOG_KEY_INFO, logInfo);
			}
			currentSession().saveOrUpdate(entity);
			currentSession().flush();
		} catch (HibernateException hibernateException) {
			logger.error("Error: Merging objects");
			throw new HistoDatabaseMergeException(entity);
		} catch (OptimisticLockException | HibernateOptimisticLockingFailureException e) {
			currentSession().getTransaction().rollback();
			// currentSession().beginTransaction();
			logger.error("Version conflict, rolling back! " + e);
			throw new HistoDatabaseInconsistentVersionException(entity);
		} catch (Exception e) {
			currentSession().getTransaction().rollback();
			// currentSession().beginTransaction();
			logger.error("Error, rolling back!" + e);
			e.printStackTrace();
			throw new HistoDatabaseException(entity);
		}
	}

	/**
	 * Saves a collection
	 * 
	 * @param objects
	 */
	public void saveCollection(List<Object> objects) {
		saveCollection(objects, null);
	}

	/**
	 * Saves a collection
	 * 
	 * @param objects
	 * @param resourcesKey
	 * @param resourcesKeyInsert
	 */
	public void saveCollection(List<Object> objects, String resourcesKey, Object... resourcesKeyInsert) {
		saveCollection(objects, null, resourcesKey, resourcesKeyInsert);
	}

	/**
	 * Saves a collection
	 * 
	 * @param objects
	 * @param resourcesKey
	 * @param resourcesKeyInsert
	 * @param patient
	 */
	public void saveCollection(List<Object> objects, Patient patient, String resourcesKey,
			Object... resourcesKeyInsert) {
		for (Object object : objects) {
			save(object, patient, resourcesKey, resourcesKeyInsert);
		}
	}

	/**
	 * Deletes an entity
	 * 
	 * @param entity
	 */
	public void delete(Object entity) {
		delete(entity, null);
	}

	/**
	 * Deletes an entity
	 * 
	 * @param entity
	 * @param resourcesKey
	 * @param resourcesKeyInsert
	 */
	public void delete(Object entity, String resourcesKey, Object... resourcesKeyInsert) {
		delete(entity, null, resourcesKey, resourcesKeyInsert);
	}

	/**
	 * Deletes an entity
	 * 
	 * @param entity
	 * @param patient
	 * @param resourcesKey
	 * @param resourcesKeyInsert
	 */
	public void delete(Object entity, Patient patient, String resourcesKey, Object... resourcesKeyInsert) {
		try {
			if (resourcesKey != null) {
				LogInfo logInfo = new LogInfo(resourceBundle.get(resourcesKey, resourcesKeyInsert), patient);
				SecurityContextHolderUtil.setObjectToSecurityContext(LogListener.LOG_KEY_INFO, logInfo);
			}
			currentSession().delete(entity);
			currentSession().flush();
		} catch (HibernateException hibernateException) {
			logger.error("Error: Merging objects");
			throw new HistoDatabaseMergeException(entity);
		} catch (OptimisticLockException | HibernateOptimisticLockingFailureException e) {
			currentSession().getTransaction().rollback();
			// currentSession().beginTransaction();
			logger.error("Version conflict, rolling back! " + e);
			throw new HistoDatabaseInconsistentVersionException(entity);
		} catch (PersistenceException e) {
			currentSession().getTransaction().rollback();
			throw new HistoDatabaseConstraintViolationException(entity);
		} catch (Exception e) {
			currentSession().getTransaction().rollback();
			// currentSession().beginTransaction();
			logger.error("Error, rolling back!" + e);
			throw new HistoDatabaseException(entity);
		}
	}
}