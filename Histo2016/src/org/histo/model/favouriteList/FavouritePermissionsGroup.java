package org.histo.model.favouriteList;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.histo.model.user.HistoGroup;

import lombok.Getter;
import lombok.Setter;

@Entity
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@Getter
@Setter
public class FavouritePermissionsGroup extends FavouritePermissions {

	@OneToOne
	private HistoGroup group;

	public FavouritePermissionsGroup() {
	}
	
	public FavouritePermissionsGroup(HistoGroup group) {
		this.group = group;
	}
}
