package org.histo.service;

import java.io.IOException;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.histo.action.handler.GlobalSettings;
import org.histo.adaptors.LdapHandler;
import org.histo.dao.GenericDAO;
import org.histo.dao.OrganizationDAO;
import org.histo.model.Physician;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Service
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

	public void updatePhysicianDataFromLdap(Physician physician) {

		Physician ldapPhysician = getPhysicianFromLdap(physician.getUid());
		if (ldapPhysician != null)
			physician.copyIntoObject(ldapPhysician);

		organizationDAO.synchronizeOrganizations(physician.getPerson().getOrganizsations());

		genericDAO.save(physician, "log.userSettings.update", new Object[] { physician.toString() });

	}

}
