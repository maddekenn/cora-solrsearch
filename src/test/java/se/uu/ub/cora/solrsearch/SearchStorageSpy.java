package se.uu.ub.cora.solrsearch;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataElement;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.searchstorage.SearchStorage;

public class SearchStorageSpy implements SearchStorage {

	public List<String> searchTermIds = new ArrayList<>();
	public String indexTypeToReturn = "indexTypeString";

	public List<String> collectIndexTermIds = new ArrayList<>();

	@Override
	public DataGroup getSearchTerm(String searchTermId) {
		searchTermIds.add(searchTermId);

		DataGroup searchTerm = createSearchTermWithId(searchTermId);
		if ("titleSearchTerm".equals(searchTermId)) {
			searchTerm.addChild(createIndexTermWithIndexTermId("titleIndexTerm"));
			searchTerm.addChild(createSearchTermType("final"));
		}
		if ("anUnindexedTerm".equals(searchTermId)) {
			searchTerm.addChild(createIndexTermWithIndexTermId("titleIndexTerm"));
			searchTerm.addChild(createSearchTermType("final"));
		}
		if ("linkedTextSearchTerm".equals(searchTermId)) {
			searchTerm.addChild(createSearchTermType("linkedData"));
			searchTerm.addChild(createLinkedOnWithIndexTermId("linkedTextIndexTerm"));
			searchTerm.addChild(createSearchInRecordTypeWithRecordTypeId("coraText"));
			searchTerm.addChild(createIndexTermWithIndexTermId("swedishIndexTerm"));

		}
		return searchTerm;
	}

	private DataElement createLinkedOnWithIndexTermId(String indexTermId) {
		DataGroup indexTerm = DataGroup.withNameInData("linkedOn");
		indexTerm.addChild(
				DataAtomic.withNameInDataAndValue("linkedRecordType", "collectIndexTerm"));
		indexTerm.addChild(DataAtomic.withNameInDataAndValue("linkedRecordId", indexTermId));
		return indexTerm;
	}

	private DataElement createSearchInRecordTypeWithRecordTypeId(String recordTypeId) {
		DataGroup searchInRecordType = DataGroup.withNameInData("searchInRecordType");
		searchInRecordType
				.addChild(DataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
		searchInRecordType
				.addChild(DataAtomic.withNameInDataAndValue("linkedRecordId", recordTypeId));
		return searchInRecordType;
	}

	private DataAtomic createSearchTermType(String searchTermType) {
		return DataAtomic.withNameInDataAndValue("searchTermType", searchTermType);
	}

	private DataGroup createIndexTermWithIndexTermId(String indexTermId) {
		DataGroup indexTerm = DataGroup.withNameInData("indexTerm");
		indexTerm.addChild(
				DataAtomic.withNameInDataAndValue("linkedRecordType", "collectIndexTerm"));
		indexTerm.addChild(DataAtomic.withNameInDataAndValue("linkedRecordId", indexTermId));
		return indexTerm;
	}

	private DataGroup createSearchTermWithId(String searchTermId) {
		DataGroup searchTerm = DataGroup.withNameInData("searchTerm");
		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", searchTermId));
		searchTerm.addChild(recordInfo);
		return searchTerm;
	}

	@Override
	public DataGroup getCollectIndexTerm(String collectIndexTermId) {
		collectIndexTermIds.add(collectIndexTermId);

		DataGroup titleIndexTerm = DataGroup.withNameInData("collectTerm");
		DataGroup extraData = DataGroup.withNameInData("extraData");
		titleIndexTerm.addChild(extraData);
		if ("titleIndexTerm".equals(collectIndexTermId)) {
			extraData.addChild(DataAtomic.withNameInDataAndValue("indexFieldName", "title"));
			extraData.addChild(DataAtomic.withNameInDataAndValue("indexType", indexTypeToReturn));
		}
		if ("linkedTextIndexTerm".equals(collectIndexTermId)) {
			extraData.addChild(DataAtomic.withNameInDataAndValue("indexFieldName", "textId"));
			extraData.addChild(DataAtomic.withNameInDataAndValue("indexType", "indexTypeString"));
		}
		if ("swedishIndexTerm".equals(collectIndexTermId)) {
			extraData.addChild(DataAtomic.withNameInDataAndValue("indexFieldName", "swedish"));
			extraData.addChild(DataAtomic.withNameInDataAndValue("indexType", "indexTypeText"));
		}

		return titleIndexTerm;
	}

}
