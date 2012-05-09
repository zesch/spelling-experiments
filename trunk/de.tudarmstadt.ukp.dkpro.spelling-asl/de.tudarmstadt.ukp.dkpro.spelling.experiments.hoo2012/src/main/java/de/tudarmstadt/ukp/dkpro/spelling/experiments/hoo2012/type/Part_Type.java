
/* First created by JCasGen Wed May 09 14:35:09 CEST 2012 */
package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type;

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

/** 
 * Updated by JCasGen Wed May 09 14:35:09 CEST 2012
 * @generated */
public class Part_Type extends Annotation_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Part_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Part_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Part(addr, Part_Type.this);
  			   Part_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Part(addr, Part_Type.this);
  	  }
    };
  /** @generated */
  public final static int typeIndexID = Part.typeIndexID;
  /** @generated 
     @modifiable */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.Part");
 
  /** @generated */
  final Feature casFeat_pid;
  /** @generated */
  final int     casFeatCode_pid;
  /** @generated */ 
  public String getPid(int addr) {
        if (featOkTst && casFeat_pid == null)
      jcas.throwFeatMissing("pid", "de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.Part");
    return ll_cas.ll_getStringValue(addr, casFeatCode_pid);
  }
  /** @generated */    
  public void setPid(int addr, String v) {
        if (featOkTst && casFeat_pid == null)
      jcas.throwFeatMissing("pid", "de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.Part");
    ll_cas.ll_setStringValue(addr, casFeatCode_pid, v);}
    
  
 
  /** @generated */
  final Feature casFeat_count;
  /** @generated */
  final int     casFeatCode_count;
  /** @generated */ 
  public int getCount(int addr) {
        if (featOkTst && casFeat_count == null)
      jcas.throwFeatMissing("count", "de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.Part");
    return ll_cas.ll_getIntValue(addr, casFeatCode_count);
  }
  /** @generated */    
  public void setCount(int addr, int v) {
        if (featOkTst && casFeat_count == null)
      jcas.throwFeatMissing("count", "de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.Part");
    ll_cas.ll_setIntValue(addr, casFeatCode_count, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public Part_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_pid = jcas.getRequiredFeatureDE(casType, "pid", "uima.cas.String", featOkTst);
    casFeatCode_pid  = (null == casFeat_pid) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_pid).getCode();

 
    casFeat_count = jcas.getRequiredFeatureDE(casType, "count", "uima.cas.Integer", featOkTst);
    casFeatCode_count  = (null == casFeat_count) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_count).getCode();

  }
}



    