package se.uu.ub.cora.solrsearch;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class QueryResponseSpy extends QueryResponse {
	private static final long serialVersionUID = -3679333870730130124L;

	public int noOfDocumentsToReturn = 1;

	@Override
	public SolrDocumentList getResults() {
		SolrDocumentList solrDocumentList = new SolrDocumentList();

		for (int no = 0; no < noOfDocumentsToReturn; no++) {
			String id = String.valueOf(552199 + no);
			solrDocumentList.add(createOneSolrDocumentWithId(id));
		}

		return solrDocumentList;
	}

	private SolrDocument createOneSolrDocumentWithId(String id) {
		SolrDocument document = new SolrDocument();
		document.addField("id", id);
		document.addField("name", "kalle");
		document.addField("name", "kula");
		document.addField("price", "49.99");
		String bookAsJson = "{\"name\":\"book\",\"children\":[{\"name\":\"recordInfo\",\"children\":[{\"name\":\"id\",\"value\":\"book:39921376484193\"},{\"name\":\"type\",\"children\":[{\"name\":\"linkedRecordType\",\"value\":\"recordType\"},{\"name\":\"linkedRecordId\",\"value\":\"book\"}]},{\"name\":\"createdBy\",\"value\":\"141414\"},{\"name\":\"dataDivider\",\"children\":[{\"name\":\"linkedRecordType\",\"value\":\"system\"},{\"name\":\"linkedRecordId\",\"value\":\"bibsys\"}]}]},{\"name\":\"bookTitle\",\"value\":\"Workshop 2\"}]}";

		document.addField("recordAsJson", bookAsJson);
		return document;
	}
}
