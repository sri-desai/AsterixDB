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
package org.apache.asterix.om.base;

import org.json.JSONException;
import org.json.JSONObject;

import org.apache.asterix.common.exceptions.AsterixException;
import org.apache.asterix.om.types.BuiltinType;
import org.apache.asterix.om.types.IAType;
import org.apache.asterix.om.visitors.IOMVisitor;

public class AInt32 implements IAObject {

    protected int value;

    public AInt32(int value) {
        super();
        this.value = value;
    }

    public AInt32(byte[] bytes, int offset, int length) {
        value = valueFromBytes(bytes, offset, length);
    }

    public Integer getIntegerValue() {
        return value;
    }

    @Override
    public IAType getType() {
        return BuiltinType.AINT32;
    }

    @Override
    public String toString() {
        return "AInt32: {" + value + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AInt32)) {
            return false;
        } else {
            return value == (((AInt32) obj).getIntegerValue());
        }
    }

    @Override
    public int hashCode() {
        return value;
    }

    private static Integer valueFromBytes(byte[] bytes, int offset, int length) {
        return ((bytes[offset] & 0xff) << 24) + ((bytes[offset + 1] & 0xff) << 16) + ((bytes[offset + 2] & 0xff) << 8)
                + ((bytes[offset + 3] & 0xff) << 0);
    }

    public byte[] toBytes() {
        return new byte[] { (byte) (value >>> 24), (byte) (value >> 16 & 0xff), (byte) (value >> 8 & 0xff),
                (byte) (value & 0xff) };
    }

    @Override
    public void accept(IOMVisitor visitor) throws AsterixException {
        visitor.visitAInt32(this);
    }

    @Override
    public boolean deepEqual(IAObject obj) {
        return equals(obj);
    }

    @Override
    public int hash() {
        return hashCode();
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("AInt32", value);

        return json;
    }
}
