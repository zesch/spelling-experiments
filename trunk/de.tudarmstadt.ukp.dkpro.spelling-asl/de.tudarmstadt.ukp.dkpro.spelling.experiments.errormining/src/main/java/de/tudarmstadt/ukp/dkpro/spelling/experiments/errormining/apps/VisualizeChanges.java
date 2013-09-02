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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.apps;

import static de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase.INCLUDE_PREFIX;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.pipeline.SimplePipeline;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.gate.GateLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.io.bincas.BinaryCasReader;
import de.tudarmstadt.ukp.dkpro.core.io.jwpl.WikipediaRevisionPairReader;
import de.tudarmstadt.ukp.dkpro.core.matetools.MateLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.AnnotationByLengthFilter;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.ChangeVisualizer;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.SentenceAligner;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.SentenceFilter;

public class VisualizeChanges
{

//    public static final String LANGUAGE_CODE = "de";
  public static final String LANGUAGE_CODE = "en";


    public static void main(String[] args)
        throws Exception
    {
      
          CollectionReaderDescription reader = null;
          if (LANGUAGE_CODE.equals("de")) {
//            reader = createReaderDescription(
//                    WikipediaRevisionPairReader.class,
//                    WikipediaReaderBase.PARAM_HOST,       "",
//                    WikipediaReaderBase.PARAM_DB,         "",
//                    WikipediaReaderBase.PARAM_USER,       "",
//                    WikipediaReaderBase.PARAM_PASSWORD,   "",
//                    WikipediaReaderBase.PARAM_LANGUAGE,   Language.german.name()
//            );
            reader = createReaderDescription(
                    BinaryCasReader.class,
                    BinaryCasReader.PARAM_SOURCE_LOCATION, "classpath*:/wikirevision_data/de/",
                    BinaryCasReader.PARAM_PATTERNS, INCLUDE_PREFIX + "*.bin"
            );
        }
        else if (LANGUAGE_CODE.equals("en")) {
//            reader = createReaderDescription(
//                    WikipediaRevisionPairReader.class,
//                    WikipediaReaderBase.PARAM_HOST,       "",
//                    WikipediaReaderBase.PARAM_DB,         "",
//                    WikipediaRevisionPairReader.PARAM_MAX_CHANGE, 20,
//                    WikipediaRevisionPairReader.PARAM_SKIP_FIRST_N_PAIRS, skipFirstNRevisions,
//                    WikipediaReaderBase.PARAM_USER,       "",
//                    WikipediaReaderBase.PARAM_PASSWORD,   "",
//                    WikipediaReaderBase.PARAM_LANGUAGE,   Language.english.name()
//            );
            reader = createReaderDescription(
                    BinaryCasReader.class,
                    BinaryCasReader.PARAM_SOURCE_LOCATION, "classpath*:/wikirevision_data/en/",
                    BinaryCasReader.PARAM_PATTERNS, INCLUDE_PREFIX + "*.bin"
            );
        }
          
          AnalysisEngineDescription segmenter = createEngineDescription(
                  BreakIteratorSegmenter.class
          );

          AnalysisEngineDescription tokenFilter = createEngineDescription(
                  AnnotationByLengthFilter.class,
                  AnnotationByLengthFilter.PARAM_FILTER_ANNOTATION_TYPES, Token.class.getName(),
                  AnnotationByLengthFilter.PARAM_MAX_LENGTH, 30
          );

          AnalysisEngineDescription sentenceFilter = createEngineDescription(
                  SentenceFilter.class
          );

          AnalysisEngineDescription tagger = createEngineDescription(
                  OpenNlpPosTagger.class,
                  OpenNlpPosTagger.PARAM_LANGUAGE, LANGUAGE_CODE
          );
          
          AnalysisEngineDescription lemmatizerEn = createEngineDescription(
                  GateLemmatizer.class
          );

          AnalysisEngineDescription lemmatizerDe = createEngineDescription(
                  MateLemmatizer.class,
                  MateLemmatizer.PARAM_LANGUAGE, LANGUAGE_CODE
          );

          AnalysisEngineDescription analyzer = createEngineDescription(
                  SentenceAligner.class,
                  SentenceAligner.PARAM_THRESHOLD, 0.9f
          );
          
          AnalysisEngineDescription filter = createEngineDescription(
                  ChangeVisualizer.class,
                  ChangeVisualizer.PARAM_MIN_CHANGED_WORDS, 1,
                  ChangeVisualizer.PARAM_MAX_CHANGED_WORDS, 2
          );

          AggregateBuilder builderEn = new AggregateBuilder();
          builderEn.add(segmenter, "_InitialView", WikipediaRevisionPairReader.REVISION_1);
          builderEn.add(segmenter, "_InitialView", WikipediaRevisionPairReader.REVISION_2);
          builderEn.add(tokenFilter, "_InitialView", WikipediaRevisionPairReader.REVISION_1);
          builderEn.add(tokenFilter, "_InitialView", WikipediaRevisionPairReader.REVISION_2);
          builderEn.add(sentenceFilter, "_InitialView", WikipediaRevisionPairReader.REVISION_1);
          builderEn.add(sentenceFilter, "_InitialView", WikipediaRevisionPairReader.REVISION_2);
          builderEn.add(tagger, "_InitialView", WikipediaRevisionPairReader.REVISION_1);
          builderEn.add(tagger, "_InitialView", WikipediaRevisionPairReader.REVISION_2);
          builderEn.add(lemmatizerEn, "_InitialView", WikipediaRevisionPairReader.REVISION_1);
          builderEn.add(lemmatizerEn, "_InitialView", WikipediaRevisionPairReader.REVISION_2);
          AnalysisEngineDescription aggrEn = builderEn.createAggregateDescription();

          AggregateBuilder builderDe = new AggregateBuilder();
          builderDe.add(segmenter, "_InitialView", WikipediaRevisionPairReader.REVISION_1);
          builderDe.add(segmenter, "_InitialView", WikipediaRevisionPairReader.REVISION_2);
          builderDe.add(tokenFilter, "_InitialView", WikipediaRevisionPairReader.REVISION_1);
          builderDe.add(tokenFilter, "_InitialView", WikipediaRevisionPairReader.REVISION_2);
          builderDe.add(sentenceFilter, "_InitialView", WikipediaRevisionPairReader.REVISION_1);
          builderDe.add(sentenceFilter, "_InitialView", WikipediaRevisionPairReader.REVISION_2);
          builderDe.add(tagger, "_InitialView", WikipediaRevisionPairReader.REVISION_1);
          builderDe.add(tagger, "_InitialView", WikipediaRevisionPairReader.REVISION_2);
          builderDe.add(lemmatizerDe, "_InitialView", WikipediaRevisionPairReader.REVISION_1);
          builderDe.add(lemmatizerDe, "_InitialView", WikipediaRevisionPairReader.REVISION_2);
          AnalysisEngineDescription aggrDe = builderDe.createAggregateDescription();

          if (LANGUAGE_CODE.equals("en")) {
              SimplePipeline.runPipeline(
                      reader,
                      aggrEn,
                      analyzer,
                      filter
              );
          }
          else {
              SimplePipeline.runPipeline(
                      reader,
                      aggrDe,
                      analyzer,
                      filter
              ); 
          }
    }
}