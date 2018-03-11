package org.histo.dao;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.DiagnosisPreset;
import org.histo.model.ListItem;
import org.histo.model.MaterialPreset;
import org.histo.model.StainingPrototype;
import org.histo.model.StainingPrototype.StainingType;
import org.histo.util.dataList.HasDataList;
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
		dataList = genericDAO.reattach(dataList);
		Hibernate.initialize(dataList.getAttachedPdfs());
		return dataList;
	}

	/**
	 * Returns all staining prototypes
	 * 
	 * @return
	 */
	public List<StainingPrototype> getStainingPrototypes() {
		return getStainingPrototypes(null);
	}

	/**
	 * Returns a list with staining prototypes of the given type
	 * 
	 * @param types
	 * @return
	 */
	public List<StainingPrototype> getStainingPrototypes(StainingType[] types) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<StainingPrototype> criteria = qb.createQuery(StainingPrototype.class);
		Root<StainingPrototype> root = criteria.from(StainingPrototype.class);
		criteria.select(root);

		if (types != null) {
			Expression<Long> typeColumn = root.get("type");
			Predicate listCodition = typeColumn.in(Arrays.asList(types));

			criteria.where(listCodition);
		}

		criteria.distinct(true);

		List<StainingPrototype> result = getSession().createQuery(criteria).getResultList();

		return result;
	}

	@SuppressWarnings("unchecked")
	public List<StainingPrototype> getAllStainingPrototypes() {
		DetachedCriteria query = DetachedCriteria.forClass(StainingPrototype.class, "sPrototype");
		query.addOrder(Order.asc("indexInList"));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Gets all standardDiagnoses from the database
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<DiagnosisPreset> getAllDiagnosisPrototypes() {

		DetachedCriteria query = DetachedCriteria.forClass(DiagnosisPreset.class, "diagnosis");
		query.addOrder(Order.asc("indexInList"));

		List<DiagnosisPreset> result = query.getExecutableCriteria(getSession()).list();
		return result;
	}

	/**
	 * Returns a list with all available MaterialPresets.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<MaterialPreset> getAllMaterialPresets(boolean initialize) {
		DetachedCriteria query = DetachedCriteria.forClass(MaterialPreset.class, "mPresets");
		query.addOrder(Order.asc("indexInList"));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		List<MaterialPreset> result = query.getExecutableCriteria(getSession()).list();

		if (initialize && result != null) {
			result.stream().forEach(p -> initMaterialPreset(p));
		}

		return query.getExecutableCriteria(getSession()).list();
	}

	/**
	 * Initializes a MaterialPrest, stainings are fetched lazy
	 * 
	 * @param stainingPrototypeLists
	 */
	public void initMaterialPreset(MaterialPreset stainingPrototypeLists) {
		Hibernate.initialize(stainingPrototypeLists.getStainingPrototypes());
	}

	public List<ListItem> getAllStaticListItems(ListItem.StaticList list) {
		return getAllStaticListItems(list, false);
	}

	@SuppressWarnings("unchecked")
	public List<ListItem> getAllStaticListItems(ListItem.StaticList list, boolean archived) {
		logger.debug("Searching for " + list.toString() + " in Database. "
				+ (archived ? " Showing archived items." : " Showing none archived items."));
		DetachedCriteria query = DetachedCriteria.forClass(ListItem.class, "listItem");
		query.addOrder(Order.asc("indexInList"));
		query.add(Restrictions.eq("listType", list));
		query.add(Restrictions.eq("archived", archived));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}

}