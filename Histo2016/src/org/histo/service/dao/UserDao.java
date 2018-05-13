package org.histo.service.dao;

import java.util.List;

import org.histo.model.user.HistoUser;

public interface UserDao extends GenericDao<HistoUser, Long> {

	/**
	 * Gets a list of users.
	 */
	List<HistoUser> list(boolean irgnoreArchived);

	/**
	 * Returns a user with the given user name.
	 */
	HistoUser find(String name);

	/**
	 * Returns users of the given group.
	 */
	List<HistoUser> findListByGroup(long id);

}