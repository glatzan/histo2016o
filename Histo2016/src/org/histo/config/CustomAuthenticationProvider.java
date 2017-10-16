package org.histo.config;

import java.io.IOException;
import java.util.Collection;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.apache.log4j.Logger;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Role;
import org.histo.dao.TransientDAO;
import org.histo.model.Contact;
import org.histo.model.HistoUser;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.settings.LdapHandler;
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

	@Override
	public Authentication authenticate(Authentication authentication) {
		String userName = authentication.getName().trim();
		String password = authentication.getCredentials().toString().trim();

		try {

			if (globalSettings.getProgramSettings().isOffline()) {
				logger.info("LDAP login disabled");

				HistoUser histoUser = transientDAO.loadUserByName(userName);
				if (histoUser == null) {
					logger.info("No user found, creating new one");
					histoUser = new HistoUser(userName, Role.USER);
				} else if (histoUser.getPhysician() == null) {
					histoUser.setPhysician(new Physician());
					histoUser.getPhysician().setPerson(new Person());

					// set role clinicalDoctor or clical personnel
					histoUser.getPhysician().setClinicEmployee(true);
				}

				histoUser.setLastLogin(System.currentTimeMillis());

				transientDAO.save(histoUser, "log.userSettings.update", new Object[] { histoUser.toString() });

				Collection<? extends GrantedAuthority> authorities = histoUser.getAuthorities();

				return new UsernamePasswordAuthenticationToken(histoUser, password, authorities);
			}

			LdapHandler ladpHandler = globalSettings.getLdapHandler();
			DirContext connection = ladpHandler.openConnection();

			Physician physician = ladpHandler.getPhyscican(connection,userName);

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
						histoUser.getPhysician().setClinicEmployee(true);
						// Default role for that physician
						histoUser.getPhysician().addAssociateRole(ContactRole.OTHER_PHYSICIAN);
					}
				}

				if (histoUser.getPhysician() == null) {
					logger.info("No Physsican found");
					histoUser.setPhysician(new Physician(new Person()));
					histoUser.getPhysician().setUid(userName);
					histoUser.getPhysician().setClinicEmployee(true);
					// Default role for that physician
					histoUser.getPhysician().addAssociateRole(ContactRole.OTHER_PHYSICIAN);
				}

				histoUser.getPhysician().copyIntoObject(physician);

				histoUser.setLastLogin(System.currentTimeMillis());

				// saving new organizations
				for (Organization organization : histoUser.getPhysician().getPerson().getOrganizsations()) {
					if (organization.getId() == 0)
						transientDAO.save(organization, "log.organization.created",
								new Object[] { organization.toString() });
				}

				transientDAO.save(histoUser, "log.userSettings.update", new Object[] { histoUser.toString() });

				Collection<? extends GrantedAuthority> authorities = histoUser.getAuthorities();

				return new UsernamePasswordAuthenticationToken(histoUser, password, authorities);
			} else
				throw new BadCredentialsException("Username not found.");
		} catch (NamingException | IOException | AuthenticationException e) {
			throw new BadCredentialsException("Username not found.");
		}

	}

	@Override
	public boolean supports(Class<? extends Object> authentication) {
		return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
	}
}