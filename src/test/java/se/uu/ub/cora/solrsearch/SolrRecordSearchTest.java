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

package se.uu.ub.cora.solrsearch;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.solr.SolrClientProvider;
import se.uu.ub.cora.solrindex.SolrClientProviderSpy;
import se.uu.ub.cora.solrindex.SolrClientSpy;
import se.uu.ub.cora.spider.data.SpiderSearchResult;

public class SolrRecordSearchTest {
	@Test
	public void testInit() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		SolrRecordSearch solrSearch = SolrRecordSearch
				.createSolrRecordSearchUsingSolrClientProvider(solrClientProvider);
		assertNotNull(solrSearch);
		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;
		SolrQuery solrQueryCreated = (SolrQuery) solrClientSpy.params;
		assertNull(solrQueryCreated);

		// // String urlString =
		// // "http://130.238.171.39:8983/solr/gettingstarted";
		// String urlString = "http://localhost:8983/solr/coracore";
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
		// // solrQuery.setQuery("name:kalle");
		// solrQuery.add("q", "name:kalle");
		// solrQuery.add("q", "name:kula");
		// // solrQuery.set("name", "kalle");
		// // solrQuery.setQuery("trams*");
		// // solrQuery.setFilterQueries("kalle*");
		// // CommonParams.Q;
		// try {
		// QueryResponse response = solr.query(solrQuery);
		// System.out.println(response);
		// //
		// System.out.println(response.getResults().get(0).getFieldValue("name"));
		// System.out.println("QUERY: " + solrQuery.getQuery());
		// } catch (SolrServerException | IOException e) {
		// e.printStackTrace();
		// }
	}

	@Test
	public void testSearchOneParameterNoRecordType() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		QueryResponse queryResponse = new QueryResponseSpy();
		((SolrClientProviderSpy) solrClientProvider).solrClientSpy.queryResponse = queryResponse;

		SolrRecordSearch solrSearch = SolrRecordSearch
				.createSolrRecordSearchUsingSolrClientProvider(solrClientProvider);
		List<String> recordTypes = new ArrayList<>();
		DataGroup searchData = DataGroup.withNameInData("bookSearch");
		DataGroup include = DataGroup.withNameInData("include");
		searchData.addChild(include);
		DataGroup includePart = DataGroup.withNameInData("includePart");
		include.addChild(includePart);
		includePart.addChild(DataAtomic.withNameInDataAndValue("titleSearchTerm", "A title"));

		SpiderSearchResult searchResult = solrSearch
				.searchUsingListOfRecordTypesToSearchInAndSearchData(recordTypes, searchData);
		assertNotNull(searchResult.listOfDataGroups);
		DataGroup firstResult = searchResult.listOfDataGroups.get(0);
		assertEquals(firstResult.getNameInData(), "book");

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;
		SolrQuery solrQueryCreated = (SolrQuery) solrClientSpy.params;
		assertEquals(solrQueryCreated.getQuery(), "titleSearchTerm:A title");
	}

	@Test
	public void testReturnThreeRecords() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		QueryResponseSpy queryResponse = new QueryResponseSpy();
		queryResponse.noOfDocumentsToReturn = 3;
		((SolrClientProviderSpy) solrClientProvider).solrClientSpy.queryResponse = queryResponse;

		SolrRecordSearch solrSearch = SolrRecordSearch
				.createSolrRecordSearchUsingSolrClientProvider(solrClientProvider);
		List<String> recordTypes = new ArrayList<>();
		DataGroup searchData = DataGroup.withNameInData("bookSearch");
		DataGroup include = DataGroup.withNameInData("include");
		searchData.addChild(include);
		DataGroup includePart = DataGroup.withNameInData("includePart");
		include.addChild(includePart);
		includePart.addChild(DataAtomic.withNameInDataAndValue("titleSearchTerm", "A title"));

		SpiderSearchResult searchResult = solrSearch
				.searchUsingListOfRecordTypesToSearchInAndSearchData(recordTypes, searchData);
		assertEquals(searchResult.listOfDataGroups.size(), 3);
	}

	@Test(expectedExceptions = SolrSearchException.class)
	public void testSearchErrorException() {
		SolrClientProviderSpy solrClientProvider = new SolrClientProviderSpy();
		solrClientProvider.returnErrorThrowingClient = true;
		QueryResponseSpy queryResponse = new QueryResponseSpy();
		queryResponse.noOfDocumentsToReturn = 3;
		solrClientProvider.solrClientSpy.queryResponse = queryResponse;

		SolrRecordSearch solrSearch = SolrRecordSearch
				.createSolrRecordSearchUsingSolrClientProvider(solrClientProvider);
		List<String> recordTypes = new ArrayList<>();
		DataGroup searchData = DataGroup.withNameInData("bookSearch");
		DataGroup include = DataGroup.withNameInData("include");
		searchData.addChild(include);
		DataGroup includePart = DataGroup.withNameInData("includePart");
		include.addChild(includePart);
		includePart.addChild(DataAtomic.withNameInDataAndValue("titleSearchTerm", "A title"));

		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(recordTypes, searchData);
	}

}
