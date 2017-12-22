package org.histo.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;

import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.ListOrder;
import org.histo.model.interfaces.LogAble;

import lombok.Getter;
import lombok.Setter;

@Entity
@SequenceGenerator(name = "stainingPrototype_sequencegenerator", sequenceName = "stainingPrototype_sequence")
@Getter
@Setter
public class StainingPrototype implements LogAble, ListOrder<StainingPrototype>, HasID {

	public static final int TYPE_NORMAL = 0;
	public static final int TYPE_IMMUN = 1;

	public enum StainingType {
		NORMAL, IMMUN;
	}

	@Id
	@GeneratedValue(generator = "stainingPrototype_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@Column(columnDefinition = "VARCHAR")
	private String name;

	@Column(columnDefinition = "VARCHAR")
	private String commentary;

	@Enumerated(EnumType.STRING)
	private StainingType type;

	@Column
	private boolean archived;

	@Column
	private int indexInList;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "staining")
	@OrderColumn(name="INDEX")
	private List<StainingPrototypeDetails> batchDetails;

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof StainingPrototype && ((StainingPrototype) obj).getId() == getId())
			return true;

		return super.equals(obj);
	}

}
