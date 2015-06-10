var network;

var nodes = new vis.DataSet();
var edges = new vis.DataSet();

function computeNodesEdges(aggResult){
	//console.log(aggResult);
	// get query param
	qTerm = location.search.substring(3).toLowerCase();
	
	// create an array with nodes
	nodesData = [];
	// create an array with edges
	edgesData = [];
	var buckets = aggResult['buckets'];
	var map = {};
	var datasetIdNameMap = {}
	for(index = 0; index < buckets.length; index ++) {
		var bucket = buckets[index].datasetId;
		map[bucket] = index;
	}
	var arr = aggResult['hits'];
	for (index = 0; index < arr.length; index++) {
		//set the size of node
		if (arr[index].name.toLowerCase() ===  qTerm) {
			//if name of variable is an exact match with query term
			val = 3;
		} else if(arr[index].name.toLowerCase().indexOf(qTerm) > -1) {
			//if query term is contained in the name of the variable
			val = 2;
			
		} else val = 1;
		datasetIdNameMap[arr[index].datasetId] = arr[index].datasetName;
		//create node for each variable
		nodesData.push({
	          id: "v" + index,
	          label: arr[index].name,
		  value: val,
	          group : map[arr[index].datasetId],
		  extra: "<a href='datasetB.html?q=" + arr[index].datasetId + "' target='_self'><h2>" + arr[index].name + "</h2></a>" +
			"<span class='result-label'>Publishing organization: </span>" + arr[index].org + "<br />" +
			"<span class='result-label'>Description: </span>" + arr[index].description + "<br />" +
			"<span class='result-label'>External dataset URL: </span> <a href='" + arr[index].datasetUrl + "'>" + arr[index].datasetUrl + "</a> <br />"
	        });
		//TODO: potentially replace label for dataset from the mapping here
		//create edge between variable and corresponding dataset
		edgesData.push({
		  from: "v" + index,
		  to: "d" + map[arr[index].datasetId],
		  value: 1
		});
	}
	for(index = 0; index < buckets.length; index ++) {
		//create a node for each dataset
		nodesData.push({
		  id: "d" + index,
		  label: datasetIdNameMap[buckets[index].datasetId],
		  title: datasetIdNameMap[buckets[index].datasetId],
		  shape: 'box',
	          group : index
		});
	}
	//use tag data to draw edges between datasets
	var tagBuckets = aggResult['tagbuckets'];
	for (index = 0; index < tagBuckets.length; index++) {
		var relatedDatasets = tagBuckets[index].datasets;
		for (i = 0; i < (relatedDatasets.length - 1); i++) {
			for(j = i+1; j < relatedDatasets.length; j++) {
				edgesData.push({
				  from: "d" + map[relatedDatasets[i]],
				  to: "d" + map[relatedDatasets[j]],
				  value: 1
				});
			}
		}
	}

    redrawAll(nodesData, edgesData);
}

function redrawAll(nodesData, edgesData) {
    nodes.clear();
    edges.clear();

    network = null;
    
    // create a network
    nodes.add(nodesData);
    edges.add(edgesData);

    var container = document.getElementById('mynetwork');
    var data = {
        nodes: nodes,
        edges: edges
    };
    var options = {
        nodes: {
            shape: 'dot',
            radiusMin: 10,
            radiusMax: 30,
            fontSize: 12,
            fontFace: "Tahoma"
        },
        edges: {
            width: 0.15,
            inheritColor: "from"
        },
        tooltip: {
            delay: 200,
            fontSize: 12,
            color: {
                background: "#fff"
            }
        },
        smoothCurves: {dynamic:false, type: "continuous"},
        stabilize: false,
	physics: {barnesHut: {gravitationalConstant: -2000, centralGravity: 0.2, springConstant: 0.02, springLength: 200, damping: 0.1}},
        //physics: {repulsion: { centralGravity: 0.1, springLength: 95, springConstant: 0.05, nodeDistance: 100, damping: 0.09}},
        hideEdgesOnDrag: true
    };

    network = new vis.Network(container, data, options);
    network.on("click",onClick);
}


function onClick(selectedItems) {
    var nodeId;
    var degrees = 2;
    // we get all data from the dataset once to avoid updating multiple times.
    var allNodes = nodes.get({returnType:"Object"});
    // first, render the info div
    if (selectedItems.nodes[0] != undefined && selectedItems.nodes[0].indexOf("v") >= 0 && allNodes[selectedItems.nodes[0]] != undefined) {
	document.getElementById('info').innerHTML = allNodes[selectedItems.nodes[0]].extra;
	document.getElementById('info').style.display = 'block';
    } else {
	document.getElementById('info').innerHTML = "";
    }
    // now, deal with selection or deselection
    if (selectedItems.nodes.length == 0) {
        // restore on unselect
        for (nodeId in allNodes) {
            if (allNodes.hasOwnProperty(nodeId)) {
                allNodes[nodeId].color = undefined;
                if (allNodes[nodeId].oldLabel !== undefined) {
                    allNodes[nodeId].label = allNodes[nodeId].oldLabel;
                    allNodes[nodeId].oldLabel = undefined;
                }
                allNodes[nodeId]['levelOfSeperation'] = undefined;
                allNodes[nodeId]['inConnectionList'] = undefined;
            }
        }
    }
    else {
        var allEdges = edges.get();

        // we clear the level of separation in all nodes.
        clearLevelOfSeperation(allNodes);

        // we will now start to collect all the connected nodes we want to highlight.
        var connectedNodes = selectedItems.nodes;

        // we can store them into levels of separation and we could then later use this to define a color per level
        // any data can be added to a node, this is just stored in the nodeObject.
        storeLevelOfSeperation(connectedNodes,0, allNodes);
        for (var i = 1; i < degrees + 1; i++) {
            appendConnectedNodes(connectedNodes, allEdges);
            storeLevelOfSeperation(connectedNodes, i, allNodes);
        }
        for (nodeId in allNodes) {
            if (allNodes.hasOwnProperty(nodeId)) {
                if (allNodes[nodeId]['inConnectionList'] == true) {
                    if (allNodes[nodeId]['levelOfSeperation'] !== undefined) {
                        if (allNodes[nodeId]['levelOfSeperation'] >= 2) {
                            allNodes[nodeId].color = 'rgba(150,150,150,0.75)';
                        }
                        else {
                            allNodes[nodeId].color = undefined;
                        }
                    }
                    else {
                        allNodes[nodeId].color = undefined;
                    }
                    if (allNodes[nodeId].oldLabel !== undefined) {
                        allNodes[nodeId].label = allNodes[nodeId].oldLabel;
                        allNodes[nodeId].oldLabel = undefined;
                    }
                }
                else {
                    allNodes[nodeId].color = 'rgba(200,200,200,0.5)';
                    if (allNodes[nodeId].oldLabel === undefined) {
                        allNodes[nodeId].oldLabel = allNodes[nodeId].label;
                        allNodes[nodeId].label = "";
                    }
                }
            }
        }
    }
    var updateArray = [];
    for (nodeId in allNodes) {
        if (allNodes.hasOwnProperty(nodeId)) {
            updateArray.push(allNodes[nodeId]);
        }
    }
    nodes.update(updateArray);
}


/**
 * update the allNodes object with the level of separation.
 * Arrays are passed by reference, we do not need to return them because we are working in the same object.
 */
function storeLevelOfSeperation(connectedNodes, level, allNodes) {
    for (var i = 0; i < connectedNodes.length; i++) {
        var nodeId = connectedNodes[i];
        if (allNodes[nodeId]['levelOfSeperation'] === undefined) {
            allNodes[nodeId]['levelOfSeperation'] = level;
        }
        allNodes[nodeId]['inConnectionList'] = true;
    }
}

function clearLevelOfSeperation(allNodes) {
    for (var nodeId in allNodes) {
        if (allNodes.hasOwnProperty(nodeId)) {
            allNodes[nodeId]['levelOfSeperation'] = undefined;
            allNodes[nodeId]['inConnectionList'] = undefined;
        }
    }
}

/**
 * Add the connected nodes to the list of nodes we already have
 *
 *
 */
function appendConnectedNodes(sourceNodes, allEdges) {
    var tempSourceNodes = [];
    // first we make a copy of the nodes so we do not extend the array we loop over.
    for (var i = 0; i < sourceNodes.length; i++) {
        tempSourceNodes.push(sourceNodes[i])
    }

    for (var i = 0; i < tempSourceNodes.length; i++) {
        var nodeId = tempSourceNodes[i];
        if (sourceNodes.indexOf(nodeId) == -1) {
            sourceNodes.push(nodeId);
        }
        addUnique(getConnectedNodes(nodeId, allEdges),sourceNodes);
    }
    tempSourceNodes = null;
}

/**
 * Join two arrays without duplicates
 * @param fromArray
 * @param toArray
 */
function addUnique(fromArray, toArray) {
    for (var i = 0; i < fromArray.length; i++) {
        if (toArray.indexOf(fromArray[i]) == -1) {
            toArray.push(fromArray[i]);
        }
    }
}

/**
 * Get a list of nodes that are connected to the supplied nodeId with edges.
 * @param nodeId
 * @returns {Array}
 */
function getConnectedNodes(nodeId, allEdges) {
    var edgesArray = allEdges;
    var connectedNodes = [];

    for (var i = 0; i < edgesArray.length; i++) {
        var edge = edgesArray[i];
        if (edge.to == nodeId) {
            connectedNodes.push(edge.from);
        }
        else if (edge.from == nodeId) {
            connectedNodes.push(edge.to)
        }
    }
    return connectedNodes;
}

//redrawAll();
                 