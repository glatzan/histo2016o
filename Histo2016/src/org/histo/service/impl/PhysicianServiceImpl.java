package org.histo.service.impl;

import java.io.IOException;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.histo.action.handler.GlobalSettings;
import org.histo.adaptors.LdapHandler;
import org.histo.model.Physician;
import org.histo.service.OrganizationService;
import org.histo.service.PhysicianService;
import org.histo.service.dao.PhysicianDao;
import org.histo.util.CopySettingsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("physicianService")
@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
public class PhysicianServiceImpl extends AbstractService implements PhysicianService {

	@Autowired
	private PhysicianDao physicianDao;

	@Autowired
	private OrganizationService organizationService;

	@Autowired
	private GlobalSettings globalSettings;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.PhysicianService#addOrMergePhysician(org.histo.model.
	 * Physician)
	 */
	@Override
	public Physician addOrMergePhysician(Physician physician) {
		// if the physician was added as surgeon the useracc an the
		// physician will be merged
		Physician physicianFromDatabase = physicianDao.find(physician.getUid());

		// undating the foud physician
		if (physicianFromDatabase != null) {
			logger.info("Physician already in database " + physician.getPerson().getFullName());
			mergePhysicians(physician, physicianFromDatabase);

			physicianFromDatabase.setArchived(false);

			// overwriting roles
			physicianFromDatabase.setAssociatedRoles(physician.getAssociatedRoles());

			physicianDao.save(physicianFromDatabase, resourceBundle.get("log.settings.physician.ldap.update",
					physicianFromDatabase.getPerson().getFullName()));

			physician = physicianFromDatabase;

		} else {
			logger.info("Creating new phyisician " + physician.getPerson().getFullName());

			// removing physicians temp id
			// TODO crate container object
			physician.setId(0);

			organizationService.synchronizeOrganizations(physician.getPerson().getOrganizsations());

			physicianDao.save(physician,
					resourceBundle.get("log.settings.physician.ldap.save", physician.getPerson().getFullName()));
		}

		return physician;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.histo.service.PhysicianService#mergePhysicians(org.histo.model.Physician,
	 * org.histo.model.Physician)
	 */
	@Override
	public void mergePhysicians(Physician source, Physician destination) {
		CopySettingsUtil.copyPhysicianData(source, destination, false);
		organizationService.synchronizeOrganizations(destination.getPerson().getOrganizsations());
		physicianDao.save(destination, "user.role.settings.update", destination.toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.PhysicianService#archivePhysician(org.histo.model.
	 * Physician, boolean)
	 */
	@Override
	public void archivePhysician(Physician physician, boolean archive) {
		physician.setArchived(archive);
		physicianDao.save(physician,
				resourceBundle.get(archive ? "log.settings.physician.archived" : "log.settings.physician.archived.undo",
						physician.getPerson().getFullName()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.PhysicianService#ldapFind(java.lang.String)
	 */
	@Override
	public Physician ldapFind(String name) {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.PhysicianService#ldapUpdate(org.histo.model.Physician)
	 */
	@Override
	public void ldapUpdate(Physician physician) {

		Physician ldapPhysician = ldapFind(physician.getUid());

		if (ldapPhysician != null) {
			// setting update to true, otherwise nothing will happen
			physician.getPerson().setAutoUpdate(true);
			mergePhysicians(ldapPhysician, physician);
		}
	}
}
