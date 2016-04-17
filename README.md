# GraphAware Graph-Aided Search

## ElasticSearch Plugin providing integration with Neo4j

[![Build Status](https://travis-ci.org/graphaware/graph-aided-search.svg?branch=master)](https://travis-ci.org/graphaware/graph-aided-search) | Latest Release: 2.2.2.0

GraphAware Graph-Aided Search is an enterprise-grade bi-directional integration between Neo4j and Elasticsearch. It consists
of two independent modules plus test suites. Both modules can be used independently or together to achieve full integration.

The [first module](https://github.com/graphaware/neo4j-to-elasticsearch) is a plugin for Neo4j (more precisely, a [GraphAware Transaction-Driven Runtime Module](https://github.com/graphaware/neo4j-framework/tree/master/runtime#graphaware-runtime)),
which can be configured to transparently and asynchronously replicate data from Neo4j to ElasticSearch.

The second module (this module) is a plugin for Elasticsearch that can query the Neo4j graph database during a search query
to enrich the result (boost the score) by results that are more efficiently calculated in a graph database, e.g. recommendations.

Both modules are now open-source production-ready for everyone. They are also officially supported by GraphAware for <a href="http://graphaware.com/enterprise/" target="_blank">GraphAware Enterprise</a> subscribers.

## Feature Overview: Graph-Aided Search

This module is a plugin for Elasticsearch that enables users to improve search results by boosting or filtering them using data stored in the Neo4j graph database.
After performing a search in Elasticsearch, just before returning the results to the user, this plugin requests additional information from Neo4j via its REST API in order to boost or filter the results.

Two main features are exposed by the plugin: 

* **_Result Boosting_**: This feature allows changing the scores of the results. The score can be changed in different ways: mixing graph score with Elasticsearch score or replacing it entirely are just two examples.
It is possible to customize this behaviour with different formulas, rewriting some methods of the Graph-Aided Search Booster.
Usage examples include boosting (i) based on interest prediction (recommendations), (ii) based on friends' interests/likes, (iii) all use cases that are a good fit for Neo4j
 
* **_Result Filtering_**: This feature allows filtering, thus removing documents from the results list. By providing a Cypher query, it is possible to return to the user only documents with IDs matching the results of the Cypher query.

Detailed workflow:

1. Intercept and parse any "Search query" and try to find the `GraphAidedSearch` extension parameter;
2. Process the query extension identifying the type of the extension (boosting or a filter), and instantiate the related class;
3. Perform the operation required to boost or filter by calling the Neo4j REST API (or a Neo4j extension like [Graphaware Recommendation Engine](https://github.com/graphaware/neo4j-reco), passing all necessary information,
e.g. Cypher query, target user, etc...;
4. Return the filtered/boosted result set back to the user;

![overview](https://s3-eu-west-1.amazonaws.com/graphaware/assets/graphAidedSearchIntro2.png)

---

## Usage: Installation

### Install Graph-Aided Search Binary

```bash
$ $ES_HOME/bin/plugin install com.graphaware.es/graph-aided-search/2.2.2.0
```

### Build from source

```bash
$ git clone git@github.com:graphaware/graph-aided-search.git
$ mvn clean package
$ $ES_HOME/bin/plugin install file:///path/to/project/graph-aided-search/target/releases/graph-aided-search-2.2.2.0.zip
```

Start elasticsearch

### Configuration

Then configure indexes with the url of Neo4j. This can be done in two ways. First:

```bash
$ curl -XPUT http://localhost:9200/indexName/_settings?index.gas.neo4j.hostname=http://localhost:7474
$ curl -XPUT http://localhost:9200/indexName/_settings?index.gas.enable=true
```

If the Neo4j Rest Api is protected by Basic Authentication confire username and password for neo4j in the following way:

```bash
$ curl -XPUT http://localhost:9200/indexName/_settings?index.gas.neo4j.user=neo4j
$ curl -XPUT http://localhost:9200/indexName/_settings?index.gas.neo4j.password=password
```

Second, you can use also template to configure settings in the index:

```json
    POST _template/template_gas
    {
      "template": "*",
      "settings": {
        "index.gas.neo4j.hostname": "http://localhost:7474",
        "index.gas.enable": true,
        "index.gas.neo4j.user": "neo4j",
        "index.gas.neo4j.password": "password"
      }
    }
```

### Disable Plugin

```bash
$ curl -XPUT http://localhost:9200/indexName/_settings?index.gas.enable=false
```

Queries will continue to work even with Graph-Aided-Search-specific elements, e.g. "gas-boost" and "gas-filter".

## Usage: Search Phase

The integration with a pre-existing search query is seamless, since the plugin only requires the addition of new elements into the query.

### Booster example

Boosters allow to change the score by an external score source. This could be a `recommender`, a Cypher query, or any custom booster provider.
A simple Elasticsearch query could have the following structure:

```bash
  curl -X POST http://localhost:9200/neo4j-index/Movie/_search -d '{
    "query" : {
        "match_all" : {}
    }';
```

In this case all the Elasticsearch result hits will have a relevancy score value of `1`. If you would like to boost these results according to user interest computed by Graphaware Recommendation Plugin on top of
Neo4j, you would change the query in the following way.

```bash
  curl -X POST http://localhost:9200/neo4j-index/Movie/_search -d '{
    "query" : {
        "match_all" : {}
    },
    "gas-booster" :{
          "name": "SearchResultNeo4jBooster",
          "target": "2",
          "maxResultSize": 10,
          "keyProperty": "objectId",
          "neo4j.endpoint": "/graphaware/recommendation/movie/filter/"
       }
  }';
```
The **_gas-booster_** clause identifies the type of operation, in this case it defines a boost operation.
The **_name_** parameter is mandatory and allows to specify the Booster class. The remaining parameters depend on the type of the booster.
In the following paragraph the available boosters are described.

#### SearchResultNeo4jBooster

This booster uses Neo4j through custom REST APIs available as plugins for the database. In this case, the _name_ value must be set to `SearchResultNeo4jBooster`.

The following parameters are available for this booster:

* **target**: (Mandatory) This parameter contains the identifier of the target for which the boosting values are computed.
Since the boosting is customized according to a target, this parameter is mandatory and allows getting different results for different target (typically a user).

* **maxResultSize**: (Default is set to the max result windows size of elasticsearch, defined by the parameter index.max_result_window)
When search query is changed before submitting it to elasticsearch engine, the value of "size" for the results returned is changed according to this parameter.
This is necessary since once the boosting function is applied, the order may change. Some of the results that wouldn't "make it" may be boosted and fall into the "size" window.

* **keyProperty**: (Default value is `uuid`) the id of each document in the search results must match some property value of the nodes in the graph.
In order to avoid ambiguities in the results, this property must identify a single node. Using <a href="https://github.com/graphaware/neo4j-uuid" target="_blank">GraphAware UUID</a> with Neo4j is recommended for this purpose.

* **operator**: (Default is multiply [*]) It specifies how to combine the Elasticsearch score with the score provided by Neo4j.
Available operators are: * (multiply), + (sum), - (substract), / (divide), replace (replace score).

* **neo4j.endpoint**: (Default /graphaware/recommendation/filter) It defines the endpoint to which the request is submitted in order to get a boosting.
It is added to the Neo4j host value defined for the index.

Information about the list of IDs that should be boosted as well as the target is passed to the API running atop Neo4j. The REST API should expose a POST endpoint that accepts the following parameters:

* **target** (url parameter): This is the value of target defined above and it is used to identify the user or item for which the score will be computed from the recommender;

* **limit**: This value can be used to limit the number of results provided be the REST API;

* **from**: In order to support pagination this value allows to skip a number of results;

* **keyProperty**: Specify the property on the nodes used to identify the nodes. Such property will be used to filter the results, according to the lists of "ids";

* **ids**: Comma-separated list of node identifiers that must be evaluated and then returned;

Example Call:

```
http://localhost:7474/graphaware/recommendation/movie/filter/2

Parameters:
limit=2147483647&from=0&keyProperty=objectId&ids=99,166,486,478,270,172,73,84,351,120
```
This component supposes that the results are a json array with the following structure.

```json
[
  {
    "nodeId": 1212,
    "objectId": "270",
    "score": 3
  },
  {
    "nodeId": 1041,
    "objectId": "99",
    "score": 1
  },
  {
    "nodeId": 1420,
    "objectId": "478",
    "score": 1
  },
  {
    "nodeId": 1428,
    "objectId": "486",
    "score": 1
  }
]
```

#### SearchResultCypherBooster

This booster uses Neo4j through custom REST APIs available as plugins for the database. In this case the _name_ value must be set to `SearchResultCypherBooster`.

The following parameters are available for this booster:

* **query**: (Mandatory) This parameter contains the query to submit to the Neo4j instance.

* **scoreName**: (Default value is "score") The name of the returned value that is used as scoring function.

* **identifier**: (Default value is "id") The name of the returned value that is used for matching IDs.

* **maxResultSize**: (Default is set to the max result windows size of elasticsearch, defined by the parameter index.max_result_window)
When search query is changed before submitting it to elasticsearch engine, the value of "size" for the results returned is changed according to this parameter.
This is necessary since once the boosting function is applied, the order may change. Some of the results that wouldn't "make it" may be boosted and fall into the "size" window.

* **operator**: (Default is multiply [*]) It specifies how to combine the Elasticsearch score with the score provided by Neo4j.
Available operators are: * (multiply), + (sum), - (substract), / (divide), replace (replace score).

The Elasticsearch result hits ids are passed as Cypher query parameter as a `List` of strings named `items`.

Example Use:

```
  curl -X POST http://localhost:9200/neo4j-index/Movie/_search -d '{
    "query" : {
        "match_all" : {}
    },
    "gas-booster" :{
          "name": "SearchResultCypherBooster",
          "query": "MATCH (input:User) WHERE id(input) = 2
                    MATCH p=(input)-[r:RATED]->(movie)<-[r2:RATED]-(other)
                    WITH other, collect(p) as paths
                    WITH other, reduce(x=0, p in paths | x + reduce(i=0, r in rels(p) | i+r.rating)) as score
                    WITH other, score
                    ORDER BY score DESC
                    MATCH (other)-[:RATED]->(reco)
                    RETURN reco.objectId as id, score
                    LIMIT 500",
          "maxResultSize": 1000,
          "scoreName": "score",
          "identifier": "id"
       }
  }';
```

### Filter Example

Filters allow to filter the results using information stored in the graph. For example, you can filter movies based on what the user's friends have seen.
If you would like to filter results according to a user's friends evaluation, it is possible to change the Elasticsearch query as follows:

```
  curl -X POST http://localhost:9200/neo4j-index/Movie/_search -d '{
    "query" : {
        "match_all" : {}
    },
    "gas-booster" :{
          "name": "SearchResultCypherFilter",
          "query": "MATCH (input:User) WHERE id(input) = 2
                   MATCH (input)-[f:FRIEND_OF]->(friend)-[r:RATED]->(movie)
                   WHERE r.rate > 3
                   RETURN movie.objectId",
          "shouldExclude": false
       }
  }';
```

The **_gas-filter_** clause identifies the type of the operation; in this case a filter operation.
The **_name_** parameter is mandatory and allows to specify the Filter class. The remaining parameters depends on the type of filter.
In the following paragraph the available filters are described.

#### SearchResultCypherFilter

This filter allows to filter results using a Cypher query on Neo4j. In this case the _name_ value must be set to `SearchResultCypherFilter`.

The following parameters are available for this filter:

* **query**: (Mandatory) This parameter contains the query to submit to the Neo4j instance.

* **maxResultSize**: (Default is set to the max result windows size of elasticsearch, defined by the parameter index.max_result_window)
When search query is changed before submitting it to elasticsearch engine, the value of "size" for the results returned is changed according to this parameter.
This is necessary since once the filtering function is applied, some of the results that wouldn't "make it" may fall into the "size" window.

* **shouldExclude**: (Default true) This parameter allows to define the behaviour of the Filter.
If set to true (default), it will filter out the Neo4j results from the results provided by Elasticsearch. If set to false, it will
keep the intersection of Neo4j and Elasticsearch results, i.e. exclude everything that has not been returned by Neo4j.

## Customize the plugin

The plugin allows to implement custom boosters and filters. In order to implement a booster, `SearchResultBooster` must be implemented
and it needs to have the following annotation:

```
@SearchBooster(name = "MyCustomBooster")
```
Moreover, it should be in the package `com.graphaware.es.gas`.

In order to implement a filter, `SearchResultFilter` must be implemented and it needs to have the following annotation:

```
@SearchFilter(name = "MyCustomFilter")
```

Also in this case, it should be in the package `com.graphaware.es.gas`.

## Version Matrix

The following version are currently supported

| Version (this project)   | Elasticsearch |
|:---------:|:-------------:|
| master    | 2.2.2         |
| 2.2.2.x   | 2.2.2         |

### Issues/Questions

Please file an [issue](https://github.com/graphaware/graph-aided-search/issues "issue").

License
-------

Copyright (c) 2016 GraphAware

GraphAware is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with this program.
If not, see <http://www.gnu.org/licenses/>.
