package se.uu.ub.cora.solrsearch;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.searchstorage.SearchStorage;

public class SearchStorageSpy implements SearchStorage {

	public List<String> searchTermIds = new ArrayList<>();

	@Override
	public DataGroup getSearchTerm(String searchTermId) {
		searchTermIds.add(searchTermId);
		return null;
	}

}
