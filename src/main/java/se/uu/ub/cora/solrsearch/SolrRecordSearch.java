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
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataElement;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.data.DataPart;
import se.uu.ub.cora.bookkeeper.data.converter.JsonToDataConverter;
import se.uu.ub.cora.bookkeeper.data.converter.JsonToDataConverterFactory;
import se.uu.ub.cora.bookkeeper.data.converter.JsonToDataConverterFactoryImp;
import se.uu.ub.cora.json.parser.JsonParser;
import se.uu.ub.cora.json.parser.JsonValue;
import se.uu.ub.cora.json.parser.org.OrgJsonParser;
import se.uu.ub.cora.solr.SolrClientProvider;
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
			DataGroup searchData) {

		try {
			return tryToSearchUsingListOfRecordTypesToSearchInAndSearchData(searchData);
		} catch (Exception e) {
			// Error from server at http://localhost:8983/solr/coracore: undefined field
			// testNewsTitleSearchTerm
			// org.apache.solr.client.solrj.impl.HttpSolrClient$RemoteSolrException: Error
			// from server at http://localhost:8983/solr/coracore: undefined field
			// testNewsTitleSearchTerm
			if (e.getMessage() != null && e.getMessage().contains("undefined field")) {
				SpiderSearchResult spiderSearchResult = new SpiderSearchResult();
				spiderSearchResult.listOfDataGroups = new ArrayList<>();
				return spiderSearchResult;
			}
			throw SolrSearchException.withMessage("Error searching for records: " + e.getMessage());
		}
	}

	private SpiderSearchResult tryToSearchUsingListOfRecordTypesToSearchInAndSearchData(
			DataGroup searchData) throws SolrServerException, IOException {
		SolrClient solrClient = solrClientProvider.getSolrClient();

		SolrQuery solrQuery = new SolrQuery();
		DataGroup include = searchData.getFirstGroupWithNameInData("include");
		DataGroup includePart = include.getFirstGroupWithNameInData("includePart");
		List<DataElement> searchTerms = includePart.getChildren();
		for (DataElement searchTerm : searchTerms) {
			DataAtomic searchTermAtomic = (DataAtomic) searchTerm;
			solrQuery.set("q",
					searchTermAtomic.getNameInData() + ":" + searchTermAtomic.getValue());
		}
		SpiderSearchResult spiderSearchResult = new SpiderSearchResult();
		spiderSearchResult.listOfDataGroups = new ArrayList<>();
		QueryResponse response = solrClient.query(solrQuery);
		SolrDocumentList results = response.getResults();
		for (SolrDocument solrDocument : results) {
			String recordAsJson = (String) solrDocument.getFirstValue("recordAsJson");
			DataGroup dataGroup = convertJsonStringToDataGroup(recordAsJson);
			spiderSearchResult.listOfDataGroups.add(dataGroup);
		}

		return spiderSearchResult;
	}

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
