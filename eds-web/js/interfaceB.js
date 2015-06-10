/* global variables */
var aggResult = {};

/* hack for back button */
function dosearch() {
    document.location.hash = "all";
    search();
}

/* search function - invokes elasticsearch for the query */
function search() {
    /* extract search param */
    var term = $('#search-box').val();
    
    /* create connection to elasticsearch */
    var client = new $.es.Client({
        hosts: 'localhost:9200'
    });
    /* run search */
    client.search({
        "index" : "eds",
        "body" : {
            "size" : 1000,
            "min_score": 0.5,
            "query" : {
                "match" : {
                    "_all" : term
                }
            },
            "aggs": {
                "myagg1": {
                    "terms": {
                        "field": "datasetId",
                        "size": 0
                    }
                },
                "myagg2": {
                    "terms": {
                        "field": "tagList",
                        "size": 0
                    },
                    "aggs": {
                        "myagg3": {
                            "terms": {
                                "field": "datasetId",
                                "size": 0
                            }
                        }
                    }
                },
		"myagg4": {
                    "terms": {
                        "field": "org",
                        "size": 0
                    }
                }
            }
        }
    }).then(function (resp) {
        totalHits = resp.hits.total;
        aggResult['total'] = resp.hits.total;
        var buckets_in, buckets_out = [];
        buckets_in = (resp.aggregations.myagg1.buckets || []);
        for (var ii = 0; ii < buckets_in.length; ii++) {
            var bucket = {datasetId : buckets_in[ii].key, count : buckets_in[ii].doc_count }
            buckets_out.push(bucket);
        }
        aggResult['buckets'] = buckets_out;
	var orgs_in, orgs_out = [];
	orgs_in = (resp.aggregations.myagg4.buckets || []);
	for (var ii = 0; ii < orgs_in.length; ii++) {
	    var org = {orgName : orgs_in[ii].key, count : orgs_in[ii].doc_count}
	    orgs_out.push(org);
	}
	aggResult['orgs'] = orgs_out;
        var hits_in, hits_out = [];
        hits_in = (resp.hits || {}).hits || [];
        for (var ii = 0; ii < hits_in.length; ii++) {
            hits_out.push(hits_in[ii]._source);
        }
        aggResult['hits'] = hits_out;
        var tagbuckets_in, tagbuckets_out = [];
        tagbuckets_in = (resp.aggregations.myagg2.buckets || []);
        for (var ii = 0; ii < tagbuckets_in.length; ii++) {
            var jj = 0, tdbuckets_in, tdbuckets_out = [];
            tdbuckets_in = (tagbuckets_in[ii].myagg3.buckets || []);
            for (; jj < tdbuckets_in.length; jj++) {
                tdbuckets_out.push(tdbuckets_in[jj].key);
            }
            var bucket = {tag : tagbuckets_in[ii].key, datasets : tdbuckets_out}
            tagbuckets_out.push(bucket);
        }
        aggResult['tagbuckets'] = tagbuckets_out;
        display(aggResult);
    }, function (error) {
        console.trace(error.message);
    });
}

/* display search results from elasticsearch on page */
function display(aggResult) {
    var htmlStr = "";
    if (aggResult['orgs'] != null) {
	htmlStr += "<span class='result-label'>Redo search by publishing organization : </span>";
	htmlStr += "<select id='orgList' onchange='dosearchByOrg()'>";
	htmlStr += "<option value=''> Select option </option>";
	htmlStr += "<option value='all'> All organizations </option>";
	for (var ii = 0; ii < aggResult['orgs'].length; ii++) {
	    htmlStr += "<option value='" + aggResult['orgs'][ii].orgName + "'>";
	    htmlStr += aggResult['orgs'][ii].orgName + " (" + aggResult['orgs'][ii].count + ") "
	    htmlStr += "</option>";
	}
	htmlStr += "</select> <br />";
    }
    if (totalHits > 0) {
	htmlStr += "Found " + totalHits + " results";
    } else {
        htmlStr += "No results";
    }
    $(".results").html(htmlStr);
    //draw visualization
    computeNodesEdges(aggResult);
}

