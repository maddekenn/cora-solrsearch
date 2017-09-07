package se.uu.ub.cora.solrindex;

import org.apache.solr.client.solrj.SolrClient;

import se.uu.ub.cora.solr.SolrClientProvider;

public class SolrClientProviderSpy implements SolrClientProvider {
	public SolrClientSpy solrClientSpy = new SolrClientSpy();
	public SolrClientThrowsExceptionSpy solrClientExceptionSpy = new SolrClientThrowsExceptionSpy();
	public boolean returnErrorThrowingClient = false;
	public String errorMessage = "something went wrong";

	@Override
	public SolrClient getSolrClient() {
		if (returnErrorThrowingClient) {
			solrClientExceptionSpy.errorMessage = errorMessage;
			return solrClientExceptionSpy;
		}
		return solrClientSpy;
	}

}
