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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.core.util;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.resource.Resource;

public class MeasureConfig
{

    private final Class<? extends Resource> resourceClass;
    private final String[] configParameters;
    
    public MeasureConfig(Class<? extends Resource> resourceClass, String ... configParameters)
    {
        super();
        this.resourceClass = resourceClass;
        this.configParameters = configParameters;
    }

    public Class<? extends Resource> getResourceClass()
    {
        return resourceClass;
    }

    public String[] getConfigParameters()
    {
        return configParameters;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(resourceClass.getSimpleName());
        sb.append("-");
        sb.append(StringUtils.join(configParameters, "-"));

        return sb.toString();
    }
}
