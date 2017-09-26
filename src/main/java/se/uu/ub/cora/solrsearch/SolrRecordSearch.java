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
import se.uu.ub.cora.searchstorage.SearchStorage;
import se.uu.ub.cora.solr.SolrClientProvider;
import se.uu.ub.cora.spider.data.SpiderSearchResult;
import se.uu.ub.cora.spider.record.RecordSearch;

public final class SolrRecordSearch implements RecordSearch {

	private SolrClientProvider solrClientProvider;
	private SearchStorage searchStorage;

	private SolrRecordSearch(SolrClientProvider solrClientProvider, SearchStorage searchStorage) {
		this.solrClientProvider = solrClientProvider;
		this.searchStorage = searchStorage;
	}

	public static SolrRecordSearch createSolrRecordSearchUsingSolrClientProviderAndSearchStorage(
			SolrClientProvider solrClientProvider, SearchStorage searchStorage) {
		return new SolrRecordSearch(solrClientProvider, searchStorage);
	}

	@Override
	public SpiderSearchResult searchUsingListOfRecordTypesToSearchInAndSearchData(List<String> list,
			DataGroup searchData) {

		try {
			return tryToSearchUsingListOfRecordTypesToSearchInAndSearchData(searchData);
		} catch (Exception e) {
			if (isUndefinedFieldError(e)) {
				return createEmptySearchResult();
			}
			throw SolrSearchException.withMessage("Error searching for records: " + e.getMessage());
		}
	}

	private SpiderSearchResult tryToSearchUsingListOfRecordTypesToSearchInAndSearchData(
			DataGroup searchData) throws SolrServerException, IOException {
		SolrClient solrClient = solrClientProvider.getSolrClient();

		SolrQuery solrQuery = new SolrQuery();
		List<DataElement> searchTerms = getSearchTerms(searchData);
		for (DataElement searchTerm : searchTerms) {
			DataAtomic searchTermAtomic = (DataAtomic) searchTerm;
			String id = getIndexTermIdFromSearchTerm(searchTermAtomic);
			DataGroup collectIndexTerm = searchStorage.getCollectIndexTerm(id);
			String extractedFieldName = extractFieldName(collectIndexTerm);
			// solrQuery.set("q", id + ":" + searchTermAtomic.getValue());
			solrQuery.set("q", extractedFieldName + ":" + searchTermAtomic.getValue());
		}
		return getSpiderSearchResult(solrClient, solrQuery);
	}

	private String extractFieldName(DataGroup collectIndexTerm) {
		DataGroup extraData = collectIndexTerm.getFirstGroupWithNameInData("extraData");
		String indexType = extraData.getFirstAtomicValueWithNameInData("indexType");

		String fieldName = extraData.getFirstAtomicValueWithNameInData("indexFieldName");
		String suffix = chooseSuffixFromIndexType(indexType);
		return fieldName + suffix;
	}

	private String chooseSuffixFromIndexType(String indexType) {
		if ("indexTypeString".equals(indexType)) {
			return "_s";
		} else if ("indexTypeBoolean".equals(indexType)) {
			return "_b";
		} else if ("indexTypeDate".equals(indexType)) {
			return "_dt";
		} else if ("indexTypeNumber".equals(indexType)) {
			return "_l";
		} else {
			return "_t";
		}
	}

	private List<DataElement> getSearchTerms(DataGroup searchData) {
		DataGroup include = searchData.getFirstGroupWithNameInData("include");
		DataGroup includePart = include.getFirstGroupWithNameInData("includePart");
		return includePart.getChildren();
	}

	private String getIndexTermIdFromSearchTerm(DataAtomic searchTermAtomic) {
		DataGroup readSearchTerm = searchStorage.getSearchTerm(searchTermAtomic.getNameInData());
		DataGroup indexTerm = readSearchTerm.getFirstGroupWithNameInData("indexTerm");
		return indexTerm.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	private SpiderSearchResult getSpiderSearchResult(SolrClient solrClient, SolrQuery solrQuery)
			throws SolrServerException, IOException {
		SpiderSearchResult spiderSearchResult = createEmptySearchResult();
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

	private boolean isUndefinedFieldError(Exception e) {
		return e.getMessage().contains("undefined field");
	}

	private SpiderSearchResult createEmptySearchResult() {
		SpiderSearchResult spiderSearchResult = new SpiderSearchResult();
		spiderSearchResult.listOfDataGroups = new ArrayList<>();
		return spiderSearchResult;
	}

	public SearchStorage getSearchStorage() {
		return searchStorage;
	}

}
