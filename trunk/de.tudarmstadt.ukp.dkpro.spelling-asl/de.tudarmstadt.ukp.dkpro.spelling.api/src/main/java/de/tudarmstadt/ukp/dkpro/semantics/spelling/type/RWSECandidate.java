

/* First created by JCasGen Wed Feb 01 11:06:20 CET 2012 */
package de.tudarmstadt.ukp.dkpro.semantics.spelling.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Wed Feb 01 11:06:20 CET 2012
 * XML source: /home/zesch/workspace/de.tudarmstadt.ukp.dkpro.spelling-asl/de.tudarmstadt.ukp.dkpro.spelling.api/src/main/resources/desc/type/RWSECandidate.xml
 * @generated */
public class RWSECandidate extends Annotation {
  /** @generated
   * @ordered 
   */
  public final static int typeIndexID = JCasRegistry.register(RWSECandidate.class);
  /** @generated
   * @ordered 
   */
  public final static int type = typeIndexID;
  /** @generated  */
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected RWSECandidate() {}
    
  /** Internal - constructor used by generator 
   * @generated */
  public RWSECandidate(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public RWSECandidate(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public RWSECandidate(JCas jcas, int begin, int end) {
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
     
}

    