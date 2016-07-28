package org.histo.action;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.dao.UserDAO;
import org.histo.model.UserAcc;
import org.histo.model.UserRole;
import org.histo.util.UserAccRoleHolder;
import org.histo.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class UserHandlerAction implements Serializable {

    private static Logger log = Logger.getLogger(UserHandlerAction.class.getName());
    
    @Autowired
    HelperHandlerAction helperHandlerAction;

    @Autowired
    UserDAO userDAO;

    @Autowired
    GenericDAO genericDAO;

    @Autowired
    private HelperDAO helperDAO;
    
    public boolean isUserAvailable(){
	if(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserAcc)
	    return true;
	return false;
    }
    
    /**
     * Returns the current user.
     * @return
     */
    public UserAcc getCurrentUser() {
	UserAcc user = (UserAcc) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	return user;
    }

    public boolean hasRole(String role) {
	// sonderfall wenn nicht eingeloggt
	if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof String)
	    return false;
	else {
	    UserAcc user = (UserAcc) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	    return hasRole(user, role);
	}
    }

    public boolean hasRole(UserAcc userAcc, String role) {
	if (userAcc.getRole().equals(role))
	    return true;
	return false;
    }

    public void setRoleForUser(String role) {
	setRoleForUser(getCurrentUser(), UserUtil.createRole(role));
    }
    
    // TODO delete
    public void setRoleForUser(UserAcc user, String role) {
	setRoleForUser(user, UserUtil.createRole(role));
    }

    // TODO delete
    public void setRoleForUser(UserAcc user, UserRole role) {
	UserRole oldRole = user.getRole();
	user.setRole(role);
	user.getAuthorities().add(role);
	genericDAO.save(role);
	genericDAO.save(user);
	genericDAO.delete(oldRole);
	log.info("Setting new role to " + role.getName());
    }


    public void prepareManageUser() {
    }

    public void onCloseHandler() {
    }

    public HelperHandlerAction getHelperHandlerAction() {
	return helperHandlerAction;
    }

    public void setHelperHandlerAction(HelperHandlerAction helperHandlerAction) {
	this.helperHandlerAction = helperHandlerAction;
    }



}
