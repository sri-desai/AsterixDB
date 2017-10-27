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
package org.apache.asterix.external.feed.api;

import java.util.List;

import org.apache.asterix.external.feed.management.FeedConnectionId;
import org.apache.asterix.external.feed.management.FeedId;
import org.apache.asterix.external.feed.management.FeedJointKey;
import org.apache.hyracks.api.job.IJobLifecycleListener;

public interface IFeedLifecycleListener extends IJobLifecycleListener {
    public IFeedJoint getAvailableFeedJoint(FeedJointKey feedJoinKey);

    public boolean isFeedJointAvailable(FeedJointKey feedJoinKey);

    public List<FeedConnectionId> getActiveFeedConnections(FeedId feedId);

    public List<String> getComputeLocations(FeedId feedId);

    public List<String> getIntakeLocations(FeedId feedId);

    public List<String> getStoreLocations(FeedConnectionId feedId);

    public void registerFeedEventSubscriber(FeedConnectionId connectionId, IFeedLifecycleEventSubscriber subscriber);

    public void deregisterFeedEventSubscriber(FeedConnectionId connectionId, IFeedLifecycleEventSubscriber subscriber);

    public List<String> getCollectLocations(FeedConnectionId feedConnectionId);

    boolean isFeedConnectionActive(FeedConnectionId connectionId, IFeedLifecycleEventSubscriber eventSubscriber);

}
