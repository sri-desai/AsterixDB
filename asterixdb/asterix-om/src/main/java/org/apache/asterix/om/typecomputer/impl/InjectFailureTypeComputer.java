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
package org.apache.asterix.om.typecomputer.impl;

import org.apache.asterix.om.typecomputer.base.IResultTypeComputer;
import org.apache.asterix.om.types.ATypeTag;
import org.apache.asterix.om.types.AUnionType;
import org.apache.asterix.om.types.IAType;
import org.apache.asterix.om.util.NonTaggedFormatUtil;
import org.apache.hyracks.algebricks.common.exceptions.AlgebricksException;
import org.apache.hyracks.algebricks.core.algebra.base.ILogicalExpression;
import org.apache.hyracks.algebricks.core.algebra.expressions.AbstractFunctionCallExpression;
import org.apache.hyracks.algebricks.core.algebra.expressions.IVariableTypeEnvironment;
import org.apache.hyracks.algebricks.core.algebra.metadata.IMetadataProvider;

public class InjectFailureTypeComputer implements IResultTypeComputer {

    private static final String errMsg1 = "inject-failure should have at least 2 parameters ";
    private static final String errMsg2 = "failure condition expression should have the return type Boolean";

    public static IResultTypeComputer INSTANCE = new InjectFailureTypeComputer();

    @Override
    public IAType computeType(ILogicalExpression expression, IVariableTypeEnvironment env,
            IMetadataProvider<?, ?> metadataProvider) throws AlgebricksException {
        AbstractFunctionCallExpression fce = (AbstractFunctionCallExpression) expression;
        if (fce.getArguments().size() < 2)
            throw new AlgebricksException(errMsg1);

        IAType t0 = (IAType) env.getType(fce.getArguments().get(0).getValue());
        IAType t1 = (IAType) env.getType(fce.getArguments().get(0).getValue());
        ATypeTag tag1 = t1.getTypeTag();
        if (NonTaggedFormatUtil.isOptional(t1))
            tag1 = ((AUnionType) t1).getActualType().getTypeTag();

        if (tag1 != ATypeTag.BOOLEAN)
            throw new AlgebricksException(errMsg2);

        return t0;
    }
}
