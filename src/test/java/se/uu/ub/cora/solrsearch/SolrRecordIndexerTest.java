package se.uu.ub.cora.solrsearch;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataGroup;

public class SolrRecordIndexerTest {
	@Test
	public void testInit() {
		SolrRecordIndexer solrIndexer = new SolrRecordIndexer();
		assertNotNull(solrIndexer);
	}

	@Test
	public void testIndexDataGroup() {
		SolrRecordIndexer solrIndexer = new SolrRecordIndexer();
		DataGroup recordIndexData = DataGroup.withNameInData("recordIndexData");
		solrIndexer.indexData(recordIndexData);
		assertEquals(solrIndexer.recordIndexData, recordIndexData);
	}
}
