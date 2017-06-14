package se.uu.ub.cora.solrindex;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

public class SolrClientSpy extends SolrClient {

	public SolrInputDocument document;
	public boolean committed = false;

	@Override
	public UpdateResponse add(SolrInputDocument doc) throws SolrServerException, IOException {
		this.document = doc;
		// TODO Auto-generated method stub
		return super.add(doc);
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

	@Override
	public UpdateResponse commit() throws SolrServerException, IOException {
		committed = true;
		return super.commit();

	}

	@Override
	public QueryResponse query(SolrParams params) throws SolrServerException, IOException {
		// TODO Auto-generated method stub
		return super.query(params);
	}

	public SolrQuery getQuery() {
		// TODO Auto-generated method stub
		return null;
	}

}
