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
import static org.uimafit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.annolab.tt4j.TreeTaggerWrapper;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.resource.ExternalResourceDescription;
import org.junit.Test;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.pipeline.SimplePipeline;

import de.tudarmstadt.ukp.dkpro.core.api.resources.DKProContext;
import de.tudarmstadt.ukp.dkpro.core.frequency.resources.Web1TFrequencyCountResource;
import de.tudarmstadt.ukp.dkpro.core.io.jwpl.WikipediaRevisionPairReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiReader;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.tokit.TokenFilter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosLemmaTT4J;

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
    
    @Test
    public void getSpellingErrorsInContext() throws UIMAException, IOException
    {

        String outputPath = "target/test/";
//        int skipFirstNRevisions = 2000;
        
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
                    XmiReader.PARAM_PATH, "classpath:/wikirevision_data/de/",
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
        
        ExternalResourceDescription web1tResource = createExternalResourceDescription(
            Web1TFrequencyCountResource.class,
            Web1TFrequencyCountResource.PARAM_MIN_NGRAM_LEVEL, "1",
            Web1TFrequencyCountResource.PARAM_MAX_NGRAM_LEVEL, "1",
            Web1TFrequencyCountResource.PARAM_INDEX_PATH,
                new File(
                    DKProContext.getContext().getWorkspace("web1t"), LANGUAGE_CODE
                ).getAbsolutePath()
        );
        
        AnalysisEngineDescription segmenter = createPrimitiveDescription(
                BreakIteratorSegmenter.class
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
                SentenceAligner.PARAM_THRESHOLD, 0.95f,
                SentenceAligner.PARAM_MAX_DIFF, 3,
                SentenceAligner.PARAM_MIN_LENGTH, 3
        );
        
        AnalysisEngineDescription filter = createPrimitiveDescription(
                SpellingErrorFilter.class,
                SpellingErrorFilter.FREQUENCY_COUNT_RESOURCE, web1tResource,
                SpellingErrorFilter.PARAM_LANG, LANGUAGE_CODE,
                SpellingErrorFilter.PARAM_LOW_FREQ, lowFreqMap.get(LANGUAGE_CODE),
                SpellingErrorFilter.PARAM_LEVENSHTEIN_DISTANCE, 3,
                SpellingErrorFilter.PARAM_BLACKLIST, blacklistMap.get(LANGUAGE_CODE),
                SpellingErrorFilter.PARAM_TARGET_LOCATION, outputPath
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
        builder.add(analyzer);
        builder.add(filter);
        
        AnalysisEngineDescription aggr = builder.createAggregateDescription();
        
        SimplePipeline.runPipeline(
                reader,
                aggr
        );
    }
}
