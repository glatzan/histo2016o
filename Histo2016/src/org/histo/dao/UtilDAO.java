package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.DiagnosisPreset;
import org.histo.model.MaterialPreset;
import org.histo.model.Physician;
import org.histo.model.interfaces.HasDataList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(noRollbackFor = Exception.class)
@Scope(value = "session")
public class UtilDAO extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = -2446285129518473844L;

	@Autowired
	private GenericDAO genericDAO;

	/**
	 * Initializes a datalist for an object
	 * 
	 * @param dataList
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public HasDataList initializeDataList(HasDataList dataList) throws CustomDatabaseInconsistentVersionException {
		dataList = genericDAO.refresh(dataList);
		Hibernate.initialize(dataList.getAttachedPdfs());
		return dataList;
	}

	/**
	 * Inits the stainingprotoypes of a stainingprototypeList
	 * 
	 * @param stainingPrototypeLists
	 */
	public void initStainingPrototypeList(List<MaterialPreset> stainingPrototypeLists) {
		for (MaterialPreset stainingPrototypeList : stainingPrototypeLists) {
			initStainingPrototypeList(stainingPrototypeList);
		}
	}

	public void initStainingPrototypeList(MaterialPreset stainingPrototypeLists) {
		Hibernate.initialize(stainingPrototypeLists.getStainingPrototypes());
	}

	/**
	 * Gets all standardDiagnoses from the database
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<DiagnosisPreset> getAllDiagnosisPrototypes() {

		DetachedCriteria query = DetachedCriteria.forClass(DiagnosisPreset.class, "diagnosis");
		query.addOrder(Order.asc("diagnosis.indexInList"));

		List<DiagnosisPreset> result = query.getExecutableCriteria(getSession()).list();
		return result;
	}

	// @SuppressWarnings("unchecked")
	// public List<History> getCurrentHistory(int entryCount) {
	// Criteria c = getSession().createCriteria(History.class, "history");
	// c.addOrder(Order.desc("date"));
	// c.setMaxResults(entryCount);
	// c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
	// return (List<History>) c.list();
	// }
	//
	// @SuppressWarnings("unchecked")
	// public List<History> getCurrentHistoryForPatient(int entryCount, Patient
	// patient) {
	// Criteria c = getSession().createCriteria(History.class, "history");
	// c.add(Restrictions.eq("patient.id", patient.getId()));
	// c.addOrder(Order.desc("date"));
	// c.setMaxResults(entryCount);
	// c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
	// return (List<History>) c.list();
	// }

}