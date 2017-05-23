package org.histo.dao;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.DetachedCriteria;
import org.histo.model.FavouriteList;
import org.histo.model.FavouriteListItem;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope("session")
public class FavouriteListDAO extends AbstractDAO {

	@Autowired
	private GenericDAO genericDAO;

	public FavouriteList getFavouriteList(long id, boolean initialized) {
		FavouriteList favList = getSession().get(FavouriteList.class, id);

		if (initialized) {
			Hibernate.initialize(favList.getOwner());
			Hibernate.initialize(favList.getItems());
		}

		return favList;
	}

	@SuppressWarnings("unchecked")
	public List<FavouriteList> getAllFavouriteLists() {
		DetachedCriteria query = DetachedCriteria.forClass(FavouriteList.class, "favList");
		query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

		return (List<FavouriteList>) query.getExecutableCriteria(getSession()).list();
	}

	public boolean createFavouriteList(FavouriteList list) {
		if (!genericDAO.saveDataRollbackSave(list))
			return false;
		return true;
	}

	public boolean addTaskToList(Task task, long id) {
		FavouriteList favouriteList = getFavouriteList(id, true);
		return addTaskToList(task, favouriteList);
	}

	public boolean addTaskToList(Task task, FavouriteList favouriteList) {

		FavouriteListItem contains = getListItemFromList(favouriteList, task.getId());

		if (contains == null) {
			FavouriteListItem favItem = new FavouriteListItem(task);

			// saving new fav item
			if (!genericDAO.saveDataRollbackSave(favItem))
				return false;

			favouriteList.getItems().add(favItem);

			// saving favlist
			if (!genericDAO.saveDataRollbackSave(favouriteList))
				return false;
		} else
			logger.debug("List already contains task");

		if (!task.getFavouriteLists().contains(favouriteList)) {
			task.getFavouriteLists().add(favouriteList);
			if (!genericDAO.saveDataRollbackSave(task))
				return false;
		}else
			logger.debug("Task alread contains list");
			
		return true;
	}

	public boolean removeTaskFromList(Task task, long id) {
		FavouriteList favouriteList = getFavouriteList(id, true);
		return removeTaskFromList(task, favouriteList);
	}

	public boolean removeTaskFromList(Task task, FavouriteList favouriteList) {

		FavouriteListItem itemToRemove = getListItemFromList(favouriteList, task.getId());

		if (itemToRemove != null) {

			favouriteList.getItems().remove(itemToRemove);

			// saving new fav item
			if (!genericDAO.saveDataRollbackSave(favouriteList))
				return false;
		} else {
			logger.debug("Can not remove from favourite list, not in list");
		}

		if (task.getFavouriteLists().contains(favouriteList)) {
			logger.debug("Removing favourite list from task");
			task.getFavouriteLists().remove(favouriteList);

			// saving new fav item
			if (!genericDAO.saveDataRollbackSave(task))
				return false;
		} else {
			logger.debug("Can not remove from task, favourite list not associated.");
		}

		// TODO Delete FavouriteListItem?

		return true;
	}

	private FavouriteListItem getListItemFromList(FavouriteList favouriteList, long taskId) {
		for (FavouriteListItem item : favouriteList.getItems()) {
			if (item.getTask().getId() == taskId) {
				return item;
			}
		}
		return null;
	}
}

// public List<Patient> getPatientsByTasksInLists(long id) {
// FavouriteList favList = getFavouriteListById(id);
//
// DetachedCriteria query = DetachedCriteria.forClass(Patient.class,
// "patient");
// query.add(Restrictions.in("patient.task", favList.getTasks()));
// query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
//
// return (List<Patient>) query.getExecutableCriteria(getSession()).list();
// }
//
// /**
// * Saves a favourtie List within a new Session
// *
// * @param favouriteList
// * @param resourcesKey
// * @param resourcesKeyInsert
// * @return
// */
// public boolean saveFavouriteList(FavouriteList favouriteList, String
// resourcesKey, Object[] resourcesKeyInsert) {
// if (getSession().getTransaction().isActive()) {
// getSession().getTransaction().commit();
// }
//
// getSession().beginTransaction();
//
// if (genericDAO.saveDataRollbackSave(favouriteList, resourcesKey,
// resourcesKeyInsert)) {
// getSession().getTransaction().commit();
// return true;
// }
//
// return false;
//
// }
//
// public List<FavouriteList> getAssociatedLists(Task task) {
//
// DetachedCriteria query = DetachedCriteria.forClass(FavouriteList.class,
// "lists");
// query.add(Restrictions.eq("lists.task", task));
// query.createAlias("lists.tasks", "_tasks");
// query.add(Restrictions.ge("_tasks.id", task.getId()));
// query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
//
// List<FavouriteList> result =
// query.getExecutableCriteria(getSession()).list();
//
// return result;
// }
