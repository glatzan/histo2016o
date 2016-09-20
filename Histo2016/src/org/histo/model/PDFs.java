package org.histo.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.histo.config.enums.PDFTemplate;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "pdfs_sequencegenerator", sequenceName = "pdfs_sequence")
public class PDFs {

	private long id;

	private byte data[];
	
	private boolean finalDocument;
	
	private PDFTemplate template;

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
	public PDFTemplate getTemplate() {
		return template;
	}

	public void setTemplate(PDFTemplate template) {
		this.template = template;
	}
}
