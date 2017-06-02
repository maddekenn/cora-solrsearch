package se.uu.ub.cora.solrsearch;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.search.RecordIndexer;

public class SolrRecordIndexer implements RecordIndexer {

	public DataGroup recordIndexData;

	@Override
	public void indexData(DataGroup recordIndexData) {
		this.recordIndexData = recordIndexData;

	}

}
