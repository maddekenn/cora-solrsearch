/*
 * Copyright 2017, 2019 Uppsala University Library
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

import se.uu.ub.cora.bookkeeper.data.converter.JsonToDataConverter;
import se.uu.ub.cora.bookkeeper.data.converter.JsonToDataConverterFactory;
import se.uu.ub.cora.bookkeeper.data.converter.JsonToDataConverterFactoryImp;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataElement;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataPart;
import se.uu.ub.cora.json.parser.JsonParser;
import se.uu.ub.cora.json.parser.JsonValue;
import se.uu.ub.cora.json.parser.org.OrgJsonParser;
import se.uu.ub.cora.searchstorage.SearchStorage;
import se.uu.ub.cora.solr.SolrClientProvider;
import se.uu.ub.cora.spider.record.RecordSearch;
import se.uu.ub.cora.storage.SpiderReadResult;

public final class SolrRecordSearch implements RecordSearch {

	private static final int DEFAULT_START = 1;
	private static final String START_STRING = "start";
	private static final int DEFAULT_NUMBER_OF_ROWS_TO_RETURN = 100;
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private SolrClientProvider solrClientProvider;
	private SearchStorage searchStorage;
	private SolrQuery solrQuery;
	private SolrClient solrClient;
	private int start;

	private SolrRecordSearch(SolrClientProvider solrClientProvider, SearchStorage searchStorage) {
		this.solrClientProvider = solrClientProvider;
		this.searchStorage = searchStorage;
	}

	public static SolrRecordSearch createSolrRecordSearchUsingSolrClientProviderAndSearchStorage(
			SolrClientProvider solrClientProvider, SearchStorage searchStorage) {
		return new SolrRecordSearch(solrClientProvider, searchStorage);
	}

	@Override
	public SpiderReadResult searchUsingListOfRecordTypesToSearchInAndSearchData(
			List<String> recordTypes, DataGroup searchData) {
		try {
			return tryToSearchUsingListOfRecordTypesToSearchInAndSearchData(recordTypes,
					searchData);
		} catch (Exception e) {
			return handleErrors(e);
		}
	}

	private SpiderReadResult handleErrors(Exception e) {
		if (isUndefinedFieldError(e)) {
			return createEmptySearchResult();
		}
		throw SolrSearchException.withMessage("Error searching for records: " + e.getMessage());
	}

	private SpiderReadResult tryToSearchUsingListOfRecordTypesToSearchInAndSearchData(
			List<String> recordTypes, DataGroup searchData)
			throws SolrServerException, IOException {
		solrClient = solrClientProvider.getSolrClient();

		solrQuery = new SolrQuery();
		int rows = getNumberOfRowsToRequest(searchData);
		solrQuery.setRows(rows);
		start = getStartRowToRequest(searchData);
		solrQuery.setStart(start - 1);
		addRecordTypesToFilterQuery(recordTypes);
		addSearchTermsToQuery(searchData);
		return searchInSolr();
	}

	private int getNumberOfRowsToRequest(DataGroup searchData) {
		if (searchData.containsChildWithNameInData("rows")) {
			return getRowsAsIntOrDefault(searchData);
		}
		return DEFAULT_NUMBER_OF_ROWS_TO_RETURN;
	}

	private int getRowsAsIntOrDefault(DataGroup searchData) {
		try {
			return Integer.parseInt(searchData.getFirstAtomicValueWithNameInData("rows"));
		} catch (NumberFormatException e) {
			return DEFAULT_NUMBER_OF_ROWS_TO_RETURN;
		}
	}

	private int getStartRowToRequest(DataGroup searchData) {
		if (searchData.containsChildWithNameInData(START_STRING)) {
			return getStartValueAsIntOrDefault(searchData);
		}
		return DEFAULT_START;
	}

	private int getStartValueAsIntOrDefault(DataGroup searchData) {
		try {
			return Integer.parseInt(searchData.getFirstAtomicValueWithNameInData(START_STRING));
		} catch (NumberFormatException e) {
			return DEFAULT_START;
		}
	}

	private void addRecordTypesToFilterQuery(List<String> recordTypes) {
		List<String> recordTypesWithType = addTypeToRecordTypes(recordTypes);
		String filterQuery = String.join(" OR ", recordTypesWithType);
		solrQuery.addFilterQuery(filterQuery);
	}

	private List<String> addTypeToRecordTypes(List<String> recordTypes) {
		List<String> recordTypesWithType = new ArrayList<>();
		for (String recordType : recordTypes) {
			recordTypesWithType.add("type:" + recordType);
		}
		return recordTypesWithType;
	}

	private void addSearchTermsToQuery(DataGroup searchData) {
		List<DataElement> childElementsFromSearchData = getChildElementsFromIncludePartOfSearch(
				searchData);
		for (DataElement childElementFromSearch : childElementsFromSearchData) {
			addSearchDataToQuery(solrQuery, childElementFromSearch);
		}
	}

	private void addSearchDataToQuery(SolrQuery solrQuery, DataElement childElementFromSearch) {
		DataAtomic childElementFromSearchAsAtomic = (DataAtomic) childElementFromSearch;
		DataGroup searchTerm = searchStorage
				.getSearchTerm(childElementFromSearchAsAtomic.getNameInData());
		String indexFieldName = extractIndexFieldName(searchTerm);

		if (searchTypeIsLinkedData(searchTerm)) {
			createQueryForLinkedData(solrQuery, childElementFromSearchAsAtomic, searchTerm,
					indexFieldName);
		} else {
			createQueryForFinal(solrQuery, childElementFromSearchAsAtomic, indexFieldName);
		}
	}

	private void createQueryForLinkedData(SolrQuery solrQuery,
			DataAtomic childElementFromSearchAsAtomic, DataGroup searchTerm,
			String indexFieldName) {
		String linkedOnIndexFieldName = getLinkedOnIndexFieldNameFromStorageUsingSearchTerm(
				searchTerm);
		String query = "{!join from=ids to=" + linkedOnIndexFieldName + "}" + indexFieldName + ":"
				+ childElementFromSearchAsAtomic.getValue();
		query += " AND type:" + searchTerm.getFirstGroupWithNameInData("searchInRecordType")
				.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		solrQuery.set("q", query);
	}

	private void createQueryForFinal(SolrQuery solrQuery, DataAtomic childElementFromSearchAsAtomic,
			String indexFieldName) {
		String searchStringWithParenthesis = getSearchStringFromChildAndSurroundWithParenthesis(
				childElementFromSearchAsAtomic);
		String queryString = indexFieldName + ":" + searchStringWithParenthesis;
		solrQuery.set("q", queryString);
	}

	private String getSearchStringFromChildAndSurroundWithParenthesis(
			DataAtomic childElementFromSearchAsAtomic) {
		return "(" + childElementFromSearchAsAtomic.getValue() + ")";
	}

	private String getLinkedOnIndexFieldNameFromStorageUsingSearchTerm(DataGroup searchTerm) {
		String linkedOn = getLinkedOnFromSearchTermDataGroup(searchTerm);
		DataGroup collectIndexTerm = searchStorage.getCollectIndexTerm(linkedOn);
		return extractFieldName(collectIndexTerm);
	}

	private boolean searchTypeIsLinkedData(DataGroup searchTerm) {
		return searchTerm.getFirstAtomicValueWithNameInData("searchTermType").equals("linkedData");
	}

	private String extractIndexFieldName(DataGroup searchTerm) {
		String id = getIndexTermIdFromSearchTermDataGroup(searchTerm);
		DataGroup collectIndexTerm = searchStorage.getCollectIndexTerm(id);
		return extractFieldName(collectIndexTerm);
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

	private List<DataElement> getChildElementsFromIncludePartOfSearch(DataGroup searchData) {
		DataGroup include = searchData.getFirstGroupWithNameInData("include");
		DataGroup includePart = include.getFirstGroupWithNameInData("includePart");
		return includePart.getChildren();
	}

	private String getIndexTermIdFromSearchTermDataGroup(DataGroup searchTerm) {
		DataGroup indexTerm = searchTerm.getFirstGroupWithNameInData("indexTerm");
		return indexTerm.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	private String getLinkedOnFromSearchTermDataGroup(DataGroup searchTerm) {
		DataGroup indexTerm = searchTerm.getFirstGroupWithNameInData("linkedOn");
		return indexTerm.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	private SpiderReadResult searchInSolr() throws SolrServerException, IOException {
		SolrDocumentList results = getSolrDocumentsFromSolr();
		return createSpiderSearchResultFromSolrResults(results);
	}

	private SolrDocumentList getSolrDocumentsFromSolr() throws SolrServerException, IOException {
		QueryResponse response = solrClient.query(solrQuery);

		return response.getResults();
	}

	private SpiderReadResult createSpiderSearchResultFromSolrResults(SolrDocumentList results) {
		SpiderReadResult spiderReadResult = createEmptySearchResult();
		spiderReadResult.start = start;
		spiderReadResult.totalNumberOfMatches = results.getNumFound();
		convertAndAddJsonResultsToSearchResult(spiderReadResult, results);
		return spiderReadResult;
	}

	private void convertAndAddJsonResultsToSearchResult(SpiderReadResult spiderReadResult,
			SolrDocumentList results) {
		for (SolrDocument solrDocument : results) {
			convertAndAddJsonResultToSearchResult(spiderReadResult, solrDocument);
		}
	}

	private void convertAndAddJsonResultToSearchResult(SpiderReadResult spiderReadResult,
			SolrDocument solrDocument) {
		String recordAsJson = (String) solrDocument.getFirstValue("recordAsJson");
		DataGroup dataGroup = convertJsonStringToDataGroup(recordAsJson);
		spiderReadResult.listOfDataGroups.add(dataGroup);
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

	private SpiderReadResult createEmptySearchResult() {
		SpiderReadResult spiderReadResult = new SpiderReadResult();
		spiderReadResult.listOfDataGroups = new ArrayList<>();
		return spiderReadResult;
	}

	public SearchStorage getSearchStorage() {
		return searchStorage;
	}

	public SolrClientProvider getSolrClientProvider() {
		// Needed for test
		return solrClientProvider;
	}

}
