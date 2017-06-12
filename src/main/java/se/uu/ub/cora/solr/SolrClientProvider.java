package se.uu.ub.cora.solr;

import org.apache.solr.client.solrj.SolrClient;

public interface SolrClientProvider {

	SolrClient getSolrClient();

}
