
/* First created by JCasGen Wed Feb 01 11:06:12 CET 2012 */
package de.tudarmstadt.ukp.dkpro.semantics.spelling.type;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly_Type;

/** 
 * Updated by JCasGen Wed Feb 01 11:06:12 CET 2012
 * @generated */
public class GoldSpellingAnomaly_Type extends SpellingAnomaly_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (GoldSpellingAnomaly_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = GoldSpellingAnomaly_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new GoldSpellingAnomaly(addr, GoldSpellingAnomaly_Type.this);
  			   GoldSpellingAnomaly_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new GoldSpellingAnomaly(addr, GoldSpellingAnomaly_Type.this);
  	  }
    };
  /** @generated */
  public final static int typeIndexID = GoldSpellingAnomaly.typeIndexID;
  /** @generated 
     @modifiable */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.tudarmstadt.ukp.dkpro.semantics.spelling.type.GoldSpellingAnomaly");



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public GoldSpellingAnomaly_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

  }
}



    