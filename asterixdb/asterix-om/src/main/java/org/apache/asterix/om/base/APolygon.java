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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.asterix.common.exceptions.AsterixException;
import org.apache.asterix.om.types.BuiltinType;
import org.apache.asterix.om.types.IAType;
import org.apache.asterix.om.visitors.IOMVisitor;

public class APolygon implements IAObject {

    protected APoint[] points;

    public APolygon(APoint[] points) {
        this.points = points;
    }

    public int getNumberOfPoints() {
        return points.length;
    }

    public APoint[] getPoints() {
        return points;
    }

    @Override
    public IAType getType() {
        return BuiltinType.APOLYGON;
    }

    @Override
    public void accept(IOMVisitor visitor) throws AsterixException {
        visitor.visitAPolygon(this);
    }

    @Override
    public boolean deepEqual(IAObject obj) {
        if (!(obj instanceof APolygon)) {
            return false;
        } else {
            APolygon p = (APolygon) obj;
            if (p.getPoints().length != points.length) {
                return false;
            }
            for (int i = 0; i < points.length; i++) {
                if (!points[i].deepEqual(p.getPoints()[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public int hash() {
        int h = 0;
        for (int i = 0; i < points.length; i++) {
            h += 31 * h + points[i].hash();
        }
        return h;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("APolygon: [ ");
        for (int i = 0; i < points.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(points[i].toString());
        }
        sb.append(" ]");
        return sb.toString();
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();

        JSONArray polygon = new JSONArray();
        for (int i = 0; i < points.length; i++) {
            polygon.put(points[i].toJSON());
        }
        json.put("APolygon", polygon);

        return json;
    }
}
