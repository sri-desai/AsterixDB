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
package org.apache.asterix.lang.common.expression;

import org.apache.asterix.common.exceptions.AsterixException;
import org.apache.asterix.lang.common.base.Expression;
import org.apache.asterix.lang.common.visitor.base.ILangVisitor;
import org.apache.commons.lang3.ObjectUtils;

public class IfExpr implements Expression {
    private Expression condExpr;
    private Expression thenExpr;
    private Expression elseExpr;

    public IfExpr() {
        // default constructor
    }

    public IfExpr(Expression condExpr, Expression thenExpr, Expression elseExpr) {
        this.condExpr = condExpr;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }

    public Expression getCondExpr() {
        return condExpr;
    }

    public void setCondExpr(Expression condExpr) {
        this.condExpr = condExpr;
    }

    public Expression getThenExpr() {
        return thenExpr;
    }

    public void setThenExpr(Expression thenExpr) {
        this.thenExpr = thenExpr;
    }

    public Expression getElseExpr() {
        return elseExpr;
    }

    public void setElseExpr(Expression elseExpr) {
        this.elseExpr = elseExpr;
    }

    @Override
    public Kind getKind() {
        return Kind.IF_EXPRESSION;
    }

    @Override
    public <R, T> R accept(ILangVisitor<R, T> visitor, T arg) throws AsterixException {
        return visitor.visit(this, arg);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hashCodeMulti(condExpr, elseExpr, thenExpr);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof IfExpr)) {
            return false;
        }
        IfExpr target = (IfExpr) object;
        return ObjectUtils.equals(condExpr, target.condExpr) && ObjectUtils.equals(elseExpr, target.elseExpr)
                && ObjectUtils.equals(thenExpr, target.thenExpr);
    }

    @Override
    public String toString() {
        return "if(" + condExpr + ") then " + thenExpr + " else " + elseExpr;
    }
}
