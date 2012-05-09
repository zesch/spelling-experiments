

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
public class HOOParagraph extends Annotation {
  /** @generated
   * @ordered 
   */
  public final static int typeIndexID = JCasRegistry.register(HOOParagraph.class);
  /** @generated
   * @ordered 
   */
  public final static int type = typeIndexID;
  /** @generated  */
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected HOOParagraph() {}
    
  /** Internal - constructor used by generator 
   * @generated */
  public HOOParagraph(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public HOOParagraph(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public HOOParagraph(JCas jcas, int begin, int end) {
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
  //* Feature: count

  /** getter for count - gets 
   * @generated */
  public int getCount() {
    if (HOOParagraph_Type.featOkTst && ((HOOParagraph_Type)jcasType).casFeat_count == null)
      jcasType.jcas.throwFeatMissing("count", "de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.HOOParagraph");
    return jcasType.ll_cas.ll_getIntValue(addr, ((HOOParagraph_Type)jcasType).casFeatCode_count);}
    
  /** setter for count - sets  
   * @generated */
  public void setCount(int v) {
    if (HOOParagraph_Type.featOkTst && ((HOOParagraph_Type)jcasType).casFeat_count == null)
      jcasType.jcas.throwFeatMissing("count", "de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.HOOParagraph");
    jcasType.ll_cas.ll_setIntValue(addr, ((HOOParagraph_Type)jcasType).casFeatCode_count, v);}    
  }

    