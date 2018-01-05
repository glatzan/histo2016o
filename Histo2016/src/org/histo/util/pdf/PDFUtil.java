package org.histo.util.pdf;

import java.util.List;
import java.util.stream.Collectors;

import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.model.interfaces.HasDataList;

public class PDFUtil {

	public static PDFContainer getLastPDFofType(HasDataList listObj, DocumentType type) {
		List<PDFContainer> resultArr = listObj.getAttachedPdfs().stream().filter(p -> p.getType() == type).collect(Collectors.toList());
		
		if(resultArr.isEmpty())
			return null;
		
		PDFContainer newstContainer = resultArr.get(0);
		
		for(int i = 1; i < resultArr.size(); i++)
			if(resultArr.get(i).getCreationDate() > newstContainer.getCreationDate())
				newstContainer = resultArr.get(i);
				
		return newstContainer;
	}
}
