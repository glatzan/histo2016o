package org.histo.service;

import java.io.IOException;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.histo.action.handler.GlobalSettings;
import org.histo.adaptors.LdapHandler;
import org.histo.config.ResourceBundle;
import org.histo.dao.GenericDAO;
import org.histo.dao.OrganizationDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.model.Physician;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Service
@Scope("session")
@Getter
@Setter
public class PhysicianService {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private OrganizationDAO organizationDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PhysicianDAO physicianDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	/**
	 * Returns a physician obtained from clinic backend
	 * 
	 * @param name
	 * @return
	 */
	public Physician getPhysicianFromLdap(String name) {
		LdapHandler ladpHandler = globalSettings.getLdapHandler();
		DirContext connection;
		Physician physician = null;

		try {
			connection = ladpHandler.openConnection();
			physician = ladpHandler.getPhyscican(connection, name);
			ladpHandler.closeConnection(connection);
		} catch (IOException | NamingException e) {
		}

		return physician;
	}

	/**
	 * Gets new physician data from clinic backend and updates the given local
	 * physician object
	 * 
	 * @param physician
	 */
	public void updatePhysicianDataFromLdap(Physician physician) {

		Physician ldapPhysician = getPhysicianFromLdap(physician.getUid());

		if (ldapPhysician != null) {
			// setting update to true, otherwise nothing will happen
			physician.getPerson().setAutoUpdate(true);
			physicianDAO.mergePhysicians(ldapPhysician, physician);
		}
	}
	
}
