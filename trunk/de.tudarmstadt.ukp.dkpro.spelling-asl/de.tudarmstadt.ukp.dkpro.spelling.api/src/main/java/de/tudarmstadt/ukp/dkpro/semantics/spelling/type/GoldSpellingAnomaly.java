

/* First created by JCasGen Wed Feb 01 11:06:12 CET 2012 */
package de.tudarmstadt.ukp.dkpro.semantics.spelling.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;


/** 
 * Updated by JCasGen Wed Feb 01 11:06:12 CET 2012
 * XML source: /home/zesch/workspace/de.tudarmstadt.ukp.dkpro.spelling-asl/de.tudarmstadt.ukp.dkpro.spelling.api/src/main/resources/desc/type/GoldSpellingAnomaly.xml
 * @generated */
public class GoldSpellingAnomaly extends SpellingAnomaly {
  /** @generated
   * @ordered 
   */
  public final static int typeIndexID = JCasRegistry.register(GoldSpellingAnomaly.class);
  /** @generated
   * @ordered 
   */
  public final static int type = typeIndexID;
  /** @generated  */
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected GoldSpellingAnomaly() {}
    
  /** Internal - constructor used by generator 
   * @generated */
  public GoldSpellingAnomaly(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public GoldSpellingAnomaly(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public GoldSpellingAnomaly(JCas jcas, int begin, int end) {
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

    