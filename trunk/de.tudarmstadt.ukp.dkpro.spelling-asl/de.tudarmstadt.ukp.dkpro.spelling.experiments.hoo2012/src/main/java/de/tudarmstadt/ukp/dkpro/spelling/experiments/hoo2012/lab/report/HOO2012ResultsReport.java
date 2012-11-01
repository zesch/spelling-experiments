/*******************************************************************************
 * Copyright 2012
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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.lab.report;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;

import de.tudarmstadt.ukp.dkpro.lab.reporting.BatchReportBase;
import de.tudarmstadt.ukp.dkpro.lab.task.TaskContextMetadata;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.HOO2012Experiments;

public class HOO2012ResultsReport
    extends BatchReportBase
{
    @Override
    public void execute()
        throws Exception
    {
    	int i=0;
        for (TaskContextMetadata subcontext : getSubtasks()) {
            File folder = getContext().getStorageService().getStorageFolder(
            		subcontext.getId(),
            		HOO2012Experiments.OUTPUT_KEY
            );
            for (File resultFile : folder.listFiles()) {
            	if (resultFile.isFile()) {
            		InputStream is = FileUtils.openInputStream(resultFile);
                    storeBinary(resultFile.getName() + i + ".txt", is);
            	}
          	}
            
            i++;
        }
    }
}
