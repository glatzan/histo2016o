package org.histo.model.user;

import java.beans.Transient;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Version;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.histo.config.enums.View;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.HasID;
import org.springframework.security.core.GrantedAuthority;

import lombok.Getter;
import lombok.Setter;

@Entity
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "group_sequencegenerator", sequenceName = "group_sequence")
@Getter
@Setter
public class HistoGroup implements GrantedAuthority, HasID, ArchivAble {

	private static final long serialVersionUID = 5926752130546123895L;

	@Id
	@GeneratedValue(generator = "group_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@Version
	private long version;

	@Column(columnDefinition = "VARCHAR")
	private String name;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
	private HistoSettings settings;

	@Enumerated(EnumType.STRING)
	private AuthRole authRole;

	@Column(columnDefinition = "VARCHAR")
	private String commentary;

	@Override
	@Transient
	public String getAuthority() {
		return authRole.name();
	}

	@Column(columnDefinition = "boolean default false")
	private boolean archived;

	public HistoGroup() {
	}

	public HistoGroup(HistoSettings settings) {
		this(settings, null);
	}

	public HistoGroup(HistoSettings settings, AuthRole authRole) {
		this.settings = settings;
		this.authRole = authRole;
	}

	@ElementCollection(fetch = FetchType.EAGER)
	@Enumerated(EnumType.STRING)
	@Cascade(value = { org.hibernate.annotations.CascadeType.ALL })
	private Set<HistoPermissions> permissions;

	public enum AuthRole {
		ROLE_NONEAUTH, ROLE_GUEST, ROLE_USER;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof HistoGroup && ((HistoGroup) obj).getId() == getId())
			return true;
		return super.equals(obj);
	}
}

// public static final Role getRoleByToken(String token) {
// Role[] roles = Role.values();
//
// for (int i = 0; i < roles.length; i++) {
// if (roles[i].getToken().equals(token))
// return roles[i];
// }
//
// return NONE_AUTH;
// }

// NONE_AUTH(0, "ROLE_NONEAUTH"), GUEST(1, "ROLE_GUEST"), SCIENTIST(50,
// "ROLE_SCIENTIST"), USER(100, "ROLE_USER"), MTA(
// 200,
// "ROLE_MTA"), PHYSICIAN(300, "ROLE_PHYSICIAN"), MODERATOR(400,
// "ROLE_MODERATOR"), ADMIN(500, "ROLE_ADMIN");
