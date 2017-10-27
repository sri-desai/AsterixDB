/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.asterix.external.feed.runtime;

import java.io.Serializable;

import org.apache.asterix.external.feed.api.IFeedRuntime.FeedRuntimeType;
import org.apache.asterix.external.feed.management.FeedId;

public class FeedRuntimeId implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_TARGET_ID = "N/A";

    private final FeedId feedId;
    private final FeedRuntimeType runtimeType;
    private final int partition;
    private final String targetId;
    private final int hashCode;

    public FeedRuntimeId(FeedId feedId, FeedRuntimeType runtimeType, int partition, String targetId) {
        this.feedId = feedId;
        this.runtimeType = runtimeType;
        this.partition = partition;
        this.targetId = targetId;
        this.hashCode = toString().hashCode();
    }

    @Override
    public String toString() {
        return runtimeType + "(" + feedId + ")" + "[" + partition + "]" + "==>" + "{" + targetId + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FeedRuntimeId)) {
            return false;
        }
        FeedRuntimeId other = (FeedRuntimeId) o;
        return (other.feedId.equals(feedId) && other.getFeedRuntimeType().equals(runtimeType)
                && other.getTargetId().equals(targetId) && other.getPartition() == partition);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public FeedRuntimeType getFeedRuntimeType() {
        return runtimeType;
    }

    public int getPartition() {
        return partition;
    }

    public FeedRuntimeType getRuntimeType() {
        return runtimeType;
    }

    public String getTargetId() {
        return targetId;
    }

    public FeedId getFeedId() {
        return feedId;
    }
}
