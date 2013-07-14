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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.artificialerrors;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createCollectionReader;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.pipeline.SimplePipeline;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.NN;
import de.tudarmstadt.ukp.dkpro.core.toolbox.corpus.BrownTEICorpus;
import de.tudarmstadt.ukp.dkpro.core.toolbox.corpus.CorpusReader;
import de.tudarmstadt.ukp.dkpro.core.toolbox.corpus.TigerCorpus;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosLemmaTT4J;

public class CreateArtificialEvaluationDataset
{

//    private static final String TYPE = Token.class.getName();
    private static final String TYPE = NN.class.getName();
    
    // Following (Mays, Damerau & Mercer, 1991) and (Wilcox-O'Hearn, Hirst & Budanitsky, 2008),
    // we create an evaluation dataset of artificial spelling errors.
    //
    // We start with a corpus that is considered to be error free.
    // Then an error is introduced by changing a word into another known word with low edit distance.
    // Parameters:
    //   - the list of known words (the vocabulary)
    //   - how many errors are introduced. (Wilcox-O'Hearn et al., 2008) used one error approximately every 200 words.
    //   - which kind of words can be target words for replacement.
    //     For example, any word could be a target word (Mays, Damerau & Mercer, 1991),
    //     or only words that can be found in WordNet (Wilcox-O'Hearn, Hirst & Budanitsky, 2008).
    
    public static void main(String[] args)
        throws Exception 
    {
        createEnglish(100000, "_100000");
        createEnglish(10000, "_10000");
        createEnglish(1000, "_1000");
        createGerman(100000, "_100000");
        createGerman(10000, "_10000");
        createGerman(1000, "_1000");
    }

    private static void createEnglish(int nrOfItems, String name)
        throws Exception 
    {
        CollectionReader reader = createCollectionReader(
                CorpusReader.class,
                CorpusReader.PARAM_CORPUS, BrownTEICorpus.class.getName()
        );
        
        AnalysisEngineDescription tagger = createPrimitiveDescription(
                TreeTaggerPosLemmaTT4J.class,
                TreeTaggerPosLemmaTT4J.PARAM_LANGUAGE, "en"
        );
        
        AnalysisEngineDescription errorAdder = createPrimitiveDescription(
                SpellingErrorAdder.class,
                SpellingErrorAdder.PARAM_VOCABULARY, "classpath:/vocabulary/brown_tei.txt",
                SpellingErrorAdder.PARAM_TARGET_ANNOTATION_TYPE, TYPE,
                SpellingErrorAdder.PARAM_MAX_ERRORS_PER_SENTENCE, 1,
                SpellingErrorAdder.PARAM_MAX_ITEMS, nrOfItems,
                SpellingErrorAdder.PARAM_MIN_SENTENCE_LENGTH, 10,
                SpellingErrorAdder.PARAM_MAX_EDIT_DISTANCE, 2
        );
        
        AnalysisEngineDescription writer = createPrimitiveDescription(
                EvaluationDatasetWriter.class,
                EvaluationDatasetWriter.PARAM_OUTPUT_FILE, "target/brown_artificial" + name + ".txt"
        );
        
        SimplePipeline.runPipeline(
                reader,
                tagger,
                errorAdder,
                writer
        );
    }

    private static void createGerman(int nrOfItems, String name)
        throws Exception 
    {
        CollectionReader reader = createCollectionReader(
                CorpusReader.class,
                CorpusReader.PARAM_CORPUS, TigerCorpus.class.getName()
        );
        
        AnalysisEngineDescription tagger = createPrimitiveDescription(
                TreeTaggerPosLemmaTT4J.class,
                TreeTaggerPosLemmaTT4J.PARAM_LANGUAGE, "de"
        );

        AnalysisEngineDescription errorAdder = createPrimitiveDescription(
                SpellingErrorAdder.class,
                SpellingErrorAdder.PARAM_VOCABULARY, "classpath:/vocabulary/tiger_export.txt",
                SpellingErrorAdder.PARAM_TARGET_ANNOTATION_TYPE, TYPE,
                SpellingErrorAdder.PARAM_MAX_ERRORS_PER_SENTENCE, 1,
                SpellingErrorAdder.PARAM_MAX_ITEMS, nrOfItems,
                SpellingErrorAdder.PARAM_MIN_SENTENCE_LENGTH, 10
        );

        AnalysisEngineDescription writer = createPrimitiveDescription(
                EvaluationDatasetWriter.class,
                EvaluationDatasetWriter.PARAM_OUTPUT_FILE, "target/tiger_artificial" + name + ".txt"
        );

        SimplePipeline.runPipeline(
                reader,
                tagger,
                errorAdder,
                writer
        );
    }
}
