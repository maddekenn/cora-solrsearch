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

import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.solr.SolrClientProvider;
import se.uu.ub.cora.spider.search.RecordIndexer;

public class SolrRecordIndexer implements RecordIndexer {
	private SolrClientProvider solrClientProvider;
	private String id;
	private String type;
	private DataGroup recordIndexData;
	private SolrInputDocument document;

	private SolrRecordIndexer(SolrClientProvider solrClientProvider) {
		this.solrClientProvider = solrClientProvider;
	}

	public static SolrRecordIndexer createSolrRecordIndexerUsingSolrClientProvider(
			SolrClientProvider solrClientProvider) {
		return new SolrRecordIndexer(solrClientProvider);
	}

	@Override
	public void indexData(DataGroup recordIndexData) {
		if (dataGroupHasSearchTerms(recordIndexData)) {
			this.recordIndexData = recordIndexData;
			document = new SolrInputDocument();
			extractRecordIdentification();
			addIdToDocument();
			addTypeToDocument();
			addSearchTerms();
			sendDocumentToSolr();
		}
	}

	private boolean dataGroupHasSearchTerms(DataGroup recordIndexData) {
		return recordIndexData.containsChildWithNameInData("searchTerm");
	}

	private void extractRecordIdentification() {
		id = recordIndexData.getFirstAtomicValueWithNameInData("id");
		type = recordIndexData.getFirstAtomicValueWithNameInData("type");
	}

	private void addIdToDocument() {
		document.addField("id", id);
	}

	private void addTypeToDocument() {
		document.addField("type", type);
	}

	private void addSearchTerms() {
		List<DataGroup> allSearchTermGroups = recordIndexData
				.getAllGroupsWithNameInData("searchTerm");
		for (DataGroup searchTerm : allSearchTermGroups) {
			document.addField(searchTerm.getFirstAtomicValueWithNameInData("searchTermName"),
					searchTerm.getFirstAtomicValueWithNameInData("searchTermValue"));
		}
	}

	private void sendDocumentToSolr() {
		try {
			SolrClient solrClient = solrClientProvider.getSolrClient();
			solrClient.add(document);
			solrClient.commit();
		} catch (Exception e) {
			throw SolrIndexException.withMessage("Error while indexing record with type: " + type
					+ " and id: " + id + " " + e.getMessage());
		}
	}

}
