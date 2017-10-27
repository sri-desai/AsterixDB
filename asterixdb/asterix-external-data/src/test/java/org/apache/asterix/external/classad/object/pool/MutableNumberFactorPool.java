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
package org.apache.asterix.external.classad.object.pool;

import org.apache.asterix.external.classad.AMutableNumberFactor;
import org.apache.asterix.external.classad.Value.NumberFactor;

public class MutableNumberFactorPool extends Pool<AMutableNumberFactor> {

    @Override
    public AMutableNumberFactor newInstance() {
        return new AMutableNumberFactor();
    }

    @Override
    protected void reset(AMutableNumberFactor obj) {
        obj.setFactor(NumberFactor.NO_FACTOR);
    }

}
