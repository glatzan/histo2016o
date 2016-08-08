package org.histo.config;

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.histo.dao.GenericDAO;
import org.histo.dao.UserDAO;
import org.histo.model.UserAcc;
import org.histo.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.naming.directory.*;

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

		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, "ldap://" + host + ":" + port + "/" + suffix);

			SearchControls constraints = new SearchControls();
			constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);

			// search for user Data
			DirContext ctx = new InitialDirContext(env);

			NamingEnumeration<?> results = ctx.search(base, "(uid=" + userName + ")", constraints);

			int count = 0;
			String dn = null;
			Attributes attrs = null;
			while (results != null && results.hasMore()) {
				SearchResult result = (SearchResult) results.next();
				dn = result.getName() + "," + base + "," + suffix;
				result.getAttributes();
				System.out.println("dn: " + dn);
				count++;
				attrs = result.getAttributes();
			}

			if ((dn == null) || (count != 1)) {
				throw new NamingException("Fehler bei der Authentisierung");
			}

			ctx.close();

			env.put(Context.SECURITY_PRINCIPAL, dn);
			env.put(Context.SECURITY_CREDENTIALS, password);

			// if now error is thrown the auth attend was successful
			ctx = new InitialDirContext(env);

			
			System.out.println("*** Bind erfolgreich ***");
			UserAcc userAcc = userDAO.loadUserByName(userName);
			
			if (userAcc == null) {
				userAcc = UserUtil.createNewUser(userName);
			}
			
			if (attrs != null) {
				UserUtil.updateUserData(userAcc, attrs);
			} 
			
			ctx.close();
			
			userDAO.saveUser(userAcc);
			
			System.out.println("User loaded " + userAcc);

			Collection<? extends GrantedAuthority> authorities = userAcc.getAuthorities();

			System.out.println(userAcc + " " + password + " " + authorities);
			
			return new UsernamePasswordAuthenticationToken(userAcc, password, authorities);

		} catch (NamingException e) {
			System.err.println("NamingException: " + e.getMessage());
			throw new BadCredentialsException("Username not found.");
		}

	}

	public void search(String uid) {
		try {
			System.out.println();
			System.out.println("*** Search ***");
			System.out.println();
			Hashtable env = new Hashtable(5, 0.75f);
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, "ldap://" + host + ":" + port + "/" + suffix);

			SearchControls constraints = new SearchControls();
			constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);

			DirContext ctx = new InitialDirContext(env);

			// String[] attrIDs = {"member"};
			// String[] attrIDs = {"cn"}; // Nur die Gruppen cn-Namne werden
			// zurueckgegeben!
			// constraints. setReturningAttributes(attrIDs);
			NamingEnumeration results = ctx.search(base, "(uid=" + uid + ")", constraints);

			while (results != null && results.hasMore()) {
				SearchResult result = (SearchResult) results.next();
				System.out.println("Name: " + result.getName());
				Attributes attrs = result.getAttributes();
				if (attrs == null) {
					System.out.println("Keine Attribute vorhanden!");
				} else {
					System.out.println("*** Ausgabe von ausgewaehlten Attributen ***");
					Attribute attr = attrs.get("cn");
					if (attr != null) {
						for (NamingEnumeration vals = attr.getAll(); vals.hasMoreElements(); System.out
								.println("cn: " + vals.nextElement()))
							;
					}
					attr = attrs.get("sn");
					if (attr != null) {
						for (NamingEnumeration vals = attr.getAll(); vals.hasMoreElements(); System.out
								.println("sn: " + vals.nextElement()))
							;
					}
					attr = attrs.get("mail");
					if (attr != null) {
						for (NamingEnumeration vals = attr.getAll(); vals.hasMoreElements(); System.out
								.println("mail: " + vals.nextElement()))
							;
					}
					attr = attrs.get("telephonenumber");
					if (attr != null) {
						for (NamingEnumeration vals = attr.getAll(); vals.hasMoreElements(); System.out
								.println("telephonenumber: " + vals.nextElement()))
							;
					}
					System.out.println("*** Ausgabe aller empfangenen Attribute ***");
					for (NamingEnumeration ae = attrs.getAll(); ae.hasMoreElements();) {
						attr = (Attribute) ae.next();
						String attrId = attr.getID();
						for (Enumeration vals = attr.getAll(); vals.hasMoreElements(); System.out
								.println(attrId + ": " + vals.nextElement()))
							;
					}
				}
				System.out.println();
			}
		} catch (NamingException e) {
			System.err.println("NamingException: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public boolean supports(Class<? extends Object> authentication) {
		return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
	}
}