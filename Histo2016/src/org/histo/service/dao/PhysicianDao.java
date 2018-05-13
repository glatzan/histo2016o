package org.histo.service.dao;

import java.util.List;

import org.histo.config.enums.ContactRole;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.service.dao.impl.PhysicianDaoImpl;

public interface PhysicianDao extends GenericDao<Physician, Long> {

	/**
	 * Returns a list of all physicians which are associated with the given role.
	 * 
	 * @param role
	 * @param irgnoreArchived
	 * @return
	 */
	List<Physician> list(ContactRole role, boolean irgnoreArchived);

	/**
	 * Returns a list of all physicians which are associated with at least one given
	 * role.
	 * 
	 * @param roles
	 * @param irgnoreArchived
	 * @return
	 */
	List<Physician> list(List<ContactRole> roles, boolean irgnoreArchived);

	/**
	 * Returns a list of all physicians which are associated with at least one given
	 * role.
	 * 
	 * @param roles
	 * @param irgnoreArchived
	 * @return
	 */
	List<Physician> list(ContactRole[] roles, boolean irgnoreArchived);

	/**
	 * Returns a list of all physicians which are associated with at least one given
	 * role.
	 * 
	 * @param roles
	 * @param irgnoreArchived
	 * @param sortOrder
	 * @return
	 */
	List<Physician> list(ContactRole[] roles, boolean irgnoreArchived, PhysicianSortOrder sortOrder);

	/**
	 * Returns a list of all physicians which are associated with at least one given
	 * role.
	 * 
	 * @param roles
	 * @param irgnoreArchived
	 * @param sortOrder
	 * @return
	 */
	List<Physician> list(List<ContactRole> roles, boolean irgnoreArchived, PhysicianSortOrder sortOrder);

	/**
	 * Returns a physician with the given uui.
	 * 
	 * @param uid
	 * @return
	 */
	Physician find(String uid);

	/**
	 * Returns a physician with the given person.
	 * 
	 * @param person
	 * @return
	 */
	Physician find(Person person);

	/**
	 * Sort order for physician find method
	 * 
	 * @author andi
	 *
	 */
	public enum PhysicianSortOrder {
		NAME, PRIORITY, ID;
	}
}