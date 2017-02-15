package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.histo.config.enums.StaticList;
import org.histo.model.ListItem;
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
	
	private static Logger logger = Logger.getLogger("org.histo");
	
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

	public List<ListItem> getAllStaticListItems(StaticList list) {
		return getAllStaticListItems(list, false);
	}
	
	@SuppressWarnings("unchecked")
	public List<ListItem> getAllStaticListItems(StaticList list, boolean archived) {
		logger.debug("Searching for " + list.toString() + " in Database. " + (archived ? " Showing archived items." : " Showing none archived items."));
		DetachedCriteria query = DetachedCriteria.forClass(ListItem.class, "listItem");
		query.addOrder(Order.asc("indexInList"));
		query.add(Restrictions.eq("listType", list));
		query.add(Restrictions.eq("archived", archived));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}
}
