package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.PatientRollbackAble;
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

	public <C extends HasID & PatientRollbackAble<?>> C savePatientData(C object)
			throws CustomDatabaseInconsistentVersionException {
		return savePatientData(object, object, null);
	}

	public <C extends HasID & PatientRollbackAble<?>> C savePatientData(C object, String resourcesKey,
			Object... resourcesKeyInsert) throws CustomDatabaseInconsistentVersionException {
		return savePatientData(object, object, resourcesKey, resourcesKeyInsert);
	}

	public <C extends HasID> C savePatientData(C object, PatientRollbackAble<?> hasPatient, String resourcesKey,
			Object... resourcesKeyInsert) throws CustomDatabaseInconsistentVersionException {

		Object[] keyArr = new Object[resourcesKeyInsert.length + 1];
		keyArr[0] = hasPatient.getLogPath();
		for (int i = 0; i < resourcesKeyInsert.length; i++) {
			keyArr[i + 1] = resourcesKeyInsert[i];
		}

		// if failed false will be returned
		return save(object, resourcesKey, keyArr, hasPatient.getPatient());
	}

	public <C extends HasID & PatientRollbackAble<?>> C deletePatientData(C object, String resourcesKey,
			Object... resourcesKeyInsert) throws CustomDatabaseInconsistentVersionException {
		return deletePatientData(object, object, resourcesKey, resourcesKeyInsert);
	}

	public <C extends HasID> C deletePatientData(C object, PatientRollbackAble<?> hasPatient, String resourcesKey,
			Object... resourcesKeyInsert) throws CustomDatabaseInconsistentVersionException {

		Object[] keyArr = new Object[resourcesKeyInsert.length + 1];
		keyArr[0] = hasPatient.getLogPath();
		for (int i = 0; i < resourcesKeyInsert.length; i++) {
			keyArr[i + 1] = resourcesKeyInsert[i];
		}

		return delete(object, resourcesKey, keyArr, hasPatient.getPatient());
	}
}