package org.histo.dao;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.DetachedCriteria;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.FavouriteList;
import org.histo.model.FavouriteListItem;
import org.histo.model.HistoUser;
import org.histo.model.Physician;
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

	public FavouriteList getFavouriteList(long id, boolean initialized) {
		FavouriteList favList = genericDAO.get(FavouriteList.class, id);

		if (initialized) {
			Hibernate.initialize(favList.getOwner());
			Hibernate.initialize(favList.getItems());
		}

		return favList;
	}

	public FavouriteList initFavouriteList(FavouriteList favList) {
		genericDAO.reattach(favList);
		Hibernate.initialize(favList.getOwner());
		Hibernate.initialize(favList.getItems());
		return favList;
	}

	public List<FavouriteList> getFavouriteListsOfUser(HistoUser user) {
		// Create CriteriaBuilder
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<FavouriteList> criteria = qb.createQuery(FavouriteList.class);
		Root<FavouriteList> root = criteria.from(FavouriteList.class);
		criteria.select(root);

		criteria.where(qb.equal(root.get("owner"), user));
		criteria.distinct(true);

		List<FavouriteList> favouriteLists = getSession().createQuery(criteria).getResultList();

		return favouriteLists;
	}

	@SuppressWarnings("unchecked")
	public List<FavouriteList> getAllFavouriteLists() {
		DetachedCriteria query = DetachedCriteria.forClass(FavouriteList.class, "favList");
		query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

		return (List<FavouriteList>) query.getExecutableCriteria(getSession()).list();
	}

	public void addTaskToList(Task task, PredefinedFavouriteList predefinedFavouriteList)
			throws CustomDatabaseInconsistentVersionException {
		addTaskToList(task, predefinedFavouriteList, true);
	}

	public void addTaskToList(Task task, PredefinedFavouriteList predefinedFavouriteList, boolean refresh)
			throws CustomDatabaseInconsistentVersionException {

		reattach(task);
		reattach(task.getParent());

		addTaskToList(task, getFavouriteList(predefinedFavouriteList.getId(), true));
	}

	public void addTaskToList(Task task, FavouriteList favouriteList)
			throws CustomDatabaseInconsistentVersionException {

		// list should not contain the task
		if (favouriteList.getItems().stream().noneMatch(p -> p.getTask().getId() == task.getId())) {
			logger.debug("Adding task (" + task.getTaskID() + ") to favourite lists (" + favouriteList.getName() + ")");
			FavouriteListItem favItem = new FavouriteListItem(favouriteList, task);
			// saving new fav item
			save(favItem);
			favouriteList.getItems().add(favItem);
			// saving favlist
			save(favouriteList);
		} else {
			logger.debug("List (" + favouriteList.getName() + ") already contains task (" + task.getTaskID() + ")");
		}

		// adding to task if task is not member of this list
		if (task.getFavouriteLists().stream().noneMatch(p -> p.getId() == favouriteList.getId())) {

			logger.debug("Adding favourite list(" + favouriteList.getName() + ") to task (" + task.getTaskID() + ")");

			task.getFavouriteLists().add(favouriteList);
			genericDAO.savePatientData(task, "log.patient.task.favouriteList.added",
					new Object[] { task.getTaskID().toString(), favouriteList.toString() });
		} else
			logger.debug("Task (" + task.getTaskID() + ") alread contains list (" + favouriteList.getName() + ")");
	}

	public void removeTaskFromList(Task task, PredefinedFavouriteList[] predefinedFavouriteLists)
			throws CustomDatabaseInconsistentVersionException {
		for (PredefinedFavouriteList predefinedFavouriteList : predefinedFavouriteLists) {
			removeTaskFromList(task, predefinedFavouriteList);
		}

	}

	public void removeTaskFromList(Task task, PredefinedFavouriteList predefinedFavouriteList)
			throws CustomDatabaseInconsistentVersionException {
		if (task.isListedInFavouriteList(predefinedFavouriteList)) {
			reattach(task);
			removeTaskFromList(task, getFavouriteList(predefinedFavouriteList.getId(), true));
		}
	}

	public void removeTaskFromList(Task task, FavouriteList favouriteList)
			throws CustomDatabaseInconsistentVersionException {
		System.out.println(getSession().hashCode() + "!!");
		try {
			logger.debug(
					"Removing task (" + task.getTaskID() + ") from favourite lists (" + favouriteList.getName() + ")");

			// searching for item to remove
			FavouriteListItem itemToRemove = favouriteList.getItems().stream()
					.filter(p -> p.getTask().getId() == task.getId()).collect(StreamUtils.singletonCollector());

			favouriteList.getItems().remove(itemToRemove);
			// saving new fav item
			save(favouriteList);
			genericDAO.deletePatientData(itemToRemove, task.getPatient(), "log.patient.task.favouriteList.removed",
					task.getTaskID().toString(), favouriteList.toString());
		} catch (IllegalStateException e) {
			// no item found
			logger.debug("Can not remove task (" + task.getTaskID() + ") from favourite list ("
					+ favouriteList.getName() + "), not in list");
		}

		try {
			logger.debug(
					"Removing favourite list(" + favouriteList.getName() + ") from task (" + task.getTaskID() + ")");

			FavouriteList listToRemove = task.getFavouriteLists().stream()
					.filter(p -> p.getId() == favouriteList.getId()).collect(StreamUtils.singletonCollector());

			task.getFavouriteLists().remove(listToRemove);

			// saving new fav item
			save(task);
		} catch (IllegalStateException e) {
			// no item found
			logger.debug("Can not remove favourite list(" + favouriteList.getName() + ") from task (" + task.getTaskID()
					+ "), not listed ");
		}
		// TODO Delete FavouriteListItem?
	}

	public void removeTaskFromAllLists(Task task) {
		// removing from favouriteLists
		while (task.getFavouriteLists().size() > 0) {
			System.out.println(getSession().hashCode() + "fav");
			removeTaskFromList(task, getFavouriteList(task.getFavouriteLists().get(0).getId(), true));
		}
	}
}
