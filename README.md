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

Under active development, stay tuned.

License
-------

Copyright (c) 2015 GraphAware

GraphAware is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with this program.
If not, see <http://www.gnu.org/licenses/>.
