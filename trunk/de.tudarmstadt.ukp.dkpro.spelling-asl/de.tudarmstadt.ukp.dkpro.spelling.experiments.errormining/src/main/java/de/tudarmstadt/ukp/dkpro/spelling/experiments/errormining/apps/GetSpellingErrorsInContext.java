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
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ExternalResourceDescription;

import de.tudarmstadt.ukp.dkpro.core.api.resources.DkproContext;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.frequency.resources.Web1TFrequencyCountResource;
import de.tudarmstadt.ukp.dkpro.core.gate.GateLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.io.bincas.BinaryCasReader;
import de.tudarmstadt.ukp.dkpro.core.io.jwpl.WikipediaRevisionPairReader;
import de.tudarmstadt.ukp.dkpro.core.matetools.MateLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.AnnotationByLengthFilter;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.SentenceAligner;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.SentenceFilter;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.SpellingErrorFilter;

@SuppressWarnings("serial")
public class GetSpellingErrorsInContext
{

//    public static final String LANGUAGE_CODE = "de";
    public static final String LANGUAGE_CODE = "en";
    
    public static final Map<String,Integer> lowFreqMap = new HashMap<String,Integer>() {{
        put("en", 25000);
        put("de", 10000);
    }};
    
    public static final Map<String,String> blacklistMap = new HashMap<String,String>() {{
        put("en", "classpath:/blacklists/english_blacklist.txt");
        put("de", "classpath:/blacklists/german_blacklist.txt");
    }};

    public static void main(String[] args)
        throws Exception
    {

        String outputPath = "target/test/";
//        int skipFirstNRevisions = 2000;
        
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
                    BinaryCasReader.PARAM_SOURCE_LOCATION, "classpath:/wikirevision_data/de/",
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
                    BinaryCasReader.PARAM_SOURCE_LOCATION, "classpath:/wikirevision_data/en/",
                    BinaryCasReader.PARAM_PATTERNS, INCLUDE_PREFIX + "*.bin"
            );
        }
        
        ExternalResourceDescription web1tResource = createExternalResourceDescription(
            Web1TFrequencyCountResource.class,
            Web1TFrequencyCountResource.PARAM_MIN_NGRAM_LEVEL, "1",
            Web1TFrequencyCountResource.PARAM_MAX_NGRAM_LEVEL, "1",
            Web1TFrequencyCountResource.PARAM_INDEX_PATH,
                new File(
                    DkproContext.getContext().getWorkspace("web1t"), LANGUAGE_CODE
                ).getAbsolutePath()
        );
        
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
                SentenceAligner.PARAM_THRESHOLD, 0.95f,
                SentenceAligner.PARAM_MAX_DIFF, 3,
                SentenceAligner.PARAM_MIN_LENGTH, 3
        );
        
        AnalysisEngineDescription filter = createEngineDescription(
                SpellingErrorFilter.class,
                SpellingErrorFilter.FREQUENCY_COUNT_RESOURCE, web1tResource,
                SpellingErrorFilter.PARAM_LANG, LANGUAGE_CODE,
                SpellingErrorFilter.PARAM_LOW_FREQ, lowFreqMap.get(LANGUAGE_CODE),
                SpellingErrorFilter.PARAM_LEVENSHTEIN_DISTANCE, 3,
                SpellingErrorFilter.PARAM_BLACKLIST, blacklistMap.get(LANGUAGE_CODE),
                SpellingErrorFilter.PARAM_TARGET_LOCATION, outputPath
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
        builderEn.add(analyzer);
        builderEn.add(filter);
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
        builderDe.add(analyzer);
        builderDe.add(filter);
        AnalysisEngineDescription aggrDe = builderDe.createAggregateDescription();

        if (LANGUAGE_CODE.equals("en")) {
            SimplePipeline.runPipeline(
                    reader,
                    aggrEn
            ); 
        }
        else {
            SimplePipeline.runPipeline(
                    reader,
                    aggrDe
            );  
        }
    }
}
