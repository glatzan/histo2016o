package org.histo.config;

import java.io.IOException;
import java.util.Collection;
import javax.naming.NamingException;
import org.apache.log4j.Logger;
import org.histo.dao.UserDAO;
import org.histo.model.HistoUser;
import org.histo.model.Physician;
import org.histo.model.transitory.json.LdapConnection;
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

	private static Logger logger = Logger.getLogger("histo");

	public static String host = "ldap.ukl.uni-freiburg.de";
	public static String port = "389";
	public static String suffix = "dc=ukl,dc=uni-freiburg,dc=de";
	public static String base = "ou=people";

	@Autowired
	private UserDAO userDAO;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String userName = authentication.getName().trim();
		String password = authentication.getCredentials().toString().trim();

		try {
			LdapConnection connection = LdapConnection.factroy(HistoSettings.LDAP_JSON);

			connection.openConnection();

			Physician physician = connection.getPhyscican(userName);

			if (physician != null) {
				String dn = physician.getDnObjectName() + "," + base + "," + suffix;

				connection.closeConnection();

				logger.info("Physician found " + physician.getPerson().getFullName());

				// if now error was thrown auth was successful
				connection.checkPassword(dn, password);

				logger.info("Login successful " + physician.getPerson().getFullName());

				HistoUser histoUser = userDAO.loadUserByName(userName);

				if (histoUser == null) {
					logger.info("Creating new HistoUser " + physician.getPerson().getFullName());
					histoUser = new HistoUser(userName);

					// if the physician was added as surgeon the useracc an the
					// physician will be merged
					Physician physicianFromDatabase = userDAO.loadPhysicianByUID(userName);
					if (physicianFromDatabase != null) {
						histoUser.setPhysician(physicianFromDatabase);
						logger.info("Physician already in datanse " + physician.getPerson().getFullName());
					}
				}

				histoUser.getPhysician().copyIntoObject(physician);

				connection.closeConnection();

				histoUser.setLastLogin(System.currentTimeMillis());

				userDAO.saveUser(histoUser, "Benutzerdaten geupdated");

				Collection<? extends GrantedAuthority> authorities = histoUser.getAuthorities();

				return new UsernamePasswordAuthenticationToken(histoUser, password, authorities);
			} else
				throw new BadCredentialsException("Username not found.");
		} catch (NamingException | IOException e) {
			System.err.println("NamingException: " + e.getMessage());
			throw new BadCredentialsException("Username not found.");
		}

	}

	@Override
	public boolean supports(Class<? extends Object> authentication) {
		return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
	}
}