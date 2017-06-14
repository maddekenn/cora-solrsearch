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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.data.DataPart;
import se.uu.ub.cora.bookkeeper.data.converter.JsonToDataConverter;
import se.uu.ub.cora.bookkeeper.data.converter.JsonToDataConverterFactory;
import se.uu.ub.cora.bookkeeper.data.converter.JsonToDataConverterFactoryImp;
import se.uu.ub.cora.json.parser.JsonParser;
import se.uu.ub.cora.json.parser.JsonValue;
import se.uu.ub.cora.json.parser.org.OrgJsonParser;
import se.uu.ub.cora.solr.SolrClientProvider;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataElement;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderSearchResult;
import se.uu.ub.cora.spider.record.RecordSearch;

public final class SolrRecordSearch implements RecordSearch {

	private SolrClientProvider solrClientProvider;

	private SolrRecordSearch(SolrClientProvider solrClientProvider) {
		this.solrClientProvider = solrClientProvider;
	}

	public static SolrRecordSearch createSolrRecordSearchUsingSolrClientProvider(
			SolrClientProvider solrClientProvider) {
		return new SolrRecordSearch(solrClientProvider);
	}

	@Override
	public SpiderSearchResult searchUsingListOfRecordTypesToSearchInAndSearchData(List<String> list,
			SpiderDataGroup searchData) {
		SolrClient solrClient = solrClientProvider.getSolrClient();

		SolrQuery solrQuery = new SolrQuery();
		SpiderDataGroup include = searchData.extractGroup("include");
		SpiderDataGroup includePart = include.extractGroup("includePart");
		List<SpiderDataElement> searchTerms = includePart.getChildren();
		for (SpiderDataElement searchTerm : searchTerms) {
			SpiderDataAtomic searchTermAtomic = (SpiderDataAtomic) searchTerm;
			solrQuery.set(searchTermAtomic.getNameInData(), searchTermAtomic.getValue());
		}
		// solrQuery.setQuery("trams*");
		// solrQuery.setFilterQueries("kalle*");
		SpiderSearchResult spiderSearchResult = new SpiderSearchResult();
		spiderSearchResult.listOfDataGroups = new ArrayList<>();
		try {
			QueryResponse response = solrClient.query(solrQuery);
			SolrDocumentList results = response.getResults();
			String recordAsJson = (String) results.get(0).getFieldValue("recordAsJson");
			DataGroup dataGroup = convertJsonStringToDataGroup(recordAsJson);
			spiderSearchResult.listOfDataGroups.add(dataGroup);
			// System.out.println(response);
			//
			// System.out.println(response.getResults().get(0).getFieldValue("name"));
		} catch (SolrServerException | IOException e) {
			e.printStackTrace();
		}

		return spiderSearchResult;
	}

	// TODO: move this to bookkeeper
	private DataGroup convertJsonStringToDataGroup(String jsonRecord) {
		JsonParser jsonParser = new OrgJsonParser();
		JsonValue jsonValue = jsonParser.parseString(jsonRecord);
		JsonToDataConverterFactory jsonToDataConverterFactory = new JsonToDataConverterFactoryImp();
		JsonToDataConverter jsonToDataConverter = jsonToDataConverterFactory
				.createForJsonObject(jsonValue);
		DataPart dataPart = jsonToDataConverter.toInstance();
		return (DataGroup) dataPart;
	}

}
