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

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.search.RecordIndexer;

public class SolrRecordIndexer implements RecordIndexer {
	private SolrClient solrClient;

	private SolrRecordIndexer(SolrClient solrClient) {
		this.solrClient = solrClient;
		// TODO Auto-generated constructor stub
	}

	public static SolrRecordIndexer createSolrRecordIndexerUsingSolrClient(SolrClient solrClient) {
		return new SolrRecordIndexer(solrClient);
	}

	@Override
	public void indexData(DataGroup recordIndexData) {
		// TODO Auto-generated method stub
		SolrInputDocument document = new SolrInputDocument();
		document.addField("id", recordIndexData.getFirstAtomicValueWithNameInData("id"));
		document.addField("type", recordIndexData.getFirstAtomicValueWithNameInData("type"));

		DataGroup searchTerm = recordIndexData.getFirstGroupWithNameInData("searchTerm");
		document.addField(searchTerm.getFirstAtomicValueWithNameInData("searchTermName"),
				searchTerm.getFirstAtomicValueWithNameInData("searchTermValue"));

		try {
			solrClient.add(document);
		} catch (SolrServerException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
