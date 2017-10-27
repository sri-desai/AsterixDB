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
package org.apache.asterix.runtime.evaluators.common;

import java.io.DataOutput;
import java.io.IOException;

import org.apache.asterix.common.exceptions.AsterixException;
import org.apache.asterix.dataflow.data.nontagged.serde.AFloatSerializerDeserializer;
import org.apache.asterix.dataflow.data.nontagged.serde.AOrderedListSerializerDeserializer;
import org.apache.asterix.dataflow.data.nontagged.serde.AUnorderedListSerializerDeserializer;
import org.apache.asterix.formats.nontagged.AqlSerializerDeserializerProvider;
import org.apache.asterix.fuzzyjoin.IntArray;
import org.apache.asterix.fuzzyjoin.similarity.PartialIntersect;
import org.apache.asterix.fuzzyjoin.similarity.SimilarityFiltersJaccard;
import org.apache.asterix.fuzzyjoin.similarity.SimilarityMetric;
import org.apache.asterix.om.base.AFloat;
import org.apache.asterix.om.base.AMutableFloat;
import org.apache.asterix.om.types.ATypeTag;
import org.apache.asterix.om.types.BuiltinType;
import org.apache.asterix.om.types.EnumDeserializer;
import org.apache.asterix.om.types.hierachy.ATypeHierarchy;
import org.apache.hyracks.algebricks.common.exceptions.AlgebricksException;
import org.apache.hyracks.algebricks.runtime.base.IScalarEvaluator;
import org.apache.hyracks.algebricks.runtime.base.IScalarEvaluatorFactory;
import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.api.dataflow.value.ISerializerDeserializer;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.data.std.api.IPointable;
import org.apache.hyracks.data.std.primitive.VoidPointable;
import org.apache.hyracks.data.std.util.ArrayBackedValueStorage;
import org.apache.hyracks.dataflow.common.data.accessors.IFrameTupleReference;

public class SimilarityJaccardPrefixEvaluator implements IScalarEvaluator {
    // assuming type indicator in serde format
    protected final int typeIndicatorSize = 1;

    protected final ArrayBackedValueStorage resultStorage = new ArrayBackedValueStorage();
    protected final DataOutput out = resultStorage.getDataOutput();
    protected final IPointable inputVal = new VoidPointable();
    protected final IScalarEvaluator evalLen1;
    protected final IScalarEvaluator evalTokens1;
    protected final IScalarEvaluator evalLen2;
    protected final IScalarEvaluator evalTokens2;
    protected final IScalarEvaluator evalTokenPrefix;
    protected final IScalarEvaluator evalThreshold;

    protected float similarityThresholdCache;
    protected SimilarityFiltersJaccard similarityFilters;
    protected final IntArray tokens1 = new IntArray();
    protected final IntArray tokens2 = new IntArray();
    protected final PartialIntersect parInter = new PartialIntersect();

    protected float sim = 0.0f;

    // result
    protected final AMutableFloat res = new AMutableFloat(0);
    @SuppressWarnings("unchecked")
    protected final ISerializerDeserializer<AFloat> reusSerde = AqlSerializerDeserializerProvider.INSTANCE
            .getSerializerDeserializer(BuiltinType.AFLOAT);

    public SimilarityJaccardPrefixEvaluator(IScalarEvaluatorFactory[] args, IHyracksTaskContext context)
            throws AlgebricksException {
        evalLen1 = args[0].createScalarEvaluator(context);
        evalTokens1 = args[1].createScalarEvaluator(context);
        evalLen2 = args[2].createScalarEvaluator(context);
        evalTokens2 = args[3].createScalarEvaluator(context);
        evalTokenPrefix = args[4].createScalarEvaluator(context);
        evalThreshold = args[5].createScalarEvaluator(context);
    }

