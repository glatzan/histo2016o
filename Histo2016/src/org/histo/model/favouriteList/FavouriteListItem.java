package org.histo.model.favouriteList;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.histo.model.interfaces.HasID;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;

@Entity
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "favouritelistitem_sequencegenerator", sequenceName = "favouritelistitem_sequence")
public class FavouriteListItem implements HasID {

	private long id;

	private FavouriteList favouriteList;

	private Task task;

	private String commentary;

	private List<Slide> slides;

	public FavouriteListItem() {
	}

	public FavouriteListItem(FavouriteList favouriteList, Task task) {
		this.task = task;
		this.favouriteList = favouriteList;
	}

	// ************************ Getter/Setter ************************
	@Id
	@GeneratedValue(generator = "favouritelistitem_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@OneToOne
	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public String getCommentary() {
		return commentary;
	}

	public void setCommentary(String commentary) {
		this.commentary = commentary;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	public List<Slide> getSlides() {
		return slides;
	}

	public void setSlides(List<Slide> slides) {
		this.slides = slides;
	}

	@ManyToOne(fetch=FetchType.LAZY)
	public FavouriteList getFavouriteList() {
		return favouriteList;
	}

	public void setFavouriteList(FavouriteList favouriteList) {
		this.favouriteList = favouriteList;
	}

}
