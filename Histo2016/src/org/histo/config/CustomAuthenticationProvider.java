package org.histo.config;

import java.io.IOException;
import java.util.Collection;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.apache.log4j.Logger;
import org.histo.action.handler.GlobalSettings;
import org.histo.adaptors.LdapHandler;
import org.histo.config.enums.ContactRole;
import org.histo.dao.TransientDAO;
import org.histo.model.Contact;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoSettings;
import org.histo.model.user.HistoUser;
import org.histo.model.user.HistoGroup.AuthRole;
import org.histo.util.CopySettingsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

	private static Logger logger = Logger.getLogger("org.histo");

	public static String host = "ldap.ukl.uni-freiburg.de";
	public static String port = "389";
	public static String suffix = "dc=ukl,dc=uni-freiburg,dc=de";
	public static String base = "ou=people";

	@Autowired
	private GlobalSettings globalSettings;

	@Autowired
	private TransientDAO transientDAO;

	@Autowired
	protected ResourceBundle resourceBundle;

	@Override
	public Authentication authenticate(Authentication authentication) {
		String userName = authentication.getName().trim().toLowerCase();
		String password = authentication.getCredentials().toString().trim();

		try {

			if (globalSettings.getProgramSettings().isOffline()) {
				logger.info("LDAP login disabled");

				HistoUser histoUser = transientDAO.loadUserByName(userName);
				if (histoUser == null) {
					logger.info("No user found, creating new one");
					histoUser = new HistoUser(userName);

					HistoGroup group = transientDAO.getHistoGroup(HistoGroup.GROUP_GUEST_ID, true);
					histoUser.setGroup(group);
					histoUser.setSettings(new HistoSettings());
					// copy settings from group to user
					CopySettingsUtil.copyCrucialGroupSettings(histoUser, group, true);

				} else if (histoUser.getPhysician() == null) {
					histoUser.setPhysician(new Physician(new Person(new Contact())));
					histoUser.getPhysician().setUid(userName);
				}

				// throwing error if person is banned
				if(histoUser.getGroup().getAuthRole() == AuthRole.ROLE_NONEAUTH || histoUser.isArchived()) {
					throw new BadCredentialsException(resourceBundle.get("login.error.banned"));
				}

				histoUser.setLastLogin(System.currentTimeMillis());

				transientDAO.save(histoUser, "user.role.settings.update", new Object[] { histoUser.toString() });

				Collection<? extends GrantedAuthority> authorities = histoUser.getAuthorities();

				return new UsernamePasswordAuthenticationToken(histoUser, password, authorities);
			}

			LdapHandler ladpHandler = globalSettings.getLdapHandler();
			DirContext connection = ladpHandler.openConnection();

			Physician physician = ladpHandler.getPhyscican(connection, userName);

			if (physician != null) {
				String dn = physician.getDnObjectName() + "," + base + "," + suffix;

				ladpHandler.closeConnection(connection);

				logger.info("Physician found " + physician.getPerson().getFullName());

				// if now error was thrown auth was successful
				ladpHandler.checkPassword(dn, password);

				logger.info("Login successful " + physician.getPerson().getFullName());

				// checking if histouser exsists
				HistoUser histoUser = transientDAO.loadUserByName(userName);

				if (histoUser == null) {
					logger.info("Creating new HistoUser " + physician.getPerson().getFullName());
					histoUser = new HistoUser(userName);

					HistoGroup group = transientDAO.getHistoGroup(HistoGroup.GROUP_GUEST_ID, true);
					histoUser.setGroup(group);
					histoUser.setSettings(new HistoSettings());
					CopySettingsUtil.copyCrucialGroupSettings(histoUser, group, true);

					// if the physician was added as surgeon the useracc an the
					// physician will be merged
					Physician physicianFromDatabase = transientDAO.loadPhysicianByUID(userName);
					if (physicianFromDatabase != null) {
						histoUser.setPhysician(physicianFromDatabase);
						logger.info("Physician already in datanse " + physician.getPerson().getFullName());
					} else {
						// creating new physician an person
						histoUser.setPhysician(new Physician(new Person(new Contact())));
						histoUser.getPhysician().setUid(userName);
						// Default role for that physician
						histoUser.getPhysician().addAssociateRole(ContactRole.OTHER_PHYSICIAN);
					}
				}
				
				// throwing error if person is banned
				if(histoUser.getGroup().getAuthRole() == AuthRole.ROLE_NONEAUTH || histoUser.isArchived()) {
					throw new BadCredentialsException(resourceBundle.get("login.error.banned"));
				}

				histoUser.setLastLogin(System.currentTimeMillis());
				
				transientDAO.mergePhysicians(physician , histoUser.getPhysician());

				transientDAO.save(histoUser, "user.role.settings.update", new Object[] { histoUser.toString() });

				Collection<? extends GrantedAuthority> authorities = histoUser.getAuthorities();

				return new UsernamePasswordAuthenticationToken(histoUser, password, authorities);
			} else
				throw new BadCredentialsException(resourceBundle.get("login.error.text"));
		} catch (NamingException | IOException | AuthenticationException e) {
			// catch other exception an merge them into a bad credentials exception
			if(e instanceof BadCredentialsException)
				throw (BadCredentialsException)e;
			
			throw new BadCredentialsException(resourceBundle.get("login.error.text"));
		}

	}

	@Override
	public boolean supports(Class<? extends Object> authentication) {
		return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
	}
}