

/* First created by JCasGen Wed May 09 14:35:09 CEST 2012 */
package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Wed May 09 14:35:09 CEST 2012
 * XML source: /home/zesch/workspace/maven.1332361965180/de.tudarmstadt.ukp.dkpro.spelling-asl/de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012/src/main/resources/desc/type/hoo2012.xml
 * @generated */
public class Part extends Annotation {
  /** @generated
   * @ordered 
   */
  public final static int typeIndexID = JCasRegistry.register(Part.class);
  /** @generated
   * @ordered 
   */
  public final static int type = typeIndexID;
  /** @generated  */
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected Part() {}
    
  /** Internal - constructor used by generator 
   * @generated */
  public Part(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public Part(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public Part(JCas jcas, int begin, int end) {
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
  //* Feature: pid

  /** getter for pid - gets 
   * @generated */
  public String getPid() {
    if (Part_Type.featOkTst && ((Part_Type)jcasType).casFeat_pid == null)
      jcasType.jcas.throwFeatMissing("pid", "de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.Part");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Part_Type)jcasType).casFeatCode_pid);}
    
  /** setter for pid - sets  
   * @generated */
  public void setPid(String v) {
    if (Part_Type.featOkTst && ((Part_Type)jcasType).casFeat_pid == null)
      jcasType.jcas.throwFeatMissing("pid", "de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.Part");
    jcasType.ll_cas.ll_setStringValue(addr, ((Part_Type)jcasType).casFeatCode_pid, v);}    
   
    
  //*--------------*
  //* Feature: count

  /** getter for count - gets 
   * @generated */
  public int getCount() {
    if (Part_Type.featOkTst && ((Part_Type)jcasType).casFeat_count == null)
      jcasType.jcas.throwFeatMissing("count", "de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.Part");
    return jcasType.ll_cas.ll_getIntValue(addr, ((Part_Type)jcasType).casFeatCode_count);}
    
  /** setter for count - sets  
   * @generated */
  public void setCount(int v) {
    if (Part_Type.featOkTst && ((Part_Type)jcasType).casFeat_count == null)
      jcasType.jcas.throwFeatMissing("count", "de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.Part");
    jcasType.ll_cas.ll_setIntValue(addr, ((Part_Type)jcasType).casFeatCode_count, v);}    
  }

    