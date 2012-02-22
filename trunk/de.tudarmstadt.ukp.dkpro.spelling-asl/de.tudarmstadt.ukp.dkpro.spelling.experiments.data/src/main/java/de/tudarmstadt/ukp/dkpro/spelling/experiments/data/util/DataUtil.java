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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.data.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class DataUtil
{
    public static Map<String,String> getAllDatasets() throws IOException {
        return getAllDatasets("classpath*:/datasets/", new String[]{"txt"});
    }

    public static Map<String,String> getAllDatasets(String path, String[] extensions) throws IOException {
     
        Map<String,String> datasetMap = new HashMap<String,String>();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (Resource resource : resolver.getResources(path)) {
            for (File datasetFile : FileUtils.listFiles(resource.getFile(), extensions, true)) {
                datasetMap.put(datasetFile.getAbsolutePath(), datasetFile.getParentFile().getName());
            }
        }
        
        return datasetMap;
    }
}
