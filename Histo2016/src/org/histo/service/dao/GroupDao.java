package org.histo.service.dao;

import java.util.List;

import org.histo.model.user.HistoGroup;

public interface GroupDao extends GenericDao<HistoGroup, Long> {

	/**
	 * Reattaches and Initializes the given group, loads users
	 * 
	 * @param group
	 *            Group
	 * @param loadSettings
	 *            initializes the group settings
	 * @return
	 */
	HistoGroup initialize(HistoGroup group);

	/**
	 * Reattaches and Initializes the given group
	 * 
	 * @param group
	 *            Group
	 * @param loadSettings
	 *            initializes the group settings
	 * @return
	 */
	HistoGroup initialize(HistoGroup group, boolean loadSettings);

	/**
	 * Returns a list of groups
	 * 
	 * @param irgnoreArchived
	 *            ignores archived groups
	 * @return
	 */
	List<HistoGroup> list(boolean irgnoreArchived);

	/**
	 * Returns a group with loaded settings by its id
	 * 
	 * @param id
	 *            Group id
	 * @return
	 */
	HistoGroup find(long id);

	/**
	 * Returns a group by its id
	 * 
	 * @param id
	 *            Group id
	 * @param loadSettings
	 *            loads the group settings as well
	 * @return
	 */
	HistoGroup find(long id, boolean loadSettings);

}