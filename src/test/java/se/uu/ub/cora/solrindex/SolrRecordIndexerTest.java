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
import static org.testng.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.solr.SolrClientProvider;
import se.uu.ub.cora.spider.search.RecordIndexer;

public class SolrRecordIndexerTest {
	private List<String> ids = new ArrayList<>();

	@BeforeTest
	public void setUp() {
		ids.add("someType_someId");
	}

	@Test
	public void testCollectNoIndexDataGroupNoCollectedDataTerm() {

		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		DataGroup collectedData = DataGroup.withNameInData("collectedData");
		collectedData.addChild(DataAtomic.withNameInDataAndValue("type", "someType"));
		collectedData.addChild(DataAtomic.withNameInDataAndValue("id", "someId"));

		recordIndexer.indexData(Collections.emptyList(), collectedData,
				DataGroup.withNameInData("someDataGroup"));

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertNull(created);
		assertEquals(solrClientSpy.committed, false);
	}

	@Test
	public void testCollectIndexDataGroupNoCollectedDataTerm() {

		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		DataGroup collectedData = DataGroup.withNameInData("collectedData");
		collectedData.addChild(DataAtomic.withNameInDataAndValue("type", "someType"));
		collectedData.addChild(DataAtomic.withNameInDataAndValue("id", "someId"));

		DataGroup index = DataGroup.withNameInData("index");
		collectedData.addChild(index);
		recordIndexer.indexData(Collections.emptyList(), collectedData,
				DataGroup.withNameInData("someDataGroup"));

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertNull(created);
		assertEquals(solrClientSpy.committed, false);
	}

