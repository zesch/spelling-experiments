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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.NN;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public abstract class SpellingPipeline_Base
{

    public static final String CANDIDATE_TOKEN = Token.class.getName();
    public static final String CANDIDATE_NOUN = NN.class.getName();

    public static final int MIN_LENGTH = 2;

    public static Object[][] datasetMap2datasetArray(Map<String,String> datasetMap) {
        Object[][] datasetArray = new Object[datasetMap.size()][];
        int i=0;
        for (Map.Entry<String, String> entry : datasetMap.entrySet()) {
            datasetArray[i] = new Object[]{
                    "datasetPath", entry.getKey(),
                    "languageCode", entry.getValue()
            };
            i++;
        }

        return datasetArray;
    }

    public static Object[][] datasetMap2datasetArray(Map<String,String> datasetMap, String languageCode) {
        List<Object[]> tmpDatasets = new ArrayList<Object[]>();
        for (Map.Entry<String, String> entry : datasetMap.entrySet()) {
            if (entry.getValue().equals(languageCode)) {
                tmpDatasets.add(new Object[]{
                        "datasetPath", entry.getKey(),
                        "languageCode", entry.getValue()
                });
            }
        }
        
        Object[][] datasetArray = new Object[tmpDatasets.size()][];
        for (int i=0; i<tmpDatasets.size(); i++) {
            datasetArray[i] = tmpDatasets.get(i);
        }
        
        return datasetArray;
    }
    
    @SuppressWarnings("serial")
    protected static final Map<String,String> blacklistMap = new HashMap<String,String>() {{
        put("en", "classpath:/blacklists/english_blacklist.txt");
        put("de", "classpath:/blacklists/german_blacklist.txt");
    }};

    @SuppressWarnings("serial")
    protected static final Map<String,String> vocabularyMap = new HashMap<String,String>() {{
        put("en", "classpath:/vocabulary/en_US_dict.txt");
        put("de", "classpath:/vocabulary/de_dict.txt");
    }};
}
