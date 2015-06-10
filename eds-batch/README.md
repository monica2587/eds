The exploratory data search system consists of three parts:

1. A search engine that indexes datasets

2. A batch application that can scrape the data portals for datasets and index them

3. A web application that queries the search engine and displays the results.

** This is the batch application. **



To start indexing, 

1. Ensure Elasticsearch is up and running

2. Run Main.java


To change the data portals that are indexed,

1. Open resources/config.txt

2. Add/edit entry. It is important to know what platform the portal is hosted on (CKAN or Socrata) so that the right parser is picked up.
 


To Set up EDS for the first time on a new machine

1. Install JDK

2. Download + install elasticsearch
	1. Install Marvel
	2. Change config/elasticsearch.yml - cluster name and publish address
	3. Start elasticsearch

3. Download + install apache tomcat

4. Download + install eclipse

5. Application setup
	1. Copy eds-batch project to eclipse workspace
	2. Run eds-batch project in eclipse
	3. Verify that datasets are indexed
	4. Copy webapp to apache tomcat webapps folder
	5. Run tomcat
	6. Verify that application pages are loaded
