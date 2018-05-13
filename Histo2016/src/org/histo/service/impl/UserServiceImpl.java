package org.histo.service.impl;

import org.histo.config.enums.ContactRole;
import org.histo.model.Physician;
import org.histo.model.user.HistoUser;
import org.histo.service.PhysicianService;
import org.histo.service.UserService;
import org.histo.service.dao.PhysicianDao;
import org.histo.service.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("userService")
@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
public class UserServiceImpl extends AbstractService implements UserService {

	@Autowired
	private UserDao userDao;

	@Autowired
	private PhysicianService physicianService;

	/* (non-Javadoc)
	 * @see org.histo.service.UserService#addUser(org.histo.model.Physician)
	 */
	@Override
	public boolean addUser(Physician physician) {

		if (physician == null) {
			return false;
		}

		String userName = physician.getUid();

		// removing id from the list
		physician.setId(0);

		// checking if histouser exsists
		HistoUser histoUser = userDao.find(userName);

		if (histoUser == null) {
			logger.info("No User found, creating new HistoUser " + physician.getPerson().getFullName());
			histoUser = new HistoUser(userName);

			// saving or updating physician, also updating organizations
			physician = physicianService.addOrMergePhysician(physician);

			if (physician.getAssociatedRoles().size() == 0)
				physician.addAssociateRole(ContactRole.NONE);

			userDao.save(physician, "log.settings.physician.ldap.update", physician.toString());

			histoUser.setPhysician(physician);

		} else {
			physicianService.mergePhysicians(physician, histoUser.getPhysician());
		}

		userDao.save(histoUser, "user.role.settings.update", histoUser.toString());

		return true;
	}
}
