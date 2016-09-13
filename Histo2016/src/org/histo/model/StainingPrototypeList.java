package org.histo.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.histo.model.util.EditAbleEntity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

@Entity
@SequenceGenerator(name = "stainingPrototypeList_sequencegenerator", sequenceName = "stainingPrototypeList_sequence")
public class StainingPrototypeList implements EditAbleEntity<StainingPrototypeList> {

	@Expose
	private long id;

	@Expose
	private String name;

	@Expose
	private String commentary;

	@Expose
	private List<StainingPrototype> stainingPrototypes;

	public StainingPrototypeList() {
	}

	public StainingPrototypeList(StainingPrototypeList stainingPrototypeList) {
		this.id = stainingPrototypeList.getId();
		update(stainingPrototypeList);
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	@Id
	@GeneratedValue(generator = "stainingPrototypeList_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Column
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(columnDefinition = "text")
	public String getCommentary() {
		return commentary;
	}

	public void setCommentary(String commentary) {
		this.commentary = commentary;
	}

	@ManyToMany(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	public List<StainingPrototype> getStainingPrototypes() {
		if (stainingPrototypes == null)
			stainingPrototypes = new ArrayList<StainingPrototype>();

		return stainingPrototypes;
	}

	public void setStainingPrototypes(List<StainingPrototype> stainingPrototypes) {
		this.stainingPrototypes = stainingPrototypes;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	@Transient
	public void update(StainingPrototypeList stainingPrototypeList) {
		this.name = stainingPrototypeList.getName();
		this.commentary = stainingPrototypeList.getCommentary();
		this.stainingPrototypes = new ArrayList<StainingPrototype>(stainingPrototypeList.getStainingPrototypes());
	}

	@Transient
	public String asGson() {
		final GsonBuilder builder = new GsonBuilder();
		builder.excludeFieldsWithoutExposeAnnotation();
		final Gson gson = builder.create();
		return gson.toJson(this);
	}

}
