package org.histo.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.histo.model.interfaces.HasID;

@Entity
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "favouritelist_sequencegenerator", sequenceName = "favouritelist_sequence")
public class FavouriteList implements HasID {

	private long id;
	private HistoUser owner;
	private String name;

	private boolean editAble;
	private boolean globale;

	// ************************ Getter/Setter ************************
	@Id
	@GeneratedValue(generator = "favouritelist_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@OneToOne
	public HistoUser getOwner() {
		return owner;
	}

	public void setOwner(HistoUser owner) {
		this.owner = owner;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isEditAble() {
		return editAble;
	}

	public void setEditAble(boolean editAble) {
		this.editAble = editAble;
	}

	public boolean isGlobale() {
		return globale;
	}

	public void setGlobale(boolean globale) {
		this.globale = globale;
	}

}
