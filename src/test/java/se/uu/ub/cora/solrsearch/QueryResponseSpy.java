package se.uu.ub.cora.solrsearch;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class QueryResponseSpy extends QueryResponse {
	@Override
	public SolrDocumentList getResults() {
		// TODO Auto-generated method stub
		// return super.getResults();
		SolrDocumentList solrDocumentList = new SolrDocumentList();

		SolrDocument document = new SolrDocument();
		solrDocumentList.add(document);
		document.addField("id", "552199");
		document.addField("name", "kalle");
		document.addField("name", "kula");
		document.addField("price", "49.99");
		String bookAsJson = "{\"name\":\"book\",\"children\":[{\"name\":\"recordInfo\",\"children\":[{\"name\":\"id\",\"value\":\"book:39921376484193\"},{\"name\":\"type\",\"children\":[{\"name\":\"linkedRecordType\",\"value\":\"recordType\"},{\"name\":\"linkedRecordId\",\"value\":\"book\"}]},{\"name\":\"createdBy\",\"value\":\"141414\"},{\"name\":\"dataDivider\",\"children\":[{\"name\":\"linkedRecordType\",\"value\":\"system\"},{\"name\":\"linkedRecordId\",\"value\":\"bibsys\"}]}]},{\"name\":\"bookTitle\",\"value\":\"Workshop 2\"}]}";

		document.addField("recordAsJson", bookAsJson);
		return solrDocumentList;
	}
}
