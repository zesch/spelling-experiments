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
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** Holds two aligned sentences from two different Wikipedia revisions.
 * Updated by JCasGen Wed Feb 01 16:36:32 CET 2012
 * XML source: /home/zesch/workspace/de.tudarmstadt.ukp.dkpro.spelling-asl/de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining/src/main/resources/desc/type/RevisionSentencePair.xml
 * @generated */
public class RevisionSentencePair extends Annotation {
  /** @generated
   * @ordered 
   */
  public final static int typeIndexID = JCasRegistry.register(RevisionSentencePair.class);
  /** @generated
   * @ordered 
   */
  public final static int type = typeIndexID;
  /** @generated  */
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected RevisionSentencePair() {}
    
  /** Internal - constructor used by generator 
   * @generated */
  public RevisionSentencePair(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public RevisionSentencePair(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public RevisionSentencePair(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
  @generated modifiable */
  private void readObject() {}
     
 
    
  //*--------------*
  //* Feature: Sentence1

  /** getter for Sentence1 - gets 
   * @generated */
  public Annotation getSentence1() {
    if (RevisionSentencePair_Type.featOkTst && ((RevisionSentencePair_Type)jcasType).casFeat_Sentence1 == null)
      jcasType.jcas.throwFeatMissing("Sentence1", "de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.RevisionSentencePair");
    return (Annotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((RevisionSentencePair_Type)jcasType).casFeatCode_Sentence1)));}
    
  /** setter for Sentence1 - sets  
   * @generated */
  public void setSentence1(Annotation v) {
    if (RevisionSentencePair_Type.featOkTst && ((RevisionSentencePair_Type)jcasType).casFeat_Sentence1 == null)
      jcasType.jcas.throwFeatMissing("Sentence1", "de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.RevisionSentencePair");
    jcasType.ll_cas.ll_setRefValue(addr, ((RevisionSentencePair_Type)jcasType).casFeatCode_Sentence1, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: Sentence2

  /** getter for Sentence2 - gets 
   * @generated */
  public Annotation getSentence2() {
    if (RevisionSentencePair_Type.featOkTst && ((RevisionSentencePair_Type)jcasType).casFeat_Sentence2 == null)
      jcasType.jcas.throwFeatMissing("Sentence2", "de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.RevisionSentencePair");
    return (Annotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((RevisionSentencePair_Type)jcasType).casFeatCode_Sentence2)));}
    
  /** setter for Sentence2 - sets  
   * @generated */
  public void setSentence2(Annotation v) {
    if (RevisionSentencePair_Type.featOkTst && ((RevisionSentencePair_Type)jcasType).casFeat_Sentence2 == null)
      jcasType.jcas.throwFeatMissing("Sentence2", "de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.RevisionSentencePair");
    jcasType.ll_cas.ll_setRefValue(addr, ((RevisionSentencePair_Type)jcasType).casFeatCode_Sentence2, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    
