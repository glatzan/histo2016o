package org.histo.model.favouriteList;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.histo.model.interfaces.HasID;

import lombok.Getter;
import lombok.Setter;

//@Entity
@Getter
@Setter
@MappedSuperclass
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class FavouritePermissions implements HasID {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(unique = true, nullable = false)
	protected long id;

	@Column
	protected boolean admin;

	@Column
	protected boolean readable;

	@Column
	protected boolean editable;

	@ManyToOne(fetch = FetchType.LAZY)
	protected FavouriteList favouriteList;

}
