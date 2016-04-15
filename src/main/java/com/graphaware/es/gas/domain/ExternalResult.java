/*
 * Copyright (c) 2013-2016 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.es.gas.domain;

public class ExternalResult {

    private long nodeId;
    private String objectId;
    //private String item;
    private float score;

    public ExternalResult() {
        
    }
    public ExternalResult(String objectId, float score) {
        this.objectId = objectId;
        this.score = score;
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String uuid) {
        this.objectId = uuid;
    }

//    public String getItem() {
//        return item;
//    }
//
//    public void setItem(String item) {
//        this.item = item;
//    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    ///todo this is not used anywhere:
//    class Neo4JResultComparator implements Comparator<ExternalResult> {
//
//        @Override
//        public int compare(ExternalResult o1, ExternalResult o2) {
//            if (o1.score < o2.score) {
//                return -1;
//            }
//            if (o1.score > o2.score) {
//                return 1;
//            }
//            return 0;
//        }
//
//    }

}
