package se.uu.ub.cora.solrindex;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;

public class SolrClientThrowsExceptionSpy extends SolrClient {
	public String errorMessage = "something went wrong";

	@Override
	public UpdateResponse add(SolrInputDocument doc) throws SolrServerException, IOException {
		throw new RuntimeException(errorMessage);
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public NamedList<Object> request(SolrRequest arg0, String arg1)
			throws SolrServerException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
