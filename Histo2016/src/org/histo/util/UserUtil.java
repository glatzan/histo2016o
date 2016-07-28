package org.histo.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.histo.model.UserAcc;
import org.histo.model.UserRole;

public class UserUtil {

    public static final ArrayList<UserAccRoleHolder> getUserAndRoles(List<UserAcc> userAccs){
	ArrayList<UserAccRoleHolder> userAccRoleHolders = new ArrayList<UserAccRoleHolder>();
	for (UserAcc userAcc  : userAccs) {
	    userAccRoleHolders.add(new UserAccRoleHolder(userAcc));
	}
	return userAccRoleHolders;
    }

    public static final UserRole createRole(int roleLevel){
	switch (roleLevel) {
	case UserRole.ROLE_LEVEL_GUEST:
	    return new UserRole(UserRole.ROLE_GUEST_NAME,UserRole.ROLE_LEVEL_GUEST);
	case UserRole.ROLE_LEVEL_USER:
	    return new UserRole(UserRole.ROLE_USER_NAME,UserRole.ROLE_LEVEL_USER);	    
	case UserRole.ROLE_LEVEL_MTA:
	    return new UserRole(UserRole.ROLE_MTA_NAME,UserRole.ROLE_LEVEL_MTA);
	case UserRole.ROLE_LEVEL_HISTO:
	    return new UserRole(UserRole.ROLE_HISTO_NAME,UserRole.ROLE_LEVEL_HISTO);	
	case UserRole.ROLE_LEVEL_MODERATOR:
	    return new UserRole(UserRole.ROLE_MODERATOR_NAME,UserRole.ROLE_LEVEL_MODERATOR);		    
	default:
	    return new UserRole(UserRole.ROLE_GUEST_NAME,UserRole.ROLE_LEVEL_GUEST);
	}
    }

    public static final UserRole createRole(String roleLevel){
	switch (roleLevel) {
	case UserRole.ROLE_GUEST_NAME:
	    return new UserRole(UserRole.ROLE_GUEST_NAME,UserRole.ROLE_LEVEL_GUEST);
	case UserRole.ROLE_USER_NAME:
	    return new UserRole(UserRole.ROLE_USER_NAME,UserRole.ROLE_LEVEL_USER);	    
	case UserRole.ROLE_MTA_NAME:
	    return new UserRole(UserRole.ROLE_MTA_NAME,UserRole.ROLE_LEVEL_MTA);
	case UserRole.ROLE_HISTO_NAME:
	    return new UserRole(UserRole.ROLE_HISTO_NAME,UserRole.ROLE_LEVEL_HISTO);	
	case UserRole.ROLE_MODERATOR_NAME:
	    return new UserRole(UserRole.ROLE_MODERATOR_NAME,UserRole.ROLE_LEVEL_MODERATOR);		    
	default:
	    return new UserRole(UserRole.ROLE_GUEST_NAME,UserRole.ROLE_LEVEL_GUEST);
	}
    }
    
    public static final boolean accessByUserLevel(Collection<UserRole> roles, int level) {
	for (UserRole authority : roles) {
	    if (authority.getLevel() >= level)
		return true;
	}
	return false;
    }

}
