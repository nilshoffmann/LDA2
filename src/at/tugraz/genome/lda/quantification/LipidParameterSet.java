/* 
 * This file is part of Lipid Data Analyzer
 * Lipid Data Analyzer - Automated annotation of lipid species and their molecular structures in high-throughput data from tandem mass spectrometry
 * Copyright (c) 2017 Juergen Hartler, Andreas Ziegl, Gerhard G. Thallinger 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER. 
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * by the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. 
 *  
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Please contact lda@genome.tugraz.at if you need additional information or 
 * have any questions.
 */ 

package at.tugraz.genome.lda.quantification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import at.tugraz.genome.lda.exception.ChemicalFormulaException;
import at.tugraz.genome.lda.exception.NoRuleException;
import at.tugraz.genome.lda.exception.RulesException;
import at.tugraz.genome.lda.msn.LipidomicsMSnSet;
import at.tugraz.genome.lda.msn.RulesContainer;
import at.tugraz.genome.lda.utils.StaticUtils;
import at.tugraz.genome.maspectras.parser.exceptions.SpectrummillParserException;
import at.tugraz.genome.maspectras.quantification.CgParameterSet;
import at.tugraz.genome.maspectras.quantification.CgProbe;

/**
 * class containing information about an analyte identification
 * @author Juergen Hartler
 *
 */
public class LipidParameterSet extends CgParameterSet
{
  private Integer doubleBonds_;
  private String modificationName_;
  private String analyteFormula_;
  private String modificationFormula_;
  private String chemicalFormula_;
  private String chemicalFormulaWODeducts_;
  private Integer charge_;
  private String rt_;
  /** (total) number of hydroxylation sites present on the molecule*/
  private Integer ohNumber_;
  
  public final static String MOD_SEPARATOR = "_-_";
  public final static String MOD_SEPARATOR_HR = "_";
  
  /** if there is an isobaric peak split, a hard limit is set on one side of the peak*/
  private float lowerRtHardLimit_;
  /** if there is an isobaric peak split, a hard limit is set on one side of the peak*/
  private float upperRtHardLimit_;
  /** if there is a split according to MSn intensities - a percentage of the usable peak intensity is stored*/
  private float percentalSplit_;
  /** if there is no distinct fragment, this is activated to choose the correct one based on the retention time when a parameter is set in the rules*/
  private boolean choseMoreLikelyRtWhenEqualMSn_;
  
  /**
   * for creating a clone of a previous LipidParameterSet
   * @param set the set to be cloned
   */
  public LipidParameterSet(LipidParameterSet set){
    this(set.Mz[0],set.Peptide,set.doubleBonds_,set.ohNumber_,set.modificationName_,set.rt_,set.analyteFormula_,
        set.modificationFormula_,set.charge_);
    this.Mz = set.Mz;
    this.Area = set.Area;
    this.LowerMzBand = set.LowerMzBand;
    this.UpperMzBand = set.UpperMzBand;
    this.ValleyMethod = set.ValleyMethod;
    this.m_probes = new ArrayList<CgProbe>(set.m_probes);
    this.isotopicProbes_ = new Vector<Vector<CgProbe>>(set.isotopicProbes_);
    this.lowerRtHardLimit_ = set.getLowerRtHardLimit();
    this.upperRtHardLimit_ = set.getUpperRtHardLimit();
    this.percentalSplit_ = set.getPercentalSplit();
  }
  
  /**
   * this constructor is kept for backward compatibility - might be deprecated in future
   * @param mz the anticipated m/z value
   * @param name the name of the analyte class
   * @param doubleBonds the number of double bonds - if any
   * @param ohNumber the number of hydroxylation sites
   * @param modificationName the name of the adduct/modification for ionization
   * @param rt retention time
   * @param analyteFormula the chemical formula of the neutral analyte
   * @param modificationFormula the chemical formula of the modification
   * @param charge the charge
   */
  public LipidParameterSet(float mz, String name,
      Integer doubleBonds, Integer ohNumber, String modificationName, String rt, String analyteFormula,
      String modificationFormula, Integer charge)
  {
    //TODO: the information about the hydroxylation sites has to come from somewhere else!!!!!!!
    //this(mz, name, doubleBonds, modificationName, rt, analyteFormula, modificationFormula, charge, 0);
    this(mz, name, doubleBonds, modificationName, rt, analyteFormula, modificationFormula, charge, ohNumber);
  }
  
