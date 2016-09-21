package org.histo.ui;

import org.histo.model.UserAcc;

public class UserAccRoleHolder {

    private UserAcc userAcc;
    
    private String roleName;

    public UserAccRoleHolder(UserAcc userAcc){
	setUserAcc(userAcc);
	setRoleName(userAcc.getRole().getName());
    }
    
    public UserAcc getUserAcc() {
        return userAcc;
    }

    public void setUserAcc(UserAcc userAcc) {
        this.userAcc = userAcc;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    
    
}
