package org.histo.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.Type;
import org.histo.model.StainingPrototype.StainingType;

import lombok.Getter;
import lombok.Setter;


@Entity
@SequenceGenerator(name = "stainingPrototypeDetails_sequencegenerator", sequenceName = "stainingPrototypeDetails_sequence")
@Getter
@Setter
public class StainingPrototypeDetails {

	@Id
	@GeneratedValue(generator = "stainingPrototypeDetails_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@Column(columnDefinition = "VARCHAR")
	private String name;
	
	/**
	 * Parent
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	private StainingPrototype staining;
	
	/**
	 * rabbit/ mouse
	 */
	@Column(columnDefinition = "VARCHAR")
	private String host;
	
	/**
	 * e.g. human
	 */
	@Column(columnDefinition = "VARCHAR")
	private String specifity;
	
	@Column(columnDefinition = "VARCHAR")
	private String type;
	
	/**
	 * e.g. 2ml
	 */
	@Column(columnDefinition = "VARCHAR")
	private String quantityDelivered;
	
	@Column(columnDefinition = "VARCHAR")
	private String positiveControl;
	
	/**
	 * temperature
	 */
	@Column(columnDefinition = "VARCHAR")
	private String storage;
	
	@Column(columnDefinition = "VARCHAR")
	@Type(type = "date")
	private Date bestBefore;
	
	@Column(columnDefinition = "VARCHAR")
	@Type(type = "date")
	private Date deliveryDate;
	
	@Column(columnDefinition = "VARCHAR")
	@Type(type = "date")
	private Date emptyDate;
	
	/**
	 * e.g firm
	 */
	@Column(columnDefinition = "VARCHAR")
	private String supplier;
	
	@Column(columnDefinition = "VARCHAR")
	private String treatment;
	
	@Column
	private int incubationTime;
	
	@Column(columnDefinition = "VARCHAR")
	private String dilution;
	
	@Column(columnDefinition = "VARCHAR")
	private String standardDilution;
	
	@Column(columnDefinition = "VARCHAR")
	private String process;
	
	@Column(columnDefinition = "VARCHAR")
	private String commentary;
}
