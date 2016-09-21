package org.histo.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

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
import org.histo.config.HistoSettings;
import org.histo.model.Physician;
import org.histo.model.UserAcc;
import org.histo.model.UserRole;
import org.histo.ui.UserAccRoleHolder;

public class UserUtil {

	public static final ArrayList<UserAccRoleHolder> getUserAndRoles(List<UserAcc> userAccs) {
		ArrayList<UserAccRoleHolder> userAccRoleHolders = new ArrayList<UserAccRoleHolder>();
		for (UserAcc userAcc : userAccs) {
			userAccRoleHolders.add(new UserAccRoleHolder(userAcc));
		}
		return userAccRoleHolders;
	}

	public static final UserRole createRole(int roleLevel) {
		switch (roleLevel) {
		case UserRole.ROLE_LEVEL_GUEST:
			return new UserRole(UserRole.ROLE_GUEST_NAME, UserRole.ROLE_LEVEL_GUEST);
		case UserRole.ROLE_LEVEL_USER:
			return new UserRole(UserRole.ROLE_USER_NAME, UserRole.ROLE_LEVEL_USER);
		case UserRole.ROLE_LEVEL_MTA:
			return new UserRole(UserRole.ROLE_MTA_NAME, UserRole.ROLE_LEVEL_MTA);
		case UserRole.ROLE_LEVEL_HISTO:
			return new UserRole(UserRole.ROLE_HISTO_NAME, UserRole.ROLE_LEVEL_HISTO);
		case UserRole.ROLE_LEVEL_MODERATOR:
			return new UserRole(UserRole.ROLE_MODERATOR_NAME, UserRole.ROLE_LEVEL_MODERATOR);
		default:
			return new UserRole(UserRole.ROLE_GUEST_NAME, UserRole.ROLE_LEVEL_GUEST);
		}
	}

	public static final UserRole createRole(String roleLevel) {
		switch (roleLevel) {
		case UserRole.ROLE_GUEST_NAME:
			return new UserRole(UserRole.ROLE_GUEST_NAME, UserRole.ROLE_LEVEL_GUEST);
		case UserRole.ROLE_USER_NAME:
			return new UserRole(UserRole.ROLE_USER_NAME, UserRole.ROLE_LEVEL_USER);
		case UserRole.ROLE_MTA_NAME:
			return new UserRole(UserRole.ROLE_MTA_NAME, UserRole.ROLE_LEVEL_MTA);
		case UserRole.ROLE_HISTO_NAME:
			return new UserRole(UserRole.ROLE_HISTO_NAME, UserRole.ROLE_LEVEL_HISTO);
		case UserRole.ROLE_MODERATOR_NAME:
			return new UserRole(UserRole.ROLE_MODERATOR_NAME, UserRole.ROLE_LEVEL_MODERATOR);
		default:
			return new UserRole(UserRole.ROLE_GUEST_NAME, UserRole.ROLE_LEVEL_GUEST);
		}
	}

	public static final boolean accessByUserLevel(Collection<UserRole> roles, int level) {
		for (UserRole authority : roles) {
			if (authority.getLevel() >= level)
				return true;
		}
		return false;
	}

	public static final UserAcc createNewUser(String name) {
		UserRole guestRole = UserUtil.createRole(UserRole.ROLE_LEVEL_GUEST);
		UserAcc newUser = new UserAcc();
		newUser.setUsername(name);
		newUser.setRole(guestRole);
		newUser.setPhysician(new Physician());
		// set role clinicalDoctor or clical personnel
		newUser.getPhysician().setRoleClinicDoctor(true);
		return newUser;
	}

	/**
	 * Copies the new ldap data form update Physician to the original Physician
	 * 
	 * @param original
	 * @param update
	 * @return
	 */
	public static final Physician updatePhysicianData(Physician original, Physician update) {
		original.setFullName(update.getFullName());
		original.setUid(update.getUid());
		original.setName(update.getName());
		original.setEmployeeNumber(update.getEmployeeNumber());
		original.setSurname(update.getSurname());
		original.setEmail(update.getEmail());
		original.setPhoneNumber(update.getPhoneNumber());
		original.setPager(update.getPager());
		original.setDepartment(update.getDepartment());
		original.setClinicRole(update.getTitle());
		return original;
	}

