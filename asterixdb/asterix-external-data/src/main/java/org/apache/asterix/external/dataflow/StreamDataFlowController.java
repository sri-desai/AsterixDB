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
package org.apache.asterix.external.dataflow;

import org.apache.asterix.external.api.IStreamDataParser;
import org.apache.asterix.external.api.ITupleForwarder;
import org.apache.hyracks.api.comm.IFrameWriter;
import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.dataflow.common.comm.io.ArrayTupleBuilder;

public class StreamDataFlowController extends AbstractDataFlowController {
    private final IStreamDataParser dataParser;

    public StreamDataFlowController(IHyracksTaskContext ctx, ITupleForwarder tupleForwarder,
            IStreamDataParser dataParser) {
        super(ctx, tupleForwarder);
        this.dataParser = dataParser;
    }

    @Override
    public void start(IFrameWriter writer) throws HyracksDataException {
        try {
            ArrayTupleBuilder tb = new ArrayTupleBuilder(1);
            tupleForwarder.initialize(ctx, writer);
            while (true) {
                tb.reset();
                if (!dataParser.parse(tb.getDataOutput())) {
                    break;
                }
                tb.addFieldEndOffset();
                tupleForwarder.addTuple(tb);
            }
            tupleForwarder.close();
        } catch (Exception e) {
            throw new HyracksDataException(e);
        }
    }
}
