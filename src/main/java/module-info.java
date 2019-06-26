module se.uu.ub.cora.solrsearch {
	requires transitive se.uu.ub.cora.searchstorage;
	requires transitive se.uu.ub.cora.spider;
	requires transitive solr.solrj;
	requires transitive se.uu.ub.cora.logger;
	requires se.uu.ub.cora.storage;

	exports se.uu.ub.cora.solr;
	exports se.uu.ub.cora.solrindex;
	exports se.uu.ub.cora.solrsearch;
}