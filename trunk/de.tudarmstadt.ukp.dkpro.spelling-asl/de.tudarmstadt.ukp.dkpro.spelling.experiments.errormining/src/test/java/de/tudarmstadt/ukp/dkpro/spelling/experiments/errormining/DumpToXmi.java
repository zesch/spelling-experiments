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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining;

import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.factory.CollectionReaderFactory.createCollectionReader;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.junit.Test;
import org.uimafit.pipeline.SimplePipeline;

import de.tudarmstadt.ukp.dkpro.core.io.jwpl.WikipediaReaderBase;
import de.tudarmstadt.ukp.dkpro.core.io.jwpl.WikipediaRevisionPairReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;

public class DumpToXmi
{

    @Test
    public void dumpToXMI() throws Exception
    {
        dumpDe();
        dumpEn();
    }
    
    private void dumpDe() throws Exception {
        CollectionReader readerDe = createCollectionReader(
                WikipediaRevisionPairReader.class,
                WikipediaReaderBase.PARAM_HOST,       "bender.ukp.informatik.tu-darmstadt.de",
                WikipediaReaderBase.PARAM_DB,         "wiki_de_20100813_rev",
                WikipediaReaderBase.PARAM_USER,       "student",
                WikipediaReaderBase.PARAM_PASSWORD,   "student",
                WikipediaReaderBase.PARAM_LANGUAGE,   Language.german.name(),
                WikipediaRevisionPairReader.PARAM_MAX_CHANGE, 3
        );
        
        AnalysisEngineDescription dumperDe = createPrimitiveDescription(
                XmiWriter.class,
                XmiWriter.PARAM_PATH, "target/de/xmi",
                XmiWriter.PARAM_COMPRESS, true
        );

        SimplePipeline.runPipeline(
                readerDe,
                dumperDe
        );
    }

    private void dumpEn() throws Exception {
        CollectionReader readerEn = createCollectionReader(
                WikipediaRevisionPairReader.class,
                WikipediaReaderBase.PARAM_HOST,       "bender.ukp.informatik.tu-darmstadt.de",
                WikipediaReaderBase.PARAM_DB,         "wikiapi_simple_20090119",
                WikipediaReaderBase.PARAM_USER,       "student",
                WikipediaReaderBase.PARAM_PASSWORD,   "student",
                WikipediaReaderBase.PARAM_LANGUAGE,   Language.english.name(),
                WikipediaRevisionPairReader.PARAM_MAX_CHANGE, 3
        );
        
        AnalysisEngineDescription dumperEn = createPrimitiveDescription(
                XmiWriter.class,
                XmiWriter.PARAM_PATH, "target/en/xmi",
                XmiWriter.PARAM_COMPRESS, true
        );

        SimplePipeline.runPipeline(
                readerEn,
                dumperEn
        );
    }
}