  /**
   * Constructor for LipidParameterSet
   * @param mz the anticipated m/z value
   * @param name the name of the analyte class
   * @param doubleBonds the number of double bonds - if any
   * @param modificationName the name of the adduct/modification for ionization
   * @param rt retention time
   * @param analyteFormula the chemical formula of the neutral analyte
   * @param modificationFormula the chemical formula of the modification
   * @param charge the charge
   * @param ohNumber the number of hydroxylation sites (total)
   */
  public LipidParameterSet(float mz, String name,
      Integer doubleBonds, String modificationName, String rt, String analyteFormula,
      String modificationFormula, Integer charge, Integer ohNumber)
  {
    super(mz, name,mz,-1,-1, -1,-1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1);
    this.doubleBonds_ = doubleBonds;
    this.modificationName_ = modificationName;
    this.analyteFormula_ = analyteFormula;
    this.modificationFormula_ = modificationFormula;
    if (modificationFormula_!=null && modificationFormula_.length()>1){
      try {
        Hashtable<String,Integer> formAnal = StaticUtils.categorizeFormula(this.analyteFormula_);
        Hashtable<String,Integer> formAnalWODeducts = new Hashtable<String,Integer>(formAnal);
        Hashtable<String,Integer> formMod = StaticUtils.categorizeFormula(this.modificationFormula_);
        for (String element : formMod.keySet()){
          int amount = formMod.get(element);
          int amountWODeducts = amount;
          if (formAnal.containsKey(element)) {
            amount+=formAnal.get(element);
            if (amountWODeducts>0)
              amountWODeducts+=formAnalWODeducts.get(element);
            else
              amountWODeducts=formAnalWODeducts.get(element);
          }
          formAnal.put(element, amount);
          if (amountWODeducts>0)
            formAnalWODeducts.put(element, amountWODeducts);
        }
        this.chemicalFormula_ = "";
        for (String element : formAnal.keySet()){
          chemicalFormula_ += element+String.valueOf(formAnal.get(element))+" ";
        }
        if (chemicalFormula_.length()>0) chemicalFormula_ = chemicalFormula_.substring(0,chemicalFormula_.length()-1); 
        this.chemicalFormulaWODeducts_ = "";
        for (String element : formAnalWODeducts.keySet()){
          chemicalFormulaWODeducts_ += element+String.valueOf(formAnalWODeducts.get(element))+" ";
        }
        if (chemicalFormulaWODeducts_.length()>0) chemicalFormulaWODeducts_ = chemicalFormulaWODeducts_.substring(0,chemicalFormulaWODeducts_.length()-1); 

      } catch (ChemicalFormulaException e) {
        e.printStackTrace();
      }
    } else {
      chemicalFormula_ = analyteFormula_;
      chemicalFormulaWODeducts_ = analyteFormula_;
    }
    charge_ = charge;
    this.rt_ = rt;
    this.lowerRtHardLimit_ = -1f;
    this.upperRtHardLimit_ = -1f;
    this.percentalSplit_ = -1;
    this.ohNumber_ = ohNumber;
    this.choseMoreLikelyRtWhenEqualMSn_ = false;
  }
  
  public String getNameString(){
    return StaticUtils.generateLipidNameString(Peptide, doubleBonds_,rt_);
  }
  
  public String getNameStringWithoutRt(){
    return StaticUtils.generateLipidNameString(Peptide, doubleBonds_);
  }
 
  public String getName(){
    return Peptide;
  }
  
  public Integer getDoubleBonds(){
    return this.doubleBonds_;
  }
  

  public String getModificationName()
  {
    return modificationName_;
  }

  public String getAnalyteFormula()
  {
    return analyteFormula_;
  }

  public String getModificationFormula()
  {
    return modificationFormula_;
  }

  public Integer getCharge()
  {
    return charge_;
  }

  public void setCharge(Integer charge)
  {
    this.charge_ = charge;
  }
  
  public String getRt()
  {
// this backward compatibility was removed in the course of the shotgun extension
//    if (rt_!=null && rt_.length()>0) return rt_;
//    else{
//      // this else is for backward compatibility
//      String rt = null;
//      Vector<Vector<CgProbe>> isotopes = this.getIsotopicProbes();
//      if (isotopes.size()>0){
//        Vector<CgProbe> probes = isotopes.get(0);
//        rt = "0.0";
//        float highestArea = 0f;
//        for (CgProbe probe:probes){
//          if (probe.Area>highestArea){
//            highestArea = probe.Area;
//            rt = String.valueOf(probe.Peak/60f);
//          }
//        }
//      }
//      return rt;
//    }
    return rt_;
  }
  

  public String getChemicalFormula()
  {
    return chemicalFormula_;
  }
  
  /**
   * 
   * @return the chemical formula that ignores any deductions by ionization
   */
  public String getChemicalFormulaWODeducts()
  {
    return chemicalFormulaWODeducts_;
  }

  public void setRt(String rt)
  {
    this.rt_ = rt;
  }

  public int getMinIsotope(){
    int isotope = 0;
    if (ProbeCount()>0)
      for (int i=0; i!=ProbeCount();i++){
      int isoNumber = super.Probe(i).isotopeNumber;
      if (isoNumber<isotope)
        isotope = isoNumber;
    } else if (super.getIsotopicProbes()!=null){
      for (Vector<CgProbe> isoProbes: super.getIsotopicProbes()){
        if (isoProbes.size()>0){
          int isoNumber = isoProbes.get(0).isotopeNumber;
          if (isoNumber<isotope)isotope = isoNumber;
        }
      }
    }
    
    return isotope;
  }

  public String getNameIncludingModification(){
    return this.getNameString()+MOD_SEPARATOR+this.modificationName_;
  }
  
