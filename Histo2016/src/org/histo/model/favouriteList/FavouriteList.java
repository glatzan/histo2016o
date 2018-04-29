package org.histo.model.favouriteList;

import java.beans.Transient;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.histo.model.interfaces.HasID;
import org.histo.model.patient.Task;
import org.histo.model.user.HistoUser;

import lombok.Getter;
import lombok.Setter;

@Entity
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "favouritelist_sequencegenerator", sequenceName = "favouritelist_sequence")
@Getter
@Setter
public class FavouriteList implements HasID {

	@Id
	@GeneratedValue(generator = "favouritelist_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@OneToOne(fetch = FetchType.LAZY)
	private HistoUser owner;

	@Column(columnDefinition = "VARCHAR")
	private String name;

	@Column(columnDefinition = "VARCHAR")
	private String commentary;
	
	/**
	 * System list
	 */
	@Column
	private boolean defaultList;

	@Column
	private boolean globalView;
	
	@Column
	private boolean useIcon;
	
	@Column
	private String command; 
	
	@Column
	private String icon; 
	
	@Column
	private String iconColor; 
	
	@Column
	private String infoText; 
	
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "favouriteList")
	@Fetch(value = FetchMode.SUBSELECT)
	private List<FavouriteListItem> items;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "favouriteList", targetEntity = FavouritePermissionsGroup.class)
	@OrderBy("id ASC")
	private Set<FavouritePermissionsGroup> groups;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "favouriteList", targetEntity = FavouritePermissionsUser.class)
	@OrderBy("id ASC")
	private Set<FavouritePermissionsUser> users;

	@OneToMany(fetch = FetchType.LAZY)
	private Set<HistoUser> hideListForUser;
	 
	@Column
	private boolean useDumplist;
	
	@OneToOne(fetch = FetchType.LAZY)
	private FavouriteList dumpList;
	
	@Column
	private String dumpCommentary; 
	
	@Override
	public String toString() {
		return "ID: " + getId() + ", Name: " + getName();
	}
	
	@Transient
	public boolean containsTask(Task task) {
		return items.stream().anyMatch(p -> p.getTask().equals(task));
	}
}
