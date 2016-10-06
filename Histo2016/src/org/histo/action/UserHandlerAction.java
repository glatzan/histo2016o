package org.histo.action;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.histo.config.enums.Role;
import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.dao.UserDAO;
import org.histo.model.HistoUser;
import org.histo.model.UserRole;
import org.histo.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class UserHandlerAction implements Serializable {

    private static final long serialVersionUID = -8314968695816748306L;

    @Autowired
    private HelperHandlerAction helperHandlerAction;

    @Autowired
    private GenericDAO genericDAO;

    public boolean isUserAvailable() {
	if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof HistoUser)
	    return true;
	return false;
    }

    /**
     * Returns the current user.
     * 
     * @return
     */
    public HistoUser getCurrentUser() {
	HistoUser user = (HistoUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	return user;
    }

    public boolean currentUserHasRole(Role role) {
	return getCurrentUser().getRole().equals(role);
    }

    public boolean currentUserHasRoleOrHigher(Role role) {
	return getCurrentUser().getRole().getRole().getRoleValue() >= role.getRoleValue();
    }

    
    
    
    
    
    
    
    
    
    public boolean hasRole(String role) {
	// sonderfall wenn nicht eingeloggt
	if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof String)
	    return false;
	else {
	    HistoUser user = (HistoUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	    return hasRole(user, role);
	}
    }

    public boolean hasRole(HistoUser histoUser, String role) {
	if (histoUser.getRole().equals(role))
	    return true;
	return false;
    }

    public void setRoleForUser(String role) {
	setRoleForUser(getCurrentUser(), UserUtil.createRole(role));
    }

    // TODO delete
    public void setRoleForUser(HistoUser user, String role) {
	setRoleForUser(user, UserUtil.createRole(role));
    }

    // TODO delete
    public void setRoleForUser(HistoUser user, UserRole role) {
	UserRole oldRole = user.getRole();
	user.setRole(role);
	user.getAuthorities().add(role);
	genericDAO.save(role);
	genericDAO.save(user);
	genericDAO.delete(oldRole);
	// log.info("Setting new role to " + role.getName());
    }
}
