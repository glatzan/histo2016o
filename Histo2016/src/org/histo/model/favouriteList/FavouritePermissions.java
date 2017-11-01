package org.histo.model.favouriteList;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;

import lombok.Getter;
import lombok.Setter;

@Entity
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@Getter
@Setter
@SequenceGenerator(name = "favourite_permissions_generator", sequenceName = "favourite_permissions_seq", allocationSize = 50)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class FavouritePermissions {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "favourite_permissions_generator")
	@Column(unique = true, nullable = false)
	private long id;

	@Column
	private boolean admin;

	@Column
	private boolean readable;

	@Column
	private boolean editable;

}
