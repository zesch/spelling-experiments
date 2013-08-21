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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.data;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;

import de.tudarmstadt.ukp.dkpro.core.toolbox.corpus.BrownTeiCorpus;
import de.tudarmstadt.ukp.dkpro.core.toolbox.corpus.Corpus;

/**
 * Creates a vocubalary file from a toolbox corpus.
 * 
 * @author zesch
 *
 */
public class CreateVocabularyFromCorpus
{
    
    public static void main(String[] args) throws Exception
    {
        Corpus brown = new BrownTeiCorpus();
//        Corpus tiger = new TigerCorpus();
     
        createVocabulary(brown);
//        createVocabulary(tiger);
    }

    private static void createVocabulary(Corpus corpus) throws Exception {
        Set<String> vocabulary = new TreeSet<String>();
        for (String token : corpus.getTokens()) {
            vocabulary.add(token);
        }
        
        StringBuilder sb = new StringBuilder();
        for (String item : vocabulary) {
            sb.append(item); sb.append(System.getProperty("line.separator"));
        }

        FileUtils.writeStringToFile(
                new File("target/" + corpus.getName() + ".txt"),
                sb.toString()
        );
    }
}
