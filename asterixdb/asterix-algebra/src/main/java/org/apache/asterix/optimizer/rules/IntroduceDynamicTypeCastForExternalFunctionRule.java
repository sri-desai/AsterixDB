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

package org.apache.asterix.optimizer.rules;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;

import org.apache.asterix.metadata.functions.AsterixExternalScalarFunctionInfo;
import org.apache.asterix.om.functions.AsterixBuiltinFunctions;
import org.apache.asterix.om.types.ARecordType;
import org.apache.asterix.om.types.AUnionType;
import org.apache.asterix.om.types.IAType;
import org.apache.asterix.om.util.NonTaggedFormatUtil;
import org.apache.hyracks.algebricks.common.exceptions.AlgebricksException;
import org.apache.hyracks.algebricks.core.algebra.base.ILogicalExpression;
import org.apache.hyracks.algebricks.core.algebra.base.ILogicalOperator;
import org.apache.hyracks.algebricks.core.algebra.base.IOptimizationContext;
import org.apache.hyracks.algebricks.core.algebra.base.LogicalExpressionTag;
import org.apache.hyracks.algebricks.core.algebra.base.LogicalOperatorTag;
import org.apache.hyracks.algebricks.core.algebra.base.LogicalVariable;
import org.apache.hyracks.algebricks.core.algebra.expressions.IVariableTypeEnvironment;
import org.apache.hyracks.algebricks.core.algebra.expressions.ScalarFunctionCallExpression;
import org.apache.hyracks.algebricks.core.algebra.functions.FunctionIdentifier;
import org.apache.hyracks.algebricks.core.algebra.operators.logical.AbstractLogicalOperator;
import org.apache.hyracks.algebricks.core.algebra.operators.logical.AssignOperator;
import org.apache.hyracks.algebricks.core.rewriter.base.IAlgebraicRewriteRule;

/**
 * This rule provides the same type-casting handling as the IntroduceDynamicTypeCastRule does.
 * The only difference is that this rule is intended for external functions (User-Defined Functions).
 * Refer to IntroduceDynamicTypeCastRule for the detail.
 */
public class IntroduceDynamicTypeCastForExternalFunctionRule implements IAlgebraicRewriteRule {

    @Override
    public boolean rewritePre(Mutable<ILogicalOperator> opRef, IOptimizationContext context) throws AlgebricksException {
        return false;
    }

    @Override
    public boolean rewritePost(Mutable<ILogicalOperator> opRef, IOptimizationContext context)
            throws AlgebricksException {
        /**
         * pattern match: distribute_result - project - assign (external function call) - assign (open_record_constructor)
         * resulting plan: distribute_result - project - assign (external function call) - assign (cast-record) - assign(open_record_constructor)
         */
        AbstractLogicalOperator op1 = (AbstractLogicalOperator) opRef.getValue();
        if (op1.getOperatorTag() != LogicalOperatorTag.DISTRIBUTE_RESULT)
            return false;
        AbstractLogicalOperator op2 = (AbstractLogicalOperator) op1.getInputs().get(0).getValue();
        if (op2.getOperatorTag() != LogicalOperatorTag.PROJECT)
            return false;
        AbstractLogicalOperator op3 = (AbstractLogicalOperator) op2.getInputs().get(0).getValue();
        if (op3.getOperatorTag() != LogicalOperatorTag.ASSIGN)
            return false;
        AbstractLogicalOperator op4 = (AbstractLogicalOperator) op3.getInputs().get(0).getValue();
        if (op4.getOperatorTag() != LogicalOperatorTag.ASSIGN)
            return false;

        // Op1 : assign (external function call), Op2 : assign (open_record_constructor)
        AssignOperator assignOp1 = (AssignOperator) op3;
        AssignOperator assignOp2 = (AssignOperator) op4;

        // Checks whether open-record-constructor is called to create a record in the first assign operator - assignOp2
        FunctionIdentifier fid = null;
        ILogicalExpression assignExpr = assignOp2.getExpressions().get(0).getValue();
        if (assignExpr.getExpressionTag() == LogicalExpressionTag.FUNCTION_CALL) {
            ScalarFunctionCallExpression funcExpr = (ScalarFunctionCallExpression) assignOp2.getExpressions().get(0)
                    .getValue();
            fid = funcExpr.getFunctionIdentifier();

            if (fid != AsterixBuiltinFunctions.OPEN_RECORD_CONSTRUCTOR) {
                return false;
            }
        } else {
            return false;
        }

        // Checks whether an external function is called in the second assign operator - assignOp1
        assignExpr = assignOp1.getExpressions().get(0).getValue();
        ScalarFunctionCallExpression funcExpr = null;
        if (assignExpr.getExpressionTag() == LogicalExpressionTag.FUNCTION_CALL) {
            funcExpr = (ScalarFunctionCallExpression) assignOp1.getExpressions().get(0).getValue();
            fid = funcExpr.getFunctionIdentifier();

            // Checks whether this is an internal function call. Then, we return false.
            if (AsterixBuiltinFunctions.getBuiltinFunctionIdentifier(fid) != null) {
                return false;
            }

        } else {
            return false;
        }

        AsterixExternalScalarFunctionInfo finfo = (AsterixExternalScalarFunctionInfo) funcExpr.getFunctionInfo();
        ARecordType requiredRecordType = (ARecordType) finfo.getArgumenTypes().get(0);

        List<LogicalVariable> recordVar = new ArrayList<LogicalVariable>();
        recordVar.addAll(assignOp2.getVariables());

        IVariableTypeEnvironment env = assignOp2.computeOutputTypeEnvironment(context);
        IAType inputRecordType = (IAType) env.getVarType(recordVar.get(0));

        /** the input record type can be an union type -- for the case when it comes from a subplan or left-outer join */
        boolean checkUnknown = false;
        while (NonTaggedFormatUtil.isOptional(inputRecordType)) {
            /** while-loop for the case there is a nested multi-level union */
            inputRecordType = ((AUnionType) inputRecordType).getActualType();
            checkUnknown = true;
        }

        /** see whether the input record type needs to be casted */
        boolean cast = !IntroduceDynamicTypeCastRule.compatible(requiredRecordType, inputRecordType);

        if (checkUnknown) {
            recordVar.set(0, IntroduceDynamicTypeCastRule.addWrapperFunction(requiredRecordType, recordVar.get(0),
                    assignOp1, context, AsterixBuiltinFunctions.CHECK_UNKNOWN));
        }
        if (cast) {
            IntroduceDynamicTypeCastRule.addWrapperFunction(requiredRecordType, recordVar.get(0), assignOp1, context,
                    AsterixBuiltinFunctions.CAST_RECORD);
        }
        return cast || checkUnknown;
    }

}
