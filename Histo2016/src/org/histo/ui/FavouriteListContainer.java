package org.histo.ui;

import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.favouriteList.FavouritePermissionsGroup;
import org.histo.model.favouriteList.FavouritePermissionsUser;
import org.histo.model.user.HistoUser;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavouriteListContainer {

	public static final int PERMISSION_GLOBAL = 0;
	public static final int PERMISSION_OWNER = 1;
	public static final int PERMISSION_GROUP = 2;

	private FavouriteList favouriteList;

	private boolean owner;
	private boolean global;

	private boolean userPermission;
	private boolean groupPermission;

	private boolean editable;
	private boolean readable;
	private boolean admin;

	/**
	 * 0 = global 1 = owner 2 = GROUP
	 */
	private int type = PERMISSION_GLOBAL;

	public FavouriteListContainer(FavouriteList favouriteList, HistoUser currentUser) {
		this.favouriteList = favouriteList;

		if (favouriteList.getOwner().equals(currentUser)) {
			this.editable = true;
			this.readable = true;
			this.admin = true;

			type = PERMISSION_OWNER;
		}

		// only updating if not already true
		for (FavouritePermissionsUser user : favouriteList.getUsers()) {
			if (user.getUser().equals(currentUser)) {
				this.admin = this.admin ? true : user.isAdmin();
				this.editable = this.editable ? true : user.isEditable();
				this.readable = this.readable ? true : user.isReadable();
				this.userPermission = true;

				type = type > 0 ? type : PERMISSION_GROUP;
				break;
			}
		}

		// only updating if not already true
		for (FavouritePermissionsGroup group : favouriteList.getGroups()) {
			if (group.getGroup().equals(currentUser)) {
				this.admin = this.admin ? true : group.isAdmin();
				this.editable = this.editable ? true : group.isEditable();
				this.readable = this.readable ? true : group.isReadable();
				this.groupPermission = true;

				type = type > 0 ? type : PERMISSION_GROUP;
				break;
			}
		}
	}
	
	public boolean isOwnerOrAdmin() {
		return admin || owner;
	}
}
