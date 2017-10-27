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

package org.apache.asterix.metadata.entities;

import java.util.Map;

import org.apache.asterix.common.functions.FunctionSignature;
import org.apache.asterix.external.feed.api.IFeed;
import org.apache.asterix.external.feed.management.FeedId;
import org.apache.asterix.metadata.MetadataCache;
import org.apache.asterix.metadata.api.IMetadataEntity;

/**
 * Feed POJO
 */
public class Feed implements IMetadataEntity<Feed>, IFeed {
    private static final long serialVersionUID = 1L;

    /** A unique identifier for the feed */
    private FeedId feedId;
    /** The function that is to be applied on each incoming feed tuple **/
    private FunctionSignature appliedFunction;
    /** The type {@code FeedType} associated with the feed. **/
    private IFeed.FeedType feedType;
    /** A string representation of the instance **/
    private String displayName;
    /** A string representation of the adapter name **/
    private String adapterName;
    /** Adapter configuration */
    private Map<String, String> adapterConfiguration;
    /** Source primary feed */
    private String sourceFeedName;

    public Feed(String dataverseName, String feedName, FunctionSignature appliedFunction, IFeed.FeedType feedType,
            String sourceFeedName, String adapterName, Map<String, String> configuration) {
        this.feedId = new FeedId(dataverseName, feedName);
        this.appliedFunction = appliedFunction;
        this.feedType = feedType;
        this.displayName = feedType + "(" + feedId + ")";
        this.adapterName = adapterName;
        this.adapterConfiguration = configuration;
        this.sourceFeedName = sourceFeedName;
    }

    @Override
    public FeedId getFeedId() {
        return feedId;
    }

    @Override
    public String getDataverseName() {
        return feedId.getDataverse();
    }

    @Override
    public String getFeedName() {
        return feedId.getFeedName();
    }

    @Override
    public FunctionSignature getAppliedFunction() {
        return appliedFunction;
    }

    @Override
    public IFeed.FeedType getFeedType() {
        return feedType;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Feed)) {
            return false;
        }
        Feed otherFeed = (Feed) other;
        return otherFeed.getFeedId().equals(feedId);
    }

    @Override
    public int hashCode() {
        return displayName.hashCode();
    }

    @Override
    public String toString() {
        return feedType + "(" + feedId + ")";
    }

    @Override
    public Feed addToCache(MetadataCache cache) {
        return cache.addFeedIfNotExists(this);
    }

    @Override
    public Feed dropFromCache(MetadataCache cache) {
        return cache.dropFeed(this);
    }

    @Override
    public String getAdapterName() {
        return adapterName;
    }

    @Override
    public Map<String, String> getAdapterConfiguration() {
        return adapterConfiguration;
    }

    public String getSourceFeedName() {
        return sourceFeedName;
    }
}
