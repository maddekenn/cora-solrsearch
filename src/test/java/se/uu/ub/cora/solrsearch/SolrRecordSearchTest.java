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

import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.testng.annotations.Test;

import se.uu.ub.cora.solr.SolrClientProvider;
import se.uu.ub.cora.solrindex.SolrClientProviderSpy;
import se.uu.ub.cora.solrindex.SolrClientSpy;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderSearchResult;

public class SolrRecordSearchTest {
	@Test
	public void testInit() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		SolrRecordSearch solrSearch = SolrRecordSearch
				.createSolrRecordSearchUsingSolrClientProvider(solrClientProvider);
		assertNotNull(solrSearch);
		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;
		SolrQuery solrQuery2 = solrClientSpy.getQuery();

		SolrQuery solrQuery = new SolrQuery();
		// solrQuery.setFields("id");
		solrQuery.setQuery("name:kalle");
		// solrQuery.setQuery("trams*");
		// solrQuery.setFilterQueries("kalle*");
		try {
			QueryResponse response = solr.query(solrQuery);
			System.out.println(response);
			//
			System.out.println(response.getResults().get(0).getFieldValue("name"));
		} catch (SolrServerException | IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testSearch() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		SolrRecordSearch solrSearch = SolrRecordSearch
				.createSolrRecordSearchUsingSolrClientProvider(solrClientProvider);
		List<String> recordTypes = new ArrayList<>();
		SpiderDataGroup searchData = SpiderDataGroup.withNameInData("searchData");
		SpiderSearchResult searchResult = solrSearch
				.searchUsingListOfRecordTypesToSearchInAndSearchData(recordTypes, searchData);
		assertNotNull(searchResult.listOfDataGroups);
	}

}
