package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.histo.model.DiagnosisPreset;
import org.histo.model.MaterialPreset;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Patient;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class HelperDAO extends AbstractDAO implements Serializable {

	public List<StainingPrototype> getAllStainings() {
		return (List<StainingPrototype>) getSession().createCriteria(StainingPrototype.class).list();
	}

	public List<MaterialPreset> getAllStainingLists() {
		return (List<MaterialPreset>) getSession().createCriteria(MaterialPreset.class)
				.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY).list();
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
		return getSession().createCriteria(DiagnosisPreset.class).list();
	}

//	@SuppressWarnings("unchecked")
//	public List<History> getCurrentHistory(int entryCount) {
//		Criteria c = getSession().createCriteria(History.class, "history");
//		c.addOrder(Order.desc("date"));
//		c.setMaxResults(entryCount);
//		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
//		return (List<History>) c.list();
//	}
//
//	@SuppressWarnings("unchecked")
//	public List<History> getCurrentHistoryForPatient(int entryCount, Patient patient) {
//		Criteria c = getSession().createCriteria(History.class, "history");
//		c.add(Restrictions.eq("patient.id", patient.getId()));
//		c.addOrder(Order.desc("date"));
//		c.setMaxResults(entryCount);
//		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
//		return (List<History>) c.list();
//	}

}