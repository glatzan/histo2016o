package org.histo.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.UserAcc;
import org.histo.model.UserRole;

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
		newUser.getPhysician().setPerson(new Person());
		return newUser;
	}

	/**
	 * cn: Dr. Michael Reich
	 * ou: Klinik für Augenheilkunde
	 * givenName: Andreas
	 * mail: andreas.glatz@uniklinik-freiburg.de
	 * sn: Glatz
	 * title: Arzt
	 * telephonenumber: +49 761 270 40010
	 * pager: 12-4027
	 * 
	 * @param acc
	 * @param attrs
	 * @return
	 * @throws NamingException
	 */
	public static final UserAcc updateUserData(UserAcc acc, Attributes attrs) throws NamingException {
		
		// name
		Attribute attr = attrs.get("sn");

		System.out.println(attr.size());
		if (attr != null && attr.size() == 1) {
			acc.getPhysician().getPerson().setName(attr.toString());
			System.out.println(attr.toString());
		}
		
		attr = attrs.get("givenName");
		if (attr != null && attr.size() == 1) {
			acc.getPhysician().getPerson().setSurname(attr.toString());
		}
		
		attr = attrs.get("mail");
		if (attr != null && attr.size() == 1) {
			acc.getPhysician().getPerson().setEmail(attr.toString());
		}

		attr = attrs.get("telephonenumber");
		if (attr != null && attr.size() == 1) {
			acc.getPhysician().getPerson().setPhoneNumber(attr.toString());
		}
		
		attr = attrs.get("pager");
		if (attr != null && attr.size() == 1) {
			acc.getPhysician().setPager(attr.toString());
		}
		
		attr = attrs.get("title");
		if (attr != null && attr.size() == 1) {
			acc.getPhysician().setTitle(attr.toString());
		}
		
		
		attr = attrs.get("ou");
		if (attr != null && attr.size() == 1) {
			acc.getPhysician().setDepartment(attr.toString());
		}
		
		return acc;
	}

}
