package org.histo.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.histo.model.interfaces.HasID;

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
	@Column
	private boolean defaultList;
	@Column
	private boolean editAble;
	@Column
	private boolean global;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "favouriteList")
	@Fetch(value = FetchMode.SUBSELECT)
	private List<FavouriteListItem> items;

	@Override
	public String toString() {
		return "ID: " + getId() + ", Name: " +getName(); 
	}
}
