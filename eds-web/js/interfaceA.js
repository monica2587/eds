/* hack for back button */
function dosearch() {
    document.location.hash = 0;
    $('#pagination').hide();
    search();
}

/* search function - invokes elasticsearch for the query */
function search() {
    /* extract search param */
    var term = $('#search-box').val();
    /* hack for back button */
    var startFrom = document.location.hash.substring(1);
    
    /* create connection to elasticsearch */
    var client = new $.es.Client({
        hosts: 'localhost:9200'
    });
    /* run search */
    client.search({
        "index" : "eds",
        "body" : {
            "size" : 10,
            "min_score": 0.5,
            "from" : startFrom,
            "query" : {
                "match" : {
                    "_all" : term
                }
            }
        }
    }).then(function (resp) {
        totalHits = resp.hits.total;
        var ii = 0, hits_in, hits_out = [];
        hits_in = (resp.hits || {}).hits || [];
        for (; ii < hits_in.length; ii++) {
            hits_out.push(hits_in[ii]._source);
        }
        display(totalHits, hits_out);
    }, function (error) {
        console.trace(error.message);
    });
}

/* display search results from elasticsearch on page */
function display(totalHits, hits_out) {
    var htmlStr = "";
    if (totalHits > 0) {
        htmlStr += "Found " + totalHits + " results";
	var startFrom = document.location.hash.substring(1);
        $('#pagination').bootpag({
            total: Math.ceil(totalHits/10),
            maxVisible: 10,
	    page : (startFrom/10 + 1)
        }).on('page', function(event, num){
	    startFrom = (num - 1)*10;
	    /* hack for back button */
	    document.location.hash = startFrom;
	    search();
        });
	$('#pagination').show();
    } else {
        htmlStr += "No results";
    }
    for(var ii = 0; ii < hits_out.length; ii++ ) {
        htmlStr += "<article class='result'>";
        htmlStr += "<a href='datasetA.html?q=" + hits_out[ii].datasetId + "' target='_self'><h2>" + hits_out[ii].name + "</h2></a>";
	htmlStr += "<span class='result-label'>Publishing organization: </span>" + hits_out[ii].org + "<br />";
        htmlStr += "<span class='result-label'>Description: </span>" + hits_out[ii].description + "<br />";
        htmlStr += "<span class='result-label'>External dataset URL: </span> <a href='" + hits_out[ii].datasetUrl + "'>" + hits_out[ii].datasetUrl + "</a> <br />";
        htmlStr += "<p></p>";
        htmlStr += "</article>"
    }
    $(".results").html(htmlStr);
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
    var startFrom = document.location.hash.substring(1);
    if ($('#search-box').val().trim().length > 0) {
        search();
    }
});

/* hack for back button */
window.onhashchange = function(){
    var startFrom = document.location.hash.substring(1);
    if ($('#search-box').val().trim().length > 0) {
        search();
    }
}