package com.search.eds;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;

import com.search.eds.index.CKANIndexer;
import com.search.eds.index.CustomIndexer;
import com.search.eds.index.Indexer;
import com.search.eds.index.SocrataIndexer;
import com.search.eds.utils.Config;

public class Main {

	public static void main(String[] args) {
		System.out.println("eds: Create search node instance...");
		
		Config thothConfig = Config.getInstance();
		// on startup - create a client node that joins the cluster "thoth"
		Node node = nodeBuilder().clusterName("eds").client(true).node();
		Client client = node.client();
		
		System.out.println("eds: Begin indexing...");
		Indexer indexer = null;
		boolean result = false;
		
		//index all Socrata repositories
		/*indexer = new SocrataIndexer();
		result = indexer.index(client, thothConfig.getSocrataRepoList());
		if(!result) {
			System.out.println("eds: Could not fully index all the Socrata reppositories. Please check logs for stacktrace");
		}*/
		
		//index all Custom repositories
		/*indexer = new CustomIndexer();
		result = indexer.index(client, thothConfig.getCustomRepoList());
		if(!result) {
			System.out.println("eds: Could not fully index all the HTML reppositories. Please check logs for stacktrace");
		}*/
		
		//index all CKAN repositories
		indexer = new CKANIndexer();
		result = indexer.index(client, thothConfig.getCKANRepoList());
		if(!result) {
			System.out.println("eds: Could not fully index all the CKAN reppositories. Please check logs for stacktrace");
		}
		
		// on shutdown
		node.close();
		System.out.println("eds: Completed indexing");
	}

}
