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
package org.apache.asterix.common.transactions;

import java.nio.ByteBuffer;

import org.apache.asterix.common.replication.IReplicationThread;
import org.apache.hyracks.dataflow.common.data.accessors.ITupleReference;

public interface ILogRecord {

    enum RecordReadStatus {
        TRUNCATED,
        BAD_CHKSUM,
        OK,
        LARGE_RECORD
    }

    public static final int JOB_TERMINATE_LOG_SIZE = 14; //JOB_COMMIT or ABORT log type
    public static final int ENTITY_COMMIT_LOG_BASE_SIZE = 30;
    public static final int UPDATE_LOG_BASE_SIZE = 51;
    public static final int FLUSH_LOG_SIZE = 18;
    public static final int WAIT_LOG_SIZE = 14;

    public RecordReadStatus readLogRecord(ByteBuffer buffer);

    public void writeLogRecord(ByteBuffer buffer);

    public ITransactionContext getTxnCtx();

    public void setTxnCtx(ITransactionContext txnCtx);

    public boolean isFlushed();

    public void isFlushed(boolean isFlushed);

    public byte getLogType();

    public void setLogType(byte logType);

    public int getJobId();

    public void setJobId(int jobId);

    public int getDatasetId();

    public void setDatasetId(int datasetId);

    public int getPKHashValue();

    public void setPKHashValue(int PKHashValue);

    public long getResourceId();

    public void setResourceId(long resourceId);

    public int getLogSize();

    public void setLogSize(int logSize);

    public byte getNewOp();

    public void setNewOp(byte newOp);

    public void setNewValueSize(int newValueSize);

    public ITupleReference getNewValue();

    public void setNewValue(ITupleReference newValue);

    public long getChecksum();

    public void setChecksum(long checksum);

    public long getLSN();

    public void setLSN(long LSN);

    public String getLogRecordForDisplay();

    public void computeAndSetLogSize();

    public int getPKValueSize();

    public ITupleReference getPKValue();

    public void setPKFields(int[] primaryKeyFields);

    public void computeAndSetPKValueSize();

    public void setPKValue(ITupleReference PKValue);

    public String getNodeId();

    public void readRemoteLog(ByteBuffer buffer);

    public void setReplicationThread(IReplicationThread replicationThread);

    public void setLogSource(byte logSource);

    public byte getLogSource();

    public int getRemoteLogSize();

    public void setNodeId(String nodeId);

    public int getResourcePartition();

    public void setResourcePartition(int resourcePartition);

    public void setReplicated(boolean replicated);

    /**
     * @return a flag indicating whether the log record should be sent to remote replicas
     */
    public boolean isReplicated();

    public void writeRemoteLogRecord(ByteBuffer buffer);

    public ITupleReference getOldValue();

    public void setOldValue(ITupleReference oldValue);

    public void setOldValueSize(int oldValueSize);
}