/* hack for back button */
function dosearchByOrg() {
    var orgName = ($( "#orgList option:selected" ).val()).trim();
    if (orgName === "all") {
	document.location.hash = "all";
	search();
    } else {
	document.location.hash = orgName;
	searchByOrg(orgName);
    }
}

/* Redo search by organization - same as search() except for org in search query */
function searchByOrg(orgName){
	/* extract search param */
	var term = $('#search-box').val();
	
	/* create connection to elasticsearch */
	var client = new $.es.Client({
	    hosts: 'localhost:9200'
	});
	/* run search */
	client.search({
	    "index" : "eds",
	    "body" : {
		"size" : 1000,
		"min_score": 0.5,
		"query" : {
		    "filtered": {
			"query": {
			    "match": { "_all" : term }
			},
			"filter": {
			    "terms" : { "org" : [orgName]}
			}
		    }
		},
		"aggs": {
		    "myagg1": {
			"terms": {
			    "field": "datasetId",
			    "size": 0
			}
		    },
		    "myagg2": {
			"terms": {
			    "field": "tagList",
			    "size": 0
			},
			"aggs": {
			    "myagg3": {
				"terms": {
				    "field": "datasetId",
				    "size": 0
				}
			    }
			}
		    },
		    "myagg4": {
			"terms": {
			    "field": "org",
			    "size": 0
			}
		    }
		}
	    }
	}).then(function (resp) {
	    totalHits = resp.hits.total;
	    aggResult['total'] = resp.hits.total;
	    var buckets_in, buckets_out = [];
	    buckets_in = (resp.aggregations.myagg1.buckets || []);
	    for (var ii = 0; ii < buckets_in.length; ii++) {
		var bucket = {datasetId : buckets_in[ii].key, count : buckets_in[ii].doc_count }
		buckets_out.push(bucket);
	    }
	    aggResult['buckets'] = buckets_out;
	    var orgs_in, orgs_out = [];
	    orgs_in = (resp.aggregations.myagg4.buckets || []);
	    for (var ii = 0; ii < orgs_in.length; ii++) {
		var org = {orgName : orgs_in[ii].key, count : orgs_in[ii].doc_count}
		orgs_out.push(org);
	    }
	    aggResult['orgs'] = orgs_out;
	    var hits_in, hits_out = [];
	    hits_in = (resp.hits || {}).hits || [];
	    for (var ii = 0; ii < hits_in.length; ii++) {
		hits_out.push(hits_in[ii]._source);
	    }
	    aggResult['hits'] = hits_out;
	    var tagbuckets_in, tagbuckets_out = [];
	    tagbuckets_in = (resp.aggregations.myagg2.buckets || []);
	    for (var ii = 0; ii < tagbuckets_in.length; ii++) {
		var jj = 0, tdbuckets_in, tdbuckets_out = [];
		tdbuckets_in = (tagbuckets_in[ii].myagg3.buckets || []);
		for (; jj < tdbuckets_in.length; jj++) {
		    tdbuckets_out.push(tdbuckets_in[jj].key);
		}
		var bucket = {tag : tagbuckets_in[ii].key, datasets : tdbuckets_out}
		tagbuckets_out.push(bucket);
	    }
	    aggResult['tagbuckets'] = tagbuckets_out;
	    display(aggResult);
	}, function (error) {
	    console.trace(error.message);
	});
}

/* invoke dosearch() when enter key is hit */
$(document).ready(function() {
    $('#search-box').keypress(function(e){
      if(e.keyCode==13) {
          $('#search-btn').click();
      }
    });
});

/* hack for back button */
$(window).load(function(){
    if ($('#search-box').val().trim().length > 0) {
	var orgName = document.location.hash.substring(1);
	if (orgName === "all") {
	    search();
	} else {
	    searchByOrg(orgName);
	}
    }
});

/* hack for back button */
window.onhashchange = function(){
    if ($('#search-box').val().trim().length > 0) {
	var orgName = document.location.hash.substring(1);
	if (orgName === "all") {
	    search();
	} else {
	    searchByOrg(orgName);
	}
    }
}