/*******************************************************************************
 * Copyright 2010
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.spelling.experiments.data.statistics;

import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.junit.Ignore;
import org.junit.Test;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.pipeline.SimplePipeline;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.SpellingPipeline_Base;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.data.util.DataUtil;
import de.tudarmstadt.ukp.dkpro.spelling.io.SpellingErrorInContextReader;
import de.tudarmstadt.ukp.dkpro.spelling.io.SpellingErrorInContextReader_LongFormat;


public class ComputeDatasetStatistics
    extends SpellingPipeline_Base
{

    @Test
    public void computeStatistics() throws UIMAException, IOException
    {
        
        for (String dataset : DataUtil.getAllDatasets("classpath*:/datasets/", new String[]{"txt"}).keySet()) {
            System.out.println(dataset);
            
            SimplePipeline.runPipeline(
                    CollectionReaderFactory.createCollectionReader(
                            SpellingErrorInContextReader_LongFormat.class,
                            SpellingErrorInContextReader.PARAM_INPUT_FILE, dataset
                    ),
                    createPrimitiveDescription(
                            BreakIteratorSegmenter.class
                    ),
                    createPrimitiveDescription(
                            DatasetStatisticsCollector.class
                    )
            );
        }
    }

    @Ignore
    @Test
    public void computeStatisticsWithFd() throws UIMAException, IOException
    {
        
        for (String dataset : DataUtil.getAllDatasets("classpath*:/datasets/", new String[]{"txt"}).keySet()) {
            System.out.println(dataset);
            
            SimplePipeline.runPipeline(
                    CollectionReaderFactory.createCollectionReader(
                            SpellingErrorInContextReader_LongFormat.class,
                            SpellingErrorInContextReader.PARAM_INPUT_FILE, dataset
                    ),
                    createPrimitiveDescription(
                            BreakIteratorSegmenter.class
                    ),
                    createPrimitiveDescription(
                            DatasetStatisticsCollector.class,
                            DatasetStatisticsCollector.PARAM_INCLUDE_FD, true
                    )
            );
        }
    }
}
