/*
 * Copyright 2017 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.uu.ub.cora.solrindex;

import static org.testng.Assert.assertEquals;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.search.RecordIndexer;

public class SolrRecordIndexerTest {
	@BeforeMethod
	public void setUp() {
	}

	@Test
	public void testCollectOneSearchTerm() {

		SolrClient solrClient = new SolrClientSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClient(solrClient);

		DataGroup recordIndexData = DataGroup.withNameInData("recordIndexData");
		recordIndexData.addChild(DataAtomic.withNameInDataAndValue("id", "someId"));
		recordIndexData.addChild(DataAtomic.withNameInDataAndValue("type", "someType"));

		DataGroup searchTerm = DataGroup.withNameInData("searchTerm");
		recordIndexData.addChild(searchTerm);
		searchTerm.addChild(DataAtomic.withNameInDataAndValue("searchTermName", "name"));
		searchTerm.addChild(DataAtomic.withNameInDataAndValue("searchTermValue", "value"));
		searchTerm.setRepeatId("0");

		recordIndexer.indexData(recordIndexData);

		SolrClientSpy solrClientSpy = (SolrClientSpy) solrClient;

		SolrInputDocument created = solrClientSpy.document;

		assertEquals(created.getField("id").getValue().toString(), "someId");
		assertEquals(created.getField("type").getValue().toString(), "someType");

		assertEquals(created.getField("name").getValue().toString(), "value");

		// solrClientSpy.document.

		// String urlString = "http://130.238.171.39:8983/solr/gettingstarted";
		// SolrClient solr = new HttpSolrClient.Builder(urlString).build();
		// SolrInputDocument document = new SolrInputDocument();
		// document.addField("id", "552199");
		// document.addField("name", "kalle");
		// document.addField("name", "kula");
		// document.addField("price", "49.99");
		// try {
		// UpdateResponse response = solr.add(document);
		// System.out.println(response);
		// solr.commit();
		// } catch (SolrServerException | IOException e) {
		// e.printStackTrace();
		// }
		// // Remember to commit your changes!
		//
		// SolrQuery solrQuery = new SolrQuery();
		// // solrQuery.setFields("id");
		// solrQuery.setQuery("name:kalle");
		// // solrQuery.setQuery("trams*");
		// // solrQuery.setFilterQueries("kalle*");
		// try {
		// QueryResponse response = solr.query(solrQuery);
		// System.out.println(response);
		// //
		// System.out.println(response.getResults().get(0).getFieldValue("name"));
		// } catch (SolrServerException | IOException e) {
		// e.printStackTrace();
		// }
	}
}