    @Override
    public void evaluate(IFrameTupleReference tuple, IPointable result) throws AlgebricksException {
        resultStorage.reset();
        // similarity threshold
        sim = 0;
        evalThreshold.evaluate(tuple, inputVal);
        float similarityThreshold = AFloatSerializerDeserializer.getFloat(inputVal.getByteArray(),
                inputVal.getStartOffset() + 1);

        if (similarityThreshold != similarityThresholdCache || similarityFilters == null) {
            similarityFilters = new SimilarityFiltersJaccard(similarityThreshold);
            similarityThresholdCache = similarityThreshold;
        }

        evalLen1.evaluate(tuple, inputVal);
        int length1 = 0;
        try {
            length1 = ATypeHierarchy.getIntegerValue(inputVal.getByteArray(), inputVal.getStartOffset());
        } catch (HyracksDataException e1) {
            throw new AlgebricksException(e1);
        }

        evalLen2.evaluate(tuple, inputVal);
        int length2 = 0;

        try {
            length2 = ATypeHierarchy.getIntegerValue(inputVal.getByteArray(), inputVal.getStartOffset());
        } catch (HyracksDataException e1) {
            throw new AlgebricksException(e1);
        }

        //
        // -- - length filter - --
        //
        if (similarityFilters.passLengthFilter(length1, length2)) {

            // -- - tokens1 - --
            int i;
            tokens1.reset();
            evalTokens1.evaluate(tuple, inputVal);

            byte[] serList = inputVal.getByteArray();
            int startOffset = inputVal.getStartOffset();
            if (serList[startOffset] != ATypeTag.SERIALIZED_ORDEREDLIST_TYPE_TAG
                    && serList[startOffset] != ATypeTag.SERIALIZED_UNORDEREDLIST_TYPE_TAG) {
                throw new AlgebricksException("Scan collection is not defined for values of type"
                        + EnumDeserializer.ATYPETAGDESERIALIZER.deserialize(serList[startOffset]));
            }

            int lengthTokens1;
            if (serList[startOffset] == ATypeTag.SERIALIZED_ORDEREDLIST_TYPE_TAG) {
                lengthTokens1 = AOrderedListSerializerDeserializer.getNumberOfItems(inputVal.getByteArray(),
                        startOffset);
                // read tokens
                for (i = 0; i < lengthTokens1; i++) {
                    int itemOffset;
                    int token;
                    try {
                        itemOffset = AOrderedListSerializerDeserializer.getItemOffset(serList, startOffset, i);
                    } catch (AsterixException e) {
                        throw new AlgebricksException(e);
                    }

                    try {
                        token = ATypeHierarchy.getIntegerValueWithDifferentTypeTagPosition(serList, itemOffset,
                                startOffset + 1);
                    } catch (HyracksDataException e) {
                        throw new AlgebricksException(e);
                    }
                    tokens1.add(token);
                }
            } else {
                lengthTokens1 = AUnorderedListSerializerDeserializer.getNumberOfItems(inputVal.getByteArray(),
                        startOffset);
                // read tokens
                for (i = 0; i < lengthTokens1; i++) {
                    int itemOffset;
                    int token;

                    try {
                        itemOffset = AUnorderedListSerializerDeserializer.getItemOffset(serList, startOffset, i);
                    } catch (AsterixException e) {
                        throw new AlgebricksException(e);
                    }

                    try {
                        token = ATypeHierarchy.getIntegerValueWithDifferentTypeTagPosition(serList, itemOffset,
                                startOffset + 1);
                    } catch (HyracksDataException e) {
                        throw new AlgebricksException(e);
                    }
                    tokens1.add(token);
                }
            }
            // pad tokens
            for (; i < length1; i++) {
                tokens1.add(Integer.MAX_VALUE);
            }

            // -- - tokens2 - --
            tokens2.reset();
            evalTokens2.evaluate(tuple, inputVal);

            serList = inputVal.getByteArray();
            startOffset = inputVal.getStartOffset();
            if (serList[startOffset] != ATypeTag.SERIALIZED_ORDEREDLIST_TYPE_TAG
                    && serList[startOffset] != ATypeTag.SERIALIZED_UNORDEREDLIST_TYPE_TAG) {
                throw new AlgebricksException("Scan collection is not defined for values of type"
                        + EnumDeserializer.ATYPETAGDESERIALIZER.deserialize(serList[startOffset]));
            }

            int lengthTokens2;
            if (serList[startOffset] == ATypeTag.SERIALIZED_ORDEREDLIST_TYPE_TAG) {
                lengthTokens2 = AOrderedListSerializerDeserializer.getNumberOfItems(inputVal.getByteArray(),
                        startOffset);
                // read tokens
                for (i = 0; i < lengthTokens2; i++) {
                    int itemOffset;
                    int token;

                    try {
                        itemOffset = AOrderedListSerializerDeserializer.getItemOffset(serList, startOffset, i);
                    } catch (AsterixException e) {
                        throw new AlgebricksException(e);
                    }

                    try {
                        token = ATypeHierarchy.getIntegerValueWithDifferentTypeTagPosition(serList, itemOffset,
                                startOffset + 1);
                    } catch (HyracksDataException e) {
                        throw new AlgebricksException(e);
                    }
                    tokens2.add(token);
                }
            } else {
                lengthTokens2 = AUnorderedListSerializerDeserializer.getNumberOfItems(inputVal.getByteArray(),
                        startOffset);
                // read tokens
                for (i = 0; i < lengthTokens2; i++) {
                    int itemOffset;
                    int token;

                    try {
                        itemOffset = AUnorderedListSerializerDeserializer.getItemOffset(serList, startOffset, i);
                    } catch (AsterixException e) {
                        throw new AlgebricksException(e);
                    }

                    try {
                        token = ATypeHierarchy.getIntegerValueWithDifferentTypeTagPosition(serList, itemOffset,
                                startOffset + 1);
                    } catch (HyracksDataException e) {
                        throw new AlgebricksException(e);
                    }
                    tokens2.add(token);
                }
            }
            // pad tokens
            for (; i < length2; i++) {
                tokens2.add(Integer.MAX_VALUE);
            }

            // -- - token prefix - --
            evalTokenPrefix.evaluate(tuple, inputVal);
            int tokenPrefix = 0;

            try {
                tokenPrefix = ATypeHierarchy.getIntegerValue(inputVal.getByteArray(), inputVal.getStartOffset());
            } catch (HyracksDataException e) {
                throw new AlgebricksException(e);
            }

            //
            // -- - position filter - --
            //
            SimilarityMetric.getPartialIntersectSize(tokens1.get(), 0, tokens1.length(), tokens2.get(), 0,
                    tokens2.length(), tokenPrefix, parInter);
            if (similarityFilters.passPositionFilter(parInter.intersectSize, parInter.posXStop, length1,
                    parInter.posYStop, length2)) {

                //
                // -- - suffix filter - --
                //
                if (similarityFilters.passSuffixFilter(tokens1.get(), 0, tokens1.length(), parInter.posXStart,
                        tokens2.get(), 0, tokens2.length(), parInter.posYStart)) {

                    sim = similarityFilters.passSimilarityFilter(tokens1.get(), 0, tokens1.length(),
                            parInter.posXStop + 1, tokens2.get(), 0, tokens2.length(), parInter.posYStop + 1,
                            parInter.intersectSize);
                }
            }
        }

        try {
            writeResult();
        } catch (IOException e) {
            throw new AlgebricksException(e);
        }
        result.set(resultStorage);
    }

    public void writeResult() throws AlgebricksException, IOException {
        res.setValue(sim);
        reusSerde.serialize(res, out);
    }
}
