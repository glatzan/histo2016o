package org.histo.service;

import org.histo.model.Physician;

public interface PhysicianService {

	/**
	 * Checks if physician is saved in database, if so the saved physician will be
	 * updated, otherwise a new physician will be created.
	 * 
	 * @param physician
	 * @return
	 */
	Physician addOrMergePhysician(Physician physician);

	/**
	 * Merges two physicians an updates their organizations
	 * 
	 * @param source
	 * @param destination
	 */
	void mergePhysicians(Physician source, Physician destination);

	/**
	 * Archives or restores a physician.
	 * 
	 * @param physician
	 * @param archive
	 */
	void archivePhysician(Physician physician, boolean archive);

	/**
	 * Returns a physician obtained from pdv
	 * 
	 * @param name
	 * @return
	 */
	Physician ldapFind(String name);

	/**
	 * Gets new physician data from clinic backend and updates the given local
	 * physician object
	 * 
	 * @param physician
	 */
	void ldapUpdate(Physician physician);

}