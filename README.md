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
 
* **_Result Filtering_**: This feature allow to filter results removing documents from the results list. In this case providing a cypher query it is possible to return to the user only the document which id match results from cypher query.

## Usage: Search Phase

The integration with already existing query is seamlessy, since it require to add some pieces to the query. 

## Getting the Software

### Download the binary

    $ $ES_HOME/bin/plugin install com.graphaware/graph-aided-search/2.2.1

### Build from source

## Customize the plugin

## Version Matrix

The following version are currently supported

| Version   | Elasticsearch |
|:---------:|:-------------:|
| master    | 2.2.X         |
| 2.2.1     | 2.2.1         |

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