	@Test
	public void testCollectOneSearchTerm() {

		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		DataGroup recordIndexData = createCollectedDataWithOneCollectedIndexDataTerm();

		DataGroup dataGroup = DataGroup.withNameInData("someDataGroup");
		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		dataGroup.addChild(recordInfo);
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "someId"));
		recordIndexer.indexData(ids, recordIndexData, dataGroup);

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;
		String expectedJson = "{\n" + "    \"children\": [{\n" + "        \"children\": [{\n"
				+ "            \"name\": \"id\",\n" + "            \"value\": \"someId\"\n"
				+ "        }],\n" + "        \"name\": \"recordInfo\"\n" + "    }],\n"
				+ "    \"name\": \"someDataGroup\"\n" + "}" + "";

		assertEquals(created.getField("recordAsJson").getValue().toString(), expectedJson);

		assertEquals(created.getField("id").getValue().toString(), "someType_someId");
		assertEquals(created.getField("ids").getValue().toString(), "someType_someId");
		assertEquals(created.getField("type").getValue().toString(), "someType");

		assertEquals(created.getField("title_s").getValue().toString(), "someEnteredValue");
	}

	@Test
	public void testCollectOneSearchTermTwoIds() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		DataGroup recordIndexData = createCollectedDataWithOneCollectedIndexDataTerm();

		DataGroup dataGroup = DataGroup.withNameInData("someDataGroup");
		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		dataGroup.addChild(recordInfo);
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "someId"));
		List<String> ids2 = new ArrayList<>();
		ids2.add("someType_someId");
		ids2.add("someAbstractType_someId");
		recordIndexer.indexData(ids2, recordIndexData, dataGroup);

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertEquals(created.getField("id").getValue().toString(), "someType_someId");
		assertEquals(created.getField("ids").getValue().toString(),
				"[someType_someId, someAbstractType_someId]");
		assertEquals(created.getField("type").getValue().toString(), "someType");

		assertEquals(created.getField("title_s").getValue().toString(), "someEnteredValue");
	}

	private DataGroup createCollectedDataWithOneCollectedIndexDataTerm() {
		DataGroup collectedData = DataGroup.withNameInData("collectedData");
		collectedData.addChild(DataAtomic.withNameInDataAndValue("id", "someId"));
		collectedData.addChild(DataAtomic.withNameInDataAndValue("type", "someType"));

		DataGroup indexData = DataGroup.withNameInData("index");
		collectedData.addChild(indexData);

		DataGroup indexTerm = createCollectedIndexDataTermUsingIdAndValueAndRepeatId(
				"someIndexTerm", "someEnteredValue", "0");
		indexData.addChild(indexTerm);
		DataGroup extraData = createIndexExtraData("title", "indexTypeString");
		indexTerm.addChild(extraData);

		return collectedData;
	}

	private DataGroup createIndexExtraData(String indexFieldName, String indexType) {
		DataGroup extraData = DataGroup.withNameInData("extraData");
		extraData.addChild(DataAtomic.withNameInDataAndValue("indexFieldName", indexFieldName));
		extraData.addChild(DataAtomic.withNameInDataAndValue("indexType", indexType));
		return extraData;
	}

	private DataGroup createCollectedIndexDataTermUsingIdAndValueAndRepeatId(String id,
			String value, String repeatId) {
		DataGroup collectTerm = DataGroup.withNameInData("collectedDataTerm");
		collectTerm.addChild(DataAtomic.withNameInDataAndValue("collectTermId", id));
		collectTerm.addChild(DataAtomic.withNameInDataAndValue("collectTermValue", value));
		collectTerm.setRepeatId(repeatId);

		return collectTerm;
	}

	@Test
	public void testIndexDataCommittedToSolr() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;
		assertEquals(solrClientSpy.committed, false);

		DataGroup collectedData = createCollectedDataWithOneCollectedIndexDataTerm();
		recordIndexer.indexData(ids, collectedData, DataGroup.withNameInData("someDataGroup"));

		assertEquals(solrClientSpy.committed, true);
	}

	@Test
	public void testTwoCollectedIndexDataTerms() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		DataGroup collectedData = createCollectedDataWithOneCollectedIndexDataTerm();
		DataGroup collectedIndexDataTerm = createCollectedIndexDataTermUsingIdAndValueAndRepeatId(
				"someOtherIndexTerm", "someOtherEnteredValue", "1");
		collectedData.getFirstGroupWithNameInData("index").addChild(collectedIndexDataTerm);
		DataGroup extraData = createIndexExtraData("subTitle", "indexTypeText");
		collectedIndexDataTerm.addChild(extraData);

		recordIndexer.indexData(ids, collectedData, DataGroup.withNameInData("someDataGroup"));

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertEquals(created.getField("id").getValue().toString(), "someType_someId");
		assertEquals(created.getField("type").getValue().toString(), "someType");

		assertEquals(created.getField("title_s").getValue().toString(), "someEnteredValue");
		assertEquals(created.getField("subTitle_t").getValue().toString(), "someOtherEnteredValue");
	}

	@Test
	public void testTwoCollectedDataTermsUsingSameCollectIndexTermWithDifferentValues() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		DataGroup collectedData = createCollectedDataWithOneCollectedIndexDataTerm();
		DataGroup collectedIndexDataTerm = createCollectedIndexDataTermUsingIdAndValueAndRepeatId(
				"someIndexTerm", "someOtherEnteredValue", "1");
		collectedData.getFirstGroupWithNameInData("index").addChild(collectedIndexDataTerm);
		DataGroup extraData = createIndexExtraData("title", "indexTypeString");
		collectedIndexDataTerm.addChild(extraData);

		recordIndexer.indexData(ids, collectedData, DataGroup.withNameInData("someDataGroup"));

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertEquals(created.getField("id").getValue().toString(), "someType_someId");
		assertEquals(created.getField("type").getValue().toString(), "someType");

		Iterator<Object> iterator = created.getField("title_s").getValues().iterator();
		assertEquals(iterator.next().toString(), "someEnteredValue");
		assertEquals(iterator.next().toString(), "someOtherEnteredValue");
	}

	@Test(expectedExceptions = SolrIndexException.class)
	public void testExceptionFromSolrClient() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		((SolrClientProviderSpy) solrClientProvider).returnErrorThrowingClient = true;
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		DataGroup collectedData = createCollectedDataWithOneCollectedIndexDataTerm();
		DataGroup collectedIndexDataTerm = createCollectedIndexDataTermUsingIdAndValueAndRepeatId(
				"someOtherIndexTerm", "someOtherEnteredValue", "1");
		collectedData.addChild(collectedIndexDataTerm);

		recordIndexer.indexData(ids, collectedData, DataGroup.withNameInData("someDataGroup"));
	}

	@Test
	public void testDeleteFromIndex() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		recordIndexer.deleteFromIndex("someType", "someId");
		assertEquals(solrClientSpy.deletedId, "someType_someId");

		assertEquals(solrClientSpy.committed, true);
	}

	@Test(expectedExceptions = SolrIndexException.class)
	public void testDeleteFromIndexExceptionFromSolrClient() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		((SolrClientProviderSpy) solrClientProvider).returnErrorThrowingClient = true;
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		recordIndexer.deleteFromIndex("someType", "someId");
	}

	@Test
	public void testBooleanIndexType() {
		SolrInputDocument created = createTestDataForIndexType("indexTypeBoolean");

		assertEquals(created.getField("subTitle_b").getValue().toString(), "someOtherEnteredValue");
	}

	private SolrInputDocument createTestDataForIndexType(String indexType) {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		DataGroup collectedData = createCollectedDataWithOneCollectedIndexDataTerm();
		DataGroup collectedIndexDataTerm = createCollectedIndexDataTermUsingIdAndValueAndRepeatId(
				"someOtherIndexTerm", "someOtherEnteredValue", "1");
		collectedData.getFirstGroupWithNameInData("index").addChild(collectedIndexDataTerm);
		DataGroup extraData = createIndexExtraData("subTitle", indexType);
		collectedIndexDataTerm.addChild(extraData);

		recordIndexer.indexData(ids, collectedData, DataGroup.withNameInData("someDataGroup"));
		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;
		SolrInputDocument created = solrClientSpy.document;
		return created;
	}

	@Test
	public void testDateIndexType() {
		SolrInputDocument created = createTestDataForIndexType("indexTypeDate");
		assertEquals(created.getField("subTitle_dt").getValue().toString(),
				"someOtherEnteredValue");
	}

	@Test
	public void testNumberIndexType() {
		SolrInputDocument created = createTestDataForIndexType("indexTypeNumber");
		assertEquals(created.getField("subTitle_l").getValue().toString(), "someOtherEnteredValue");
	}

	@Test
	public void testIdIndexType() {
		SolrInputDocument created = createTestDataForIndexType("indexTypeId");
		assertEquals(created.getField("subTitle_s").getValue().toString(), "someOtherEnteredValue");
	}
}