  public String getNamePlusModHumanReadable(){
    String name = this.getNameString();
    if (this.modificationName_!=null && this.modificationName_.length()>0)
      name += MOD_SEPARATOR_HR+this.modificationName_;
    return name;
  }
 
  public void setNameString(String name)
  {
    String[] firstSplit = name.split( ":" );    
    Peptide = firstSplit[0];    
    String[] secondSplit = firstSplit[1].toString().split( "_" );    
    doubleBonds_ = Integer.parseInt(secondSplit[0]);    
    rt_ = secondSplit[1];
  }
  
  public void setModificationName(String name)
  {
    modificationName_ = name;
  }
  
  public void setMZ(float[] mz)
  {
    this.Mz = mz;
  }

  /**
   * if there is an isobaric peak split, a hard limit is set on one side of the peak
   * @return the lower hard limit (if there is no limit, -1 is returned)
   */
  public float getLowerRtHardLimit()
  {
    return lowerRtHardLimit_;
  }

  /**
   * if there is an isobaric peak split, a hard limit is set on one side of the peak
   * @param lowerRtHardLimit sets the lower hard limit (if there is no limit, use -1)
   */
  public void setLowerRtHardLimit(float lowerRtHardLimit)
  {
    this.lowerRtHardLimit_ = lowerRtHardLimit;
  }

  /**
   * if there is an isobaric peak split, a hard limit is set on one side of the peak
   * @return the upper hard limit (if there is no limit, -1 is returned)
   */
  public float getUpperRtHardLimit()
  {
    return upperRtHardLimit_;
  }

  /**
   * if there is an isobaric peak split, a hard limit is set on one side of the peak
   * @param upperRtHardLimit sets the lower hard limit (if there is no limit, use -1)
   */
  public void setUpperRtHardLimit(float upperRtHardLimit)
  {
    this.upperRtHardLimit_ = upperRtHardLimit;
  }

  /**
   *  if there is a split according to MSn intensities - a percentage of the usable peak intensity is stored
   * @return a percentage of the usable peak intensity
   */
  public float getPercentalSplit()
  {
    return percentalSplit_;
  }

  /**
   * if there is a split according to MSn intensities - a percentage of the usable peak intensity is stored
   * @param percentalSplit the percentage of the usable peak intensity is stored
   */
  public void setPercentalSplit(float percentalSplit)
  {
    this.percentalSplit_ = percentalSplit;
  }
  
  /**
   * 
   * @return the area of the peak (if there is percental split - this method respects it)
   */
  public float getArea(){
    float area = super.Area;
    return this.getArea(area);
  }
  
  
  /**
   * 
   * @param maxIsotope the highest isotope to use for the total area
   * @return the area of the peak up to a certain isotope (if there is percental split - this method respects it)
   */
  public float getArea(int maxIsotope){
    float area = 0f;
    for (int i=0; i!=(maxIsotope+1) && i!=isotopicProbes_.size() ; i++){
      for (CgProbe probe : this.isotopicProbes_.get(i)){
        area += probe.Area;
      }
    }
    return this.getArea(area);
  }
  
  
  /**
   * returns the area itself or splits it in case there is a percentual split
   * @param fullArea the area
   * @return the area itself or splits it in case there is a percentual split
   */
  private float getArea(float fullArea){
    float area = fullArea;
    if (this.percentalSplit_>=0) area=(area*this.percentalSplit_)/100f;
    return area;
  }
  
  
  /**
   * returns the number of hydroxylation sites
   * @return the number of hydroxylation sites
   */
  public Integer getOhNumber()
  {
    return ohNumber_;
  }
  

  /**
   * @param className required for adduct insensitive filtering (only the ones where RetentionTimePostprocessing=true should be used for the model)
   * @return true if the confidence in the peak is high enough to use it as model for the fitting of the RT prediction curve
   * @throws SpectrummillParserException 
   * @throws IOException 
   * @throws NoRuleException 
   * @throws RulesException 
   */
  public boolean isSuitableForRtProcessingHit(String className) throws RulesException, NoRuleException, IOException, SpectrummillParserException{
    return ((this instanceof LipidomicsMSnSet) && this.lowerRtHardLimit_<0f && this.upperRtHardLimit_<0f && this.percentalSplit_<0f && RulesContainer.isRtPostprocessing(StaticUtils.getRuleName(className,modificationName_)));
  }

  
  /**
   * 
   * @return true when a selection between two equally matching adducts has to be made based on retention time
   */
  public boolean isChoseMoreLikelyRtWhenEqualMSn()
  {
    return choseMoreLikelyRtWhenEqualMSn_;
  }

  /**
   * sets whether a selection between two equally matching adducts has to be made based on retention time
   * @param choseMoreLikelyRtWhenEqualMSn whether a selection between two equally matching adducts has to be made based on retention time
   */
  public void setChoseMoreLikelyRtWhenEqualMSn(boolean choseMoreLikelyRtWhenEqualMSn)
  {
    this.choseMoreLikelyRtWhenEqualMSn_ = choseMoreLikelyRtWhenEqualMSn;
  }
  
  
  
}
