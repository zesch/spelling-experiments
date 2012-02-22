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

public class DatasetItem
{
    
    private static final String LF = "\n";

    private String correct;
    private String wrong;
    private int offset;
    private String context;
    private int pageId;
    private int revisionId;
    
    public DatasetItem(String correct, String wrong, int offset, String context)
    {
        this(correct, wrong, offset, context, 0, 0);
    }

    public DatasetItem(String wrong, String correct, int offset, String context, int pageId, int revisionId)
    {
        super();
        this.correct = correct;
        this.wrong = wrong;
        this.offset = offset;
        this.context = context;
        this.pageId = pageId;
        this.revisionId = revisionId;
    }

    public String getCorrect()
    {
        return correct;
    }

    public void setCorrect(String correct)
    {
        this.correct = correct;
    }

    public String getWrong()
    {
        return wrong;
    }

    public void setWrong(String wrong)
    {
        this.wrong = wrong;
    }

    public int getOffset()
    {
        return offset;
    }

    public void setOffset(int offset)
    {
        this.offset = offset;
    }

    public String getContext()
    {
        return context;
    }

    public void setContext(String context)
    {
        this.context = context;
    }

    public int getPageId()
    {
        return pageId;
    }

    public void setPageId(int pageId)
    {
        this.pageId = pageId;
    }

    public int getRevisionId()
    {
        return revisionId;
    }

    public void setRevisionId(int revisionId)
    {
        this.revisionId = revisionId;
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(pageId); 
        sb.append(LF); 
        sb.append(revisionId); 
        sb.append(LF); 
        sb.append(wrong); 
        sb.append(LF); 
        sb.append(correct); 
        sb.append(LF); 
        sb.append(offset); 
        sb.append(LF); 
        sb.append(context);
        sb.append(LF); 

        return sb.toString();
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((correct == null) ? 0 : correct.hashCode());
        result = prime * result + ((wrong == null) ? 0 : wrong.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DatasetItem other = (DatasetItem) obj;
        if (correct == null) {
            if (other.correct != null)
                return false;
        }
        else if (!correct.equals(other.correct))
            return false;
        if (wrong == null) {
            if (other.wrong != null)
                return false;
        }
        else if (!wrong.equals(other.wrong))
            return false;
        return true;
    }
}
