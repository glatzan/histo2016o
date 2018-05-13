package org.histo.service.dao.impl;

import org.histo.model.PDFContainer;
import org.histo.service.dao.PDFDao;
import org.springframework.stereotype.Repository;

@Repository("pdfDao")
public class PDFDaoImpl extends HibernateDao<PDFContainer, Long> implements PDFDao {

}
