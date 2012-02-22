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

import static de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase.INCLUDE_PREFIX;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.factory.CollectionReaderFactory.createCollectionReader;

import java.io.IOException;

import org.annolab.tt4j.TreeTaggerWrapper;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.junit.Test;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.pipeline.SimplePipeline;

import de.tudarmstadt.ukp.dkpro.core.io.jwpl.WikipediaRevisionPairReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.tokit.TokenFilter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosLemmaTT4J;

public class VisualizeChanges
{

    public static final String LANGUAGE_CODE = "de";
//  public static final String LANGUAGE_CODE = "en";

    @Test
    public void visualizeChanges(String[] args) throws UIMAException, IOException
    {
      
          CollectionReader reader = null;
          if (LANGUAGE_CODE.equals("de")) {
//            reader = createCollectionReader(
//                    WikipediaRevisionPairReader.class,
//                    WikipediaReaderBase.PARAM_HOST,       "",
//                    WikipediaReaderBase.PARAM_DB,         "",
//                    WikipediaReaderBase.PARAM_USER,       "",
//                    WikipediaReaderBase.PARAM_PASSWORD,   "",
//                    WikipediaReaderBase.PARAM_LANGUAGE,   Language.german.name()
//            );
            reader = createCollectionReader(
                    XmiReader.class,
                    XmiReader.PARAM_PATH, "classpath:/wikirevision_data/en/",
                    XmiReader.PARAM_PATTERNS, new String[] { INCLUDE_PREFIX + "*.xmi.gz" }
            );
        }
        else if (LANGUAGE_CODE.equals("en")) {
//            reader = createCollectionReader(
//                    WikipediaRevisionPairReader.class,
//                    WikipediaReaderBase.PARAM_HOST,       "",
//                    WikipediaReaderBase.PARAM_DB,         "",
//                    WikipediaRevisionPairReader.PARAM_MAX_CHANGE, 20,
//                    WikipediaRevisionPairReader.PARAM_SKIP_FIRST_N_PAIRS, skipFirstNRevisions,
//                    WikipediaReaderBase.PARAM_USER,       "",
//                    WikipediaReaderBase.PARAM_PASSWORD,   "",
//                    WikipediaReaderBase.PARAM_LANGUAGE,   Language.english.name()
//            );
            reader = createCollectionReader(
                    XmiReader.class,
                    XmiReader.PARAM_PATH, "classpath:/wikirevision_data/en/",
                    XmiReader.PARAM_PATTERNS, new String[] { INCLUDE_PREFIX + "*.xmi.gz" }
            );
        }
          
          AnalysisEngineDescription segmenter = createPrimitiveDescription(
                  XmiWriter.class,
                  XmiWriter.PARAM_PATH, "target/xmi",
                  XmiWriter.PARAM_COMPRESS, true
          );

          AnalysisEngineDescription tokenFilter = createPrimitiveDescription(
                  TokenFilter.class,
                  TokenFilter.PARAM_MAX_TOKEN_LENGTH, 30
          );

          AnalysisEngineDescription sentenceFilter = createPrimitiveDescription(
                  SentenceFilter.class
          );

          TreeTaggerWrapper.TRACE = false;
          AnalysisEngineDescription tagger = createPrimitiveDescription(
                  TreeTaggerPosLemmaTT4J.class,
                  TreeTaggerPosLemmaTT4J.PARAM_LANGUAGE_CODE, LANGUAGE_CODE
          );

          AnalysisEngineDescription analyzer = createPrimitiveDescription(
                  SentenceAligner.class,
                  SentenceAligner.PARAM_THRESHOLD, 0.9f
          );
          
          AnalysisEngineDescription filter = createPrimitiveDescription(
                  ChangeVisualizer.class,
                  ChangeVisualizer.PARAM_MIN_CHANGED_WORDS, 1,
                  ChangeVisualizer.PARAM_MAX_CHANGED_WORDS, 2
          );

          AggregateBuilder builder = new AggregateBuilder();
          builder.add(segmenter, "_InitialView", WikipediaRevisionPairReader.REVISION_1);
          builder.add(segmenter, "_InitialView", WikipediaRevisionPairReader.REVISION_2);
          builder.add(tokenFilter, "_InitialView", WikipediaRevisionPairReader.REVISION_1);
          builder.add(tokenFilter, "_InitialView", WikipediaRevisionPairReader.REVISION_2);
          builder.add(sentenceFilter, "_InitialView", WikipediaRevisionPairReader.REVISION_1);
          builder.add(sentenceFilter, "_InitialView", WikipediaRevisionPairReader.REVISION_2);
          builder.add(tagger, "_InitialView", WikipediaRevisionPairReader.REVISION_1);
          builder.add(tagger, "_InitialView", WikipediaRevisionPairReader.REVISION_2);
          AnalysisEngineDescription aggr = builder.createAggregateDescription();

          SimplePipeline.runPipeline(
                  reader,
                  aggr,
                  analyzer,
                  filter
          );
    }
}
