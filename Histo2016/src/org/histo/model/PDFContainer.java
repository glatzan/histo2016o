package org.histo.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.histo.config.enums.DocumentType;
import org.histo.model.interfaces.HasID;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "pdfs_sequencegenerator", sequenceName = "pdfs_sequence")
public class PDFContainer implements HasID {

	private long id;

	private byte data[];

	private DocumentType type;

	private String name;

	private long creationDate;

	private boolean finalDocument;

	private String commentary;

	public PDFContainer() {
		this.creationDate = System.currentTimeMillis();
	}

	public PDFContainer(DocumentType type) {
		this(type, null);
	}

	public PDFContainer(DocumentType type, byte[] data) {
		this(type, null, data);
	}

	public PDFContainer(DocumentType type, String name, byte[] data) {
		this.type = type;
		this.data = data;
		this.name = name;
		this.creationDate = System.currentTimeMillis();
	}

	@Id
	@GeneratedValue(generator = "pdfs_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Type(type = "org.hibernate.type.BinaryType")
	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public boolean isFinalDocument() {
		return finalDocument;
	}

	public void setFinalDocument(boolean finalDocument) {
		this.finalDocument = finalDocument;
	}

	@Enumerated(EnumType.STRING)
	public DocumentType getType() {
		return type;
	}

	public void setType(DocumentType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	@Column(columnDefinition = "text")
	public String getCommentary() {
		return commentary;
	}

	public void setCommentary(String commentary) {
		this.commentary = commentary;
	}

	/********************************************************
	 * Transient
	 ********************************************************/
	@Transient
	public Date getCreationDateAsDate() {
		return new Date(creationDate);
	}

	public void setCreationDateAsDate(Date creationDate) {
		this.creationDate = creationDate.getTime();
	}

	/********************************************************
	 * Transient
	 ********************************************************/

}
