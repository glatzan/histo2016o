package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.histo.model.MaterialPreset;
import org.histo.model.StainingPrototype;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class SettingsDAO extends AbstractDAO implements Serializable {
	
	private static final long serialVersionUID = -9035879247004620693L;

	@SuppressWarnings("unchecked")
	public List<StainingPrototype> getAllStainingPrototypes() {
		DetachedCriteria query = DetachedCriteria.forClass(StainingPrototype.class, "sPrototype");
		query.addOrder(Order.asc("indexInList"));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}
	
	@SuppressWarnings("unchecked")
	public List<MaterialPreset> getAllMaterialPresets() {
		DetachedCriteria query = DetachedCriteria.forClass(MaterialPreset.class, "mPresets");
		query.addOrder(Order.asc("indexInList"));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}
}
