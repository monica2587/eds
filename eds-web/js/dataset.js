/* search function - invokes elasticsearch for the query */
function search() {
    /* extract search param */
    var term = (document.location.search.substring(1).split("="))[1];
    
    /* create connection to elasticsearch */
    var client = new $.es.Client({
        hosts: 'localhost:9200'
    });
    /* run search */
    client.search({
        "index" : "eds",
        "body" : {
            "size" : 50,
            "query" : {
                "match" : {
                    "datasetId" : term
                }
            }
        }
    }).then(function (resp) {
        var ii = 0, hits_in, hits_out = [];
        hits_in = (resp.hits || {}).hits || [];
        for (; ii < hits_in.length; ii++) {
            hits_out.push(hits_in[ii]._source);
        }
        display(hits_out);
    }, function (error) {
        console.trace(error.message);
    });
}

/* display search results from elasticsearch on page */
function display(hits_out) {
    var htmlStr = "";
    htmlStr += "<article class='result'>";
    htmlStr += "<p>The variable you selected is present in the dataset:</p>";
    htmlStr += "<h2>" + hits_out[0].datasetName + "</h2>";
    htmlStr += "<span class='result-label'>External dataset URL: </span> <a href='" + hits_out[0].datasetUrl + "'>" + hits_out[0].datasetUrl + "</a> <hr />";
    htmlStr += "<p><span class='result-label'>Publishing organization :</span>" + hits_out[0].org + "</p>";
    htmlStr += "<p><span class='result-label'>Tags :</span>";
    if (hits_out[0].tagList != null) {
	for(var jj = 0; jj < hits_out[0].tagList.length; jj++) {
	    htmlStr += "<span> " + hits_out[0].tagList[jj] + "&nbsp;&nbsp; </span>"
	}
    }
    htmlStr += "</p>";
    htmlStr += "<p><span class='result-label'>Available formats :</span>";
    if (hits_out[0].formatList != null) {
	for(var jj = 0; jj < hits_out[0].formatList.length; jj++) {
            htmlStr += "<span> " + hits_out[0].formatList[jj] + "&nbsp;&nbsp; </span>"
	}
    }
    htmlStr += "</p>";
    htmlStr += "<p><span class='result-label'>Access dataset files directly from the following links:</span>"
    if (hits_out[0].urlList != null) {
	for(var jj = 0; jj < hits_out[0].urlList.length; jj++) {
            htmlStr += "<div> <a href='" + hits_out[0].urlList[jj] + "'>" + hits_out[0].urlList[jj] + "&nbsp;&nbsp; </a></div>"
	}
    }
    htmlStr += "</p>";
    htmlStr += "</article>";
    htmlStr += "<article class='result'>";
    htmlStr += "<p><span class='result-label'>All variables in this dataset (count = " + hits_out.length + "):</span></p>";
    for(var ii = 0; ii < hits_out.length; ii++ ) {
        htmlStr += hits_out[ii].name + "<br />";        
    }
    htmlStr += "</article>";
    $(".results").html(htmlStr);
}