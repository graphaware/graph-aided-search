GraphAware Neo4j Elasticsearch Integration (ES Module)
======================================================

[![Build Status](https://magnum.travis-ci.com/graphaware/elasticsearch-to-neo4j.svg?token=tFjWxABA1S1VaGsxdhvX)](https://magnum.travis-ci.org/graphaware/elasticsearch-to-neo4j) | Latest Release: none yet!

GraphAware Elasticsearch Integration is an enterprise-grade bi-directional integration between Neo4j and Elasticsearch.
It consists of two independent modules plus a test suite. Both modules can be used independently or together to achieve
full integration.

The [first module](https://github.com/graphaware/neo4j-to-elasticsearch) is a plugin to Neo4j (more precisely, a [GraphAware Transaction-Driven Runtime Module](https://github.com/graphaware/neo4j-framework/tree/master/runtime#graphaware-runtime)),
which can be configured to transparently and asynchronously replicate data from Neo4j to Elasticsearch. This module is now
production-ready and officially supported by GraphAware for  <a href="http://graphaware.com/enterprise/" target="_blank">GraphAware Enterprise</a> subscribers.

The second module(this module) is a plugin to Elasticsearch that can consult the Neo4j database during an Elasticsearch query to enrich
the result (boost the score) by results that are more efficiently calculated in a graph database, e.g. recommendations.
This module is in active alpha development and isn't yet officially supported. We expect it to be production-ready by
the end of 2015.

# Elasticsearch -> Neo4j

## Feature Overview: Graph Aided Search

This module is a plugin for Elasticsearch that allow to improve the search result boosting or filtering them using data stored in the neo4j graph database. 
After performing the search on Elasticsearch, and just before returing results to the user, this plugin is able to submit some requests 
to the graph database through the REST api to get information needed to boost or filter the results and then get back the results to the user.

Two main features are exposed by the plugin: 

* **_Result Boosting_**: This feature allow to change the score value of the results. The score can be changed in different ways, 
mixing graph score with elasticsearch score or replacing it entirely are just two examples. 
It is possible to customize this behaviour with different formulas, rewriting some methods of the Graph Aided Search Booster. 
Usage examples include boosting (i) based on interest prediction (recommendations), (ii) based on friends interests/likes, (iii) whichever queries on neo4j
 
* **_Result Filtering_**: This feature allow to filter results removing documents from the results list. In this case, providing a cypher query, it is possible to return to the user only the document which id match results from cypher query.

More in details it operates in the following way:

1. Intercepts any “search” to the elasticsearch to find query extension;
2. Processes query extension identifying the type of the extension, if an boosting or a filter, and instantiates the related class;
3. Performs the operation required to boost or filter connecting to the neo4j rest API (or some extension to neo4j like Graphaware Recommendation Engine)
passing information needed, like cypher query, targe user, and so on;
4. Replies back to the user that submit the query.

## Usage: Installation

### Install Graph Aided Search Binary

    $ $ES_HOME/bin/plugin install com.graphaware/graph-aided-search/2.2.1

### Build from source

    $ git clone git@github.com:graphaware/elasticsearch-to-neo4j.git
    $ mvn clean deploy
    $ $ES_HOME/bin/plugin install file:///path/to/project/elasticsearch-to-neo4j/target/releases/elasticsearch-to-neo4j-2.2.1.zip
    
Start elasticsearch

### Configuration

Then configure indexes with the url of the neo4j. This can be done in two way:

    $ curl -XPUT http://localhost:9200/indexName/_settings?index.gas.neo4j.hostname=http://localhost:7474
    $ curl -XPUT http://localhost:9200/indexName/_settings?index.gas.enable=true

Or add it to the settings in the index template:

```
    POST _template/template_gas
    {
      "template": "*",
      "settings": {
        "index.gas.neo4j.hostname": "http://localhost:7474",
        "index.gas.enable": true
      }
    }
```

### Disable Plugin

The query will continue to work with no issue, even with the "gas-boost" and "gas-filter" piece in the query. They will be removed automatically.

    $ curl -XPUT http://localhost:9200/indexName/_settings?index.gas.enable=false

## Usage: Search Phase

The integration with already existing query is seamless, since the plugin requires to add only some new pieces into the query. 

### Booster example

If you would like to search for all the Movie in the es dataset you should run a query like this:

```
  curl -X POST http://localhost:9200/neo4j-index/Movie/_search -d '{
    "query" : {
        "match_all" : {}
    }';
```
In this case you'll get as score value 1 for all the results. If you would like to boost results accordingly to user interest computed by Graphaware 
Recommendation Plugin on top of Neo4j you should change the query in the following way.

```
  curl -X POST http://localhost:9200/neo4j-index/Movie/_search -d '{
    "query" : {
        "match_all" : {}
    },
    "gas-booster" :{
          "name": "GraphAidedSearchNeo4jBooster",
          "recoTarget": "2",
          "maxResultSize": 10,
          "keyProperty": "objectId",
          "neo4j.endpoint": "/graphaware/recommendation/movie/filter/"
       }
  }';
```
The **_gas-booster_** clause identify the type of operation, in this case it is required a boost operation. 
The **_name_** parameter is mandatory and allows to specify the Booster class. The remaining parameters depends on the type of booster.
In the following paragraph the available boosters are described.

#### GraphAidedSearchNeo4jBooster

This booster connects to a neo4j instance using some REST API available as plugin for the database. 

This is the list of the parameters available for this booster:

* **recoTarget**: (mandatory) This parameter contains the identifier of the target for which the boosting values are computed. 
Since the boosting is customized accordingly to a target, this parameter is mandatory and allow to get different results for different target.
* **maxResultSize**: (Default is set to the max result windows size of elasticsearch, defined by the parameter index.max_result_window) 
When search query is changed before submitting it to elasticsearch engine, the value of "size" for the results returned is changed accordingly to this parameter.
This is necessary since once the bosting function is applied the order may change so that some of the results that fall out of size may be boosted and fall in the "size" window.
* **keyProperty**: (Default value is uuid) the id of each document in the search results must match some property value of the nodes in the graph. 
In order to avoid ambiguities in the results this property must identify a single node, for this reason is defined as key property.
* **operator**: (Default is multiply [*]) It specifies how to compose elasticsearch score with neo4j provided score. 
Available operators are: * (multiply), + (sum), - (substract), / (divide), replace (replace score). 
* **neo4j.endpoint**: (Default /graphaware/recommendation/filter) It defines the endpoint to which submit the request to get the new boosting. 
It is added to the neo4j host value defined for the index.

It passes information about the list of the ids that should be boosted as well as thetarget 
The REST API should expose a POST endpoint that admit the following parameters: 

* **target** (url parameter): This is the value of recoTarget defined before and it is used to identify the user or item for which the score will be computed from the recommender;
* **limit**: This value can be used to limit the number of results provided be the REST API;
* **from**: In order to support pagination this value allow to avoid send back again the first results in the list;
* **keyProperty**: Specify the property on the nodes used to identify the nodes. Such property will be used to filter the results, accordingly to the lists of "ids";
* **ids**: This comma separated list of identifier of the nodes that must be evaluated and then returned;

This is an example of the call:

```
http://localhost:7474/graphaware/recommendation/movie/filter/2

limit=2147483647&from=0&keyProperty=objectId&ids=99,166,486,478,270,172,73,84,351,120
```
And return a json with the following strcuture.

```
[
  {
    nodeId: 1212,
    objectId: "270",
    score: 3
  },
  {
    nodeId: 1041,
    objectId: "99",
    score: 1
  },
  {
    nodeId: 1420,
    objectId: "478",
    score: 1
  },
  {
    nodeId: 1428,
    objectId: "486",
    score: 1
  }
]
```

#### GraphAidedSearchCypherBooster

dsada



### Filter example


The _gas-filter_ clause identify the type of operation, in this case it is required a filter operation.

The following Filter classes are alrea

## Customize the plugin

The plugin allows to implement custom booster and custom filter. In order to implements ...

Here an example

## Version Matrix

The following version are currently supported

| Version   | Elasticsearch |
|:---------:|:-------------:|
| master    | 2.3.x         |
| 2.2.1.x   | 2.2.1         |
| 2.2.0.x   | 2.2.0         |
| 2.1.1.x   | 2.1.1         |

### Issues/Questions

Please file an [issue](https://github.com/graphaware/elasticsearch-to-neo4j/issues "issue").

License
-------

Copyright (c) 2016 GraphAware

GraphAware is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with this program.
If not, see <http://www.gnu.org/licenses/>.
