module se.uu.ub.cora.solrsearch {
	requires transitive se.uu.ub.cora.storage;
	requires transitive se.uu.ub.cora.spider;
	requires transitive solr.solrj;

	exports se.uu.ub.cora.solr;
	exports se.uu.ub.cora.solrindex;
	exports se.uu.ub.cora.solrsearch;
}