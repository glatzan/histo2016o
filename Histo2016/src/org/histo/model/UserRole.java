package org.histo.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.histo.config.enums.Role;
import org.histo.model.util.LogAble;
import org.springframework.security.core.GrantedAuthority;

@Entity
@SequenceGenerator(name = "role_sequencegenerator", sequenceName = "role_sequence")
public class UserRole implements Serializable, Cloneable, GrantedAuthority, LogAble {

    
	private long id;

	private Role role;

	public UserRole() {
	}

	public UserRole(Role role) {
		this.role = role;
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

	public Role getRole() {
	    return role;
	}

	public void setRole(Role role) {
	    this.role = role;
	}

	@Override
	public UserRole clone() {
		return new UserRole(getRole());
	}

	@Override
	public String getAuthority() {
		if (getRole().getRoleValue() < Role.GUEST.getRoleValue()) {
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
