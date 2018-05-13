package org.histo.service;

import org.histo.model.Physician;

public interface UserService {

	/**
	 * Creates an new user, if a physician with the given username exist they will
	 * be merged.
	 * 
	 * @param physician
	 * @return
	 */
	boolean addUser(Physician physician);

}