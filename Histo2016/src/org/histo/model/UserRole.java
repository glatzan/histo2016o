package org.histo.model;

import java.beans.Transient;
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.springframework.security.core.GrantedAuthority;

@Entity
@SequenceGenerator(name = "role_sequencegenerator", sequenceName = "role_sequence")
public class UserRole implements Serializable,Cloneable,GrantedAuthority {
    /**
     * gast
     * user
     * mta
     * befunder
     * admin
     * god
     */
    
    
    public static final String ROLE_GUEST_NAME = "guest";
    public static final int ROLE_LEVEL_GUEST = 1;
    
    public static final String ROLE_USER_NAME = "user";
    public static final int ROLE_LEVEL_USER = 2;
    
    public static final String ROLE_MTA_NAME = "mta";
    public static final int ROLE_LEVEL_MTA = 3;
    
    public static final String ROLE_HISTO_NAME = "researcher";
    public static final int ROLE_LEVEL_HISTO = 4;
    
    public static final String ROLE_MODERATOR_NAME = "moderator";
    public static final int ROLE_LEVEL_MODERATOR = 5;
    
    public static final String ROLE_SUPERADMIN_NAME = "admin";
    public static final int ROLE_SUPERADMIN_GOD = 6;
    
    private long id;
    
    private String name;
    
    private int level;
    
    public UserRole() {
    }
    
    public UserRole(String roleName, int level){
	this.name = roleName;
	this.level = level;
    }
    
    @Id
    @GeneratedValue(generator = "role_sequencegenerator")
    @Column(unique = true, nullable = false)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(unique=true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column
    public int getLevel() {
	return level;
    }

    public void setLevel(int level) {
	this.level = level;
    }

    @Override
    public UserRole clone() {
	return new UserRole(getName(), getLevel());
    }

    @Override
    public String getAuthority() {
	if(getLevel() < ROLE_LEVEL_USER){
	    return "ROLE_GUEST";
	}
	return "ROLE_USER";
    }

    public void setAuthority(String authoritys) {
    }

    @Override
    public String toString() {
	return getAuthority();
    }

}