	/**
	 * cn: Dr. Michael Reich ou: Klinik für Augenheilkunde givenName: Andreas
	 * mail: andreas.glatz@uniklinik-freiburg.de sn: Glatz title: Arzt
	 * telephonenumber: +49 761 270 40010 pager: 12-4027
	 * 
	 * @param acc
	 * @param attrs
	 * @return
	 * @throws NamingException
	 */
	public static final Physician updatePhysicianData(Physician physician, Attributes attrs) throws NamingException {

		// name surname title
		Attribute attr = attrs.get("cn");

		if (attr != null && attr.size() == 1) {
			physician.setFullName(attr.get().toString());
		}

		// uid
		attr = attrs.get("uid");
		if (attr != null && attr.size() == 1) {
			physician.setUid(attr.get().toString());
		}

		// dr titel
		attr = attrs.get("personalTitle");
		if (attr != null && attr.size() == 1) {
			physician.setTitle(attr.get().toString());
		}

		// name
		attr = attrs.get("sn");
		if (attr != null && attr.size() == 1) {
			physician.setName(attr.get().toString());
		}

		attr = attrs.get("employeeNumber");
		if (attr != null && attr.size() == 1) {
			System.out.println(attr.get().toString());
			physician.setEmployeeNumber(attr.get().toString());
		}
		attr = attrs.get("givenName");
		if (attr != null && attr.size() == 1) {
			physician.setSurname(attr.get().toString());
		}

		attr = attrs.get("mail");
		if (attr != null && attr.size() == 1) {
			physician.setEmail(attr.get().toString());
		}

		attr = attrs.get("telephonenumber");
		if (attr != null && attr.size() == 1) {
			physician.setPhoneNumber(attr.get().toString());
		}

		attr = attrs.get("pager");
		if (attr != null && attr.size() == 1) {
			physician.setPager(attr.get().toString());
		}

		// role in clinic
		attr = attrs.get("title");
		if (attr != null && attr.size() == 1) {
			physician.setClinicRole(attr.get().toString());
		}

		// department
		attr = attrs.get("ou");
		if (attr != null && attr.size() == 1) {
			physician.setDepartment(attr.get().toString());
		}
//		for (NamingEnumeration ae = attrs.getAll(); ae.hasMoreElements();) {
//			attr = (Attribute) ae.next();
//			String attrId = attr.getID();
//			for (Enumeration vals = attr.getAll(); vals.hasMoreElements(); System.out
//					.println(attrId + ": " + vals.nextElement()))
//				;
//		}

		return physician;
	}

	public static final List<Physician> getPhysiciansFromLDAP(String filter) {
		ArrayList<Physician> physicians = new ArrayList<>();

		try {
			Hashtable env = new Hashtable(5, 0.75f);
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, "ldap://" + HistoSettings.LDAP_HOST + ":" + HistoSettings.LDAP_PORT + "/"
					+ HistoSettings.LDAP_SUFFIX);

			SearchControls constraints = new SearchControls();
			constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);

			DirContext ctx = new InitialDirContext(env);

			NamingEnumeration results = ctx.search(HistoSettings.LDAP_BASE, filter, constraints);

			// temp id
			int i = 0;
			while (results != null && results.hasMore()) {
				SearchResult result = (SearchResult) results.next();
				Attributes attrs = result.getAttributes();
				if (attrs != null) {
					// check if uid is not a number, only people with a name as
					// uid are active
					Attribute attr = attrs.get("uid");
					if (attr != null && attr.size() == 1 && !StringUtils.isNumeric(attr.get().toString())) {
						Physician newPhysician = new Physician(i++);
						newPhysician.setRoleClinicDoctor(true);
						physicians.add(newPhysician);
						updatePhysicianData(newPhysician, attrs);
					}
				}
			}
		} catch (NamingException e) {
			System.err.println("NamingException: " + e.getMessage());
			e.printStackTrace();
		}

		return physicians;
	}

}
