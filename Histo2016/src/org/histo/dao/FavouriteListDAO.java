package org.histo.dao;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.histo.model.BioBank;
import org.histo.model.FavouriteList;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope("session")
public class FavouriteListDAO extends AbstractDAO {

//	@Autowired
//	private GenericDAO genericDAO;
//
//	public FavouriteList getFavouriteListById(long id) {
//		FavouriteList favList = getSession().get(FavouriteList.class, id);
//		return favList;
//	}
//
//	public List<Patient> getPatientsByTasksInLists(long id) {
//		FavouriteList favList = getFavouriteListById(id);
//
//		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");
//		query.add(Restrictions.in("patient.task", favList.getTasks()));
//		query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
//
//		return (List<Patient>) query.getExecutableCriteria(getSession()).list();
//	}
//
//	/**
//	 * Saves a favourtie List within a new Session
//	 * 
//	 * @param favouriteList
//	 * @param resourcesKey
//	 * @param resourcesKeyInsert
//	 * @return
//	 */
//	public boolean saveFavouriteList(FavouriteList favouriteList, String resourcesKey, Object[] resourcesKeyInsert) {
//		if (getSession().getTransaction().isActive()) {
//			getSession().getTransaction().commit();
//		}
//
//		getSession().beginTransaction();
//
//		if (genericDAO.saveDataRollbackSave(favouriteList, resourcesKey, resourcesKeyInsert)) {
//			getSession().getTransaction().commit();
//			return true;
//		}
//
//		return false;
//
//	}
//
//	public List<FavouriteList> getAssociatedLists(Task task) {
//
//		DetachedCriteria query = DetachedCriteria.forClass(FavouriteList.class, "lists");
//		query.add(Restrictions.eq("lists.task", task));
//		query.createAlias("lists.tasks", "_tasks");
//		query.add(Restrictions.ge("_tasks.id", task.getId()));
//		query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
//
//		List<FavouriteList> result = query.getExecutableCriteria(getSession()).list();
//
//		return result;
//	}
}
