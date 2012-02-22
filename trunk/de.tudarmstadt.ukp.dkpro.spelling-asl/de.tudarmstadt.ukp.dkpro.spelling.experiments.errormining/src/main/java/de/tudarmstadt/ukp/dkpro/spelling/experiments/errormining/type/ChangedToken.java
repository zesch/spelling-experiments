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
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** Marks a token that has been changed from one revision to another.
 * Updated by JCasGen Wed Feb 01 16:36:03 CET 2012
 * XML source: /home/zesch/workspace/de.tudarmstadt.ukp.dkpro.spelling-asl/de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining/src/main/resources/desc/type/ChangedToken.xml
 * @generated */
public class ChangedToken extends Annotation {
  /** @generated
   * @ordered 
   */
  public final static int typeIndexID = JCasRegistry.register(ChangedToken.class);
  /** @generated
   * @ordered 
   */
  public final static int type = typeIndexID;
  /** @generated  */
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected ChangedToken() {}
    
  /** Internal - constructor used by generator 
   * @generated */
  public ChangedToken(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public ChangedToken(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public ChangedToken(JCas jcas, int begin, int end) {
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
  //* Feature: Position

  /** getter for Position - gets The position in the sentence as the offset counted in tokens.
"x a a a a" => 0
"a a x a a" => 2
"a a a a x" => 4
   * @generated */
  public int getPosition() {
    if (ChangedToken_Type.featOkTst && ((ChangedToken_Type)jcasType).casFeat_Position == null)
      jcasType.jcas.throwFeatMissing("Position", "de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.ChangedToken");
    return jcasType.ll_cas.ll_getIntValue(addr, ((ChangedToken_Type)jcasType).casFeatCode_Position);}
    
  /** setter for Position - sets The position in the sentence as the offset counted in tokens.
"x a a a a" => 0
"a a x a a" => 2
"a a a a x" => 4 
   * @generated */
  public void setPosition(int v) {
    if (ChangedToken_Type.featOkTst && ((ChangedToken_Type)jcasType).casFeat_Position == null)
      jcasType.jcas.throwFeatMissing("Position", "de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.ChangedToken");
    jcasType.ll_cas.ll_setIntValue(addr, ((ChangedToken_Type)jcasType).casFeatCode_Position, v);}    
  }

    
