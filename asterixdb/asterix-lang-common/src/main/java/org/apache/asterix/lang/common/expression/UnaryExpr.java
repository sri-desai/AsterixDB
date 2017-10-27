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

import java.util.Optional;

import org.apache.asterix.common.exceptions.AsterixException;
import org.apache.asterix.lang.common.base.Expression;
import org.apache.asterix.lang.common.struct.UnaryExprType;
import org.apache.asterix.lang.common.visitor.base.ILangVisitor;
import org.apache.commons.lang3.ObjectUtils;

public class UnaryExpr implements Expression {
    private UnaryExprType unaryExprType;
    private Expression expr;

    public UnaryExpr() {
        // default constructor
    }

    public UnaryExpr(UnaryExprType type, Expression expr) {
        this.unaryExprType = type;
        this.expr = expr;
    }

    public UnaryExprType getExprType() {
        return unaryExprType;
    }

    public void setExprType(String strType) throws AsterixException {
        Optional<UnaryExprType> exprType = UnaryExprType.fromSymbol(strType);
        if (exprType.isPresent()) {
            this.unaryExprType = exprType.get();
        } else {
            throw new AsterixException("Unsupported operator: " + strType);
        }
    }

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public Kind getKind() {
        return Kind.UNARY_EXPRESSION;
    }

    @Override
    public <R, T> R accept(ILangVisitor<R, T> visitor, T arg) throws AsterixException {
        return visitor.visit(this, arg);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hashCodeMulti(expr, unaryExprType);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UnaryExpr)) {
            return false;
        }
        UnaryExpr target = (UnaryExpr) object;
        return ObjectUtils.equals(expr, target.expr) && ObjectUtils.equals(unaryExprType, target.unaryExprType);
    }
}
