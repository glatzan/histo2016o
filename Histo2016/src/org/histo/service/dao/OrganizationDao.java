package org.histo.service.dao;

import java.util.List;

import org.histo.model.Organization;

public interface OrganizationDao extends GenericDao<Organization, Long> {

	/**
	 * Initializes an organization,
	 * @param organization
	 */
	public void initialize(Organization organization);

	/**
	 * Initializes an organization,
	 * @param organization
	 */
	public void initialize(Organization organization, boolean loadPersons);

	/**
	 * Finds an organization by the given name
	 * 
	 * @param name
	 * @return
	 */
	Organization find(String name);

	/**
	 * Lists all organization, ordered by id, persons not loaded.
	 * 
	 * @param irgnoreArchived
	 * @return
	 */
	List<Organization> list(boolean irgnoreArchived);

	/**
	 * Lists all organizations
	 * 
	 * @param orderById
	 * @param loadPersons
	 * @param irgnoreArchived
	 * @return
	 */
	List<Organization> list(boolean orderById, boolean loadPersons, boolean irgnoreArchived);

}