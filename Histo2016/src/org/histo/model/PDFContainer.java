package org.histo.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "pdfs_sequencegenerator", sequenceName = "pdfs_sequence")
public class PDFContainer {

	private long id;

	private byte data[];

	private String type;

	private String name;

	private long creationDate;

	private boolean finalDocument;

	public PDFContainer() {
		this.creationDate = System.currentTimeMillis();
	}

	public PDFContainer(String pdfTemplate) {
		this(pdfTemplate, null);
	}

	public PDFContainer(String pdfTemplate, byte[] data) {
		this(pdfTemplate, null, data);
	}

	public PDFContainer(String pdfTemplate, String name, byte[] data) {
		this.type = pdfTemplate;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
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
}
