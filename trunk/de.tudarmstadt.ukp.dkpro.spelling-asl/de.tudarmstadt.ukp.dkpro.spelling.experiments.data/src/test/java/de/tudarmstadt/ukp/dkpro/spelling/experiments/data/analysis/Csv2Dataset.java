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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.data.analysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;

public class Csv2Dataset
{

    @SuppressWarnings("serial")
    protected static final List<String> datasets = new ArrayList<String>() {{
        add("SOME_DATASET");
    }};
    
    public static void main(String[] args) throws Exception
    {
        for (String dataset : datasets) {
            CollectionReader reader = CollectionReaderFactory.createCollectionReader(
                    Csv2DatasetReader.class,
                    Csv2DatasetReader.PARAM_INPUT_FILE, dataset,
                    Csv2DatasetReader.PARAM_SEPARATOR, "@@"
            );
            
            SimplePipeline.runPipeline(
                    reader,
                    createPrimitiveDescription(NoOpAnnotator.class)
            );
        }
    } 
}
