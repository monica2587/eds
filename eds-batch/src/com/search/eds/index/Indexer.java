package com.search.eds.index;

import java.util.List;

import org.elasticsearch.client.Client;

public abstract class Indexer {
	public abstract boolean index(Client client, List<String> repoList);
	//TODO: convert to a factory class
}
