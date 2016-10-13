package org.histo.config;

import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang3.StringUtils;
import org.histo.dao.UserDAO;
import org.histo.model.Physician;
import org.histo.model.HistoUser;
import org.histo.util.UserUtil;
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

	public static String host = "ldap.ukl.uni-freiburg.de";
	public static String port = "389";
	public static String suffix = "dc=ukl,dc=uni-freiburg,dc=de";
	public static String base = "ou=people";

	@Autowired
	private UserDAO userDAO;

	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String userName = authentication.getName().trim();
		String password = authentication.getCredentials().toString().trim();

//		try {
//			Hashtable<String, String> env = new Hashtable<String, String>();
//			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
//			env.put(Context.PROVIDER_URL, "ldap://" + host + ":" + port + "/" + suffix);
//
//			SearchControls constraints = new SearchControls();
//			constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
//
//			// search for user Data
//			DirContext ctx = new InitialDirContext(env);
//
//			NamingEnumeration<?> results = ctx.search(base, "(uid=" + userName + ")", constraints);
//
//			int count = 0;
//			String dn = null;
//			Attributes attrs = null;
//			while (results != null && results.hasMore()) {
//				SearchResult result = (SearchResult) results.next();
//
//				Attributes attrsTmp = result.getAttributes();
//
//				if (attrsTmp != null) {
//					Attribute attr = attrsTmp.get("uid");
//					if (attr != null && attr.size() == 1 && !StringUtils.isNumeric(attr.get().toString())) {
//						count++;
//						dn = result.getName() + "," + base + "," + suffix;
//						attrs = attrsTmp;
//					} else {
//						System.out.println("Not activ account: " + attr.get().toString());
//					}
//
//				}
//			}
//
//			if ((dn == null) || (count != 1)) {
//				throw new NamingException("Fehler bei der Authentisierung");
//			}
//
//			ctx.close();
//
//			env.put(Context.SECURITY_PRINCIPAL, dn);
//			env.put(Context.SECURITY_CREDENTIALS, password);
//
//			// if now error is thrown the auth attend was successful
//			ctx = new InitialDirContext(env);

			System.out.println("*** Bind erfolgreich ***");
			HistoUser histoUser = userDAO.loadUserByName(userName);

			if (histoUser == null) {
				histoUser = UserUtil.createNewUser(userName);

				// if the physician was added as surgeon the useracc an the
				// physician will be merged
				Physician tmp = userDAO.loadPhysicianByUID(userName);
				if (tmp != null)
					histoUser.setPhysician(tmp);
			}
			
			// updating the physician attributes 
//			UserUtil.updatePhysicianData(histoUser.getPhysician(), attrs);
//
//			ctx.close();

			histoUser.setLastLogin(System.currentTimeMillis());

			userDAO.saveUser(histoUser);


			Collection<? extends GrantedAuthority> authorities = histoUser.getAuthorities();

			return new UsernamePasswordAuthenticationToken(histoUser, password, authorities);
//
//		} catch (NamingException e) {
//			System.err.println("NamingException: " + e.getMessage());
//			throw new BadCredentialsException("Username not found.");
//		}

	}


	@Override
	public boolean supports(Class<? extends Object> authentication) {
		return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
	}
}