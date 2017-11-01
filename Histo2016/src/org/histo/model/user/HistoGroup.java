package org.histo.model.user;

import java.beans.Transient;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Version;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
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

	@Override
	@Transient
	public String getAuthority() {
		return name;
	}

	@Column(columnDefinition = "boolean default false")
	private boolean archived;

	public HistoGroup() {

	}

	public HistoGroup(HistoSettings settings) {
		this.settings = settings;
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
