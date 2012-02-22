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
/* First created by JCasGen Wed Feb 01 16:36:03 CET 2012 */
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

/** Marks a token that has been changed from one revision to another.
 * Updated by JCasGen Wed Feb 01 16:36:03 CET 2012
 * @generated */
public class ChangedToken_Type extends Annotation_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (ChangedToken_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = ChangedToken_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new ChangedToken(addr, ChangedToken_Type.this);
  			   ChangedToken_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new ChangedToken(addr, ChangedToken_Type.this);
  	  }
    };
  /** @generated */
  public final static int typeIndexID = ChangedToken.typeIndexID;
  /** @generated 
     @modifiable */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.ChangedToken");
 
  /** @generated */
  final Feature casFeat_Position;
  /** @generated */
  final int     casFeatCode_Position;
  /** @generated */ 
  public int getPosition(int addr) {
        if (featOkTst && casFeat_Position == null)
      jcas.throwFeatMissing("Position", "de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.ChangedToken");
    return ll_cas.ll_getIntValue(addr, casFeatCode_Position);
  }
  /** @generated */    
  public void setPosition(int addr, int v) {
        if (featOkTst && casFeat_Position == null)
      jcas.throwFeatMissing("Position", "de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.ChangedToken");
    ll_cas.ll_setIntValue(addr, casFeatCode_Position, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public ChangedToken_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_Position = jcas.getRequiredFeatureDE(casType, "Position", "uima.cas.Integer", featOkTst);
    casFeatCode_Position  = (null == casFeat_Position) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Position).getCode();

  }
}



    
