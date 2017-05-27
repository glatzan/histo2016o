package org.histo.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.FavouriteList;
import org.histo.model.FavouriteListItem;
import org.histo.model.patient.Task;
import org.histo.util.StreamUtils;
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

	@Autowired
	private PatientDao patientDao;

	public FavouriteList getFavouriteList(long id, boolean initialized) {
		FavouriteList favList = genericDAO.get(FavouriteList.class, id);

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

	public void addTaskToList(Task task, PredefinedFavouriteList predefinedFavouriteList)
			throws CustomDatabaseInconsistentVersionException {
		genericDAO.refresh(task);
		addTaskToList(task, getFavouriteList(predefinedFavouriteList.getId(), true));
	}

	public void addTaskToList(Task task, FavouriteList favouriteList)
			throws CustomDatabaseInconsistentVersionException {

		// list should not contain the task
		if (favouriteList.getItems().stream().noneMatch(p -> p.getId() == task.getId())) {
			FavouriteListItem favItem = new FavouriteListItem(task);
			// saving new fav item
			genericDAO.saveDataRollbackSave(favItem);
			favouriteList.getItems().add(favItem);
			// saving favlist
			genericDAO.saveDataRollbackSave(favouriteList);
		} else {
			logger.debug("List already contains task");
		}

		// adding to task if task is not member of this list
		if (task.getFavouriteLists().stream().noneMatch(p -> p.getId() == favouriteList.getId())) {
			task.getFavouriteLists().add(favouriteList);
			patientDao.savePatientAssociatedDataFailSave(task, "log.patient.task.favouriteList.added",
					new Object[] { favouriteList.toString() });
		} else
			logger.debug("Task alread contains list");
	}
	
	public void removeTaskFromList(Task task, PredefinedFavouriteList[] predefinedFavouriteLists) throws CustomDatabaseInconsistentVersionException{
		for (PredefinedFavouriteList predefinedFavouriteList : predefinedFavouriteLists) {
			if(task.isListedInFavouriteList(predefinedFavouriteList)){
				removeTaskFromList(task, predefinedFavouriteList);
			}
		}
	}

	public void removeTaskFromList(Task task, PredefinedFavouriteList predefinedFavouriteList)
			throws CustomDatabaseInconsistentVersionException {
		genericDAO.refresh(task);
		removeTaskFromList(task, getFavouriteList(predefinedFavouriteList.getId(), true));
	}

	public void removeTaskFromList(Task task, FavouriteList favouriteList) throws CustomDatabaseInconsistentVersionException {

		try {
			// searching for item to remove
			FavouriteListItem itemToRemove = favouriteList.getItems().stream().filter(p -> p.getId() == task.getId())
					.collect(StreamUtils.singletonCollector());

			favouriteList.getItems().remove(itemToRemove);
			// saving new fav item
			genericDAO.saveDataRollbackSave(favouriteList);
		} catch (IllegalStateException e) {
			// no item found
			logger.debug("Can not remove from favourite list, " + favouriteList.getName() + " not in list");
		}

		try {
			FavouriteList listToRemove = task.getFavouriteLists().stream()
					.filter(p -> p.getId() == favouriteList.getId()).collect(StreamUtils.singletonCollector());

			task.getFavouriteLists().remove(listToRemove);
			
			logger.debug("Removing favourite list from task");

			// saving new fav item
			genericDAO.saveDataRollbackSave(task);
		} catch (IllegalStateException e) {
			// no item found
			logger.debug("Can not remove from favourite list, " + favouriteList.getName() + " not in list");
		}
		// TODO Delete FavouriteListItem?
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
