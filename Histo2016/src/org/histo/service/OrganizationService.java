package org.histo.service;

import java.util.List;

import org.histo.model.Contact;
import org.histo.model.Organization;
import org.histo.model.Person;

public interface OrganizationService {

	/**
	 * Adds a new organization to the database
	 * 
	 * @param name
	 * @param contact
	 * @return
	 */
	Organization addOrganization(String name, Contact contact);

	/**
	 * Adds a new organization to the database
	 * 
	 * @param organization
	 * @return
	 */
	Organization addOrganization(Organization organization);

	/**
	 * Adds a person to an organization
	 * 
	 * @param organization
	 * @param person
	 */
	void addPerson(Organization organization, Person person);

	/**
	 * Removes a persons from an organization
	 * 
	 * @param organization
	 * @param person
	 * @return
	 */
	boolean removePerson(Organization organization, Person person);

	/**
	 * Removes a person from an organization, saving is optional
	 * 
	 * @param organization
	 * @param person
	 * @param save
	 * @return
	 */
	boolean removePerson(Organization organization, Person person, boolean save);

	/**
	 * Checks the database if organizations exist. If organization is present it
	 * will be replaced in the list, otherwise it will be stored in the database.
	 * 
	 * @param organizations
	 */
	void synchronizeOrganizations(List<Organization> organizations);

}