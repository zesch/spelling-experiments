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
/* First created by JCasGen Wed Feb 01 16:36:32 CET 2012 */
package de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** Holds two aligned sentences from two different Wikipedia revisions.
 * Updated by JCasGen Wed Feb 01 16:36:32 CET 2012
 * @generated */
public class RevisionSentencePair_Type extends Annotation_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (RevisionSentencePair_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = RevisionSentencePair_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new RevisionSentencePair(addr, RevisionSentencePair_Type.this);
  			   RevisionSentencePair_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new RevisionSentencePair(addr, RevisionSentencePair_Type.this);
  	  }
    };
  /** @generated */
  public final static int typeIndexID = RevisionSentencePair.typeIndexID;
  /** @generated 
     @modifiable */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.RevisionSentencePair");
 
  /** @generated */
  final Feature casFeat_Sentence1;
  /** @generated */
  final int     casFeatCode_Sentence1;
  /** @generated */ 
  public int getSentence1(int addr) {
        if (featOkTst && casFeat_Sentence1 == null)
      jcas.throwFeatMissing("Sentence1", "de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.RevisionSentencePair");
    return ll_cas.ll_getRefValue(addr, casFeatCode_Sentence1);
  }
  /** @generated */    
  public void setSentence1(int addr, int v) {
        if (featOkTst && casFeat_Sentence1 == null)
      jcas.throwFeatMissing("Sentence1", "de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.RevisionSentencePair");
    ll_cas.ll_setRefValue(addr, casFeatCode_Sentence1, v);}
    
  
 
  /** @generated */
  final Feature casFeat_Sentence2;
  /** @generated */
  final int     casFeatCode_Sentence2;
  /** @generated */ 
  public int getSentence2(int addr) {
        if (featOkTst && casFeat_Sentence2 == null)
      jcas.throwFeatMissing("Sentence2", "de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.RevisionSentencePair");
    return ll_cas.ll_getRefValue(addr, casFeatCode_Sentence2);
  }
  /** @generated */    
  public void setSentence2(int addr, int v) {
        if (featOkTst && casFeat_Sentence2 == null)
      jcas.throwFeatMissing("Sentence2", "de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.RevisionSentencePair");
    ll_cas.ll_setRefValue(addr, casFeatCode_Sentence2, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public RevisionSentencePair_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_Sentence1 = jcas.getRequiredFeatureDE(casType, "Sentence1", "uima.tcas.Annotation", featOkTst);
    casFeatCode_Sentence1  = (null == casFeat_Sentence1) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Sentence1).getCode();

 
    casFeat_Sentence2 = jcas.getRequiredFeatureDE(casType, "Sentence2", "uima.tcas.Annotation", featOkTst);
    casFeatCode_Sentence2  = (null == casFeat_Sentence2) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Sentence2).getCode();

  }
}



    
