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

package at.tugraz.genome.lda.vos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import at.tugraz.genome.lda.LipidomicsConstants;
import at.tugraz.genome.lda.exception.ChemicalFormulaException;
import at.tugraz.genome.lda.exception.LipidCombinameEncodingException;
import at.tugraz.genome.lda.utils.StaticUtils;
import at.tugraz.genome.maspectras.parser.exceptions.SpectrummillParserException;
import at.tugraz.genome.maspectras.parser.spectrummill.ElementConfigParser;
import at.tugraz.genome.voutils.GeneralComparator;

/**
 * 
 * @author Juergen Hartler
 *
 */
public class ResultAreaVO
{
  private String name_;
  private Integer dbs_;
  private String expName_;
  private String rtOriginal_;
  /** the original retention time values to have a lookup to the objects in the result files*/
  private Hashtable<String,Hashtable<String,String>> allOriginalRts_ = new Hashtable<String,Hashtable<String,String>>();
  private String rt_;
  private Hashtable<String,Integer> chemicalFormula_;
  private Hashtable <String,Hashtable<String,Integer>> modFormulas_; 
  private Double neutralMass_;
  private Hashtable<String,Double> mass_;
  private Hashtable<String,Double> expMass_;
  private Hashtable<String,Integer> charge_;
  private Hashtable<String,Double> retentionTime_;
  private Hashtable<String,Vector<Double>> areas_;
  private Hashtable<String,Vector<Boolean>> moreThanOnePeak_;
  /** if there is a split according to MSn intensities - a percentage of the usable peak intensity is stored*/
  private float percentalSplit_;
  /** true when this value is an internal standard*/
  private boolean internalStandard_;
  /** true when this value is an external standard*/
  private boolean externalStandard_;
  /** sum chain information over all adducts*/
  private LinkedHashMap<String,Double> chainInformationTotal_;
  /** chain information for each adduct*/
  private Hashtable<String,LinkedHashMap<String,Double>> chainInformationForEachModification_;
  /** is MSn evidence available*/
  private boolean msnEvidence_;
  
  
  
  /**
   * public constructor for creating a ResultAreaVO
   * @param name the name of the analyte
   * @param dbs the amount of double bonds
   * @param rt an identifier String for the retention time
   * @param expName the abbreviated name of the experiment 
   * @param chemicalFormula the chemical formula of the analyte
   * @param percentalSplit the percentual split if present
   * @param neutralMass the neutral mass of the analyte
   * @param internalStandard true when this value is an internal standard
   * @param externalStandard true when this value is an external standard
   * @throws ChemicalFormulaException thrown when an element is missing in the elementconfig.xml
   */
  public ResultAreaVO(String name, Integer dbs, String rt, String expName, String chemicalFormula, float percentalSplit,double neutralMass,
      boolean internalStandard, boolean externalStandard) throws ChemicalFormulaException
  {
    this(name,dbs,expName,chemicalFormula,percentalSplit,neutralMass,internalStandard,externalStandard);
    this.rtOriginal_ = rt;
    this.rt_ = rt;
    allOriginalRts_ = new Hashtable<String,Hashtable<String,String>>();
    chainInformationForEachModification_ = new Hashtable<String,LinkedHashMap<String,Double>>(); 
  }
  
  /**
   * private constructor for creating a ResultAreaVO
   * @param name the name of the analyte
   * @param dbs the amount of double bonds
   * @param expName the abbreviated name of the experiment 
   * @param chemicalFormula the chemical formula of the analyte
   * @param percentalSplit the percentual split if present
   * @param neutralMass the neutral mass of the analyte
   * @param internalStandard true when this value is an internal standard
   * @param externalStandard true when this value is an external standard
   * @throws ChemicalFormulaException thrown when an element is missing in the elementconfig.xml
   */
  private ResultAreaVO(String name, Integer dbs, String expName, String chemicalFormula, float percentalSplit,double neutralMass,
      boolean internalStandard, boolean externalStandard) throws ChemicalFormulaException
  {
    super();
    name_ = name;
    dbs_ = dbs;
    expName_ = expName;
    chemicalFormula_ = StaticUtils.categorizeFormula(chemicalFormula);
    modFormulas_ = new Hashtable <String,Hashtable<String,Integer>>();
    neutralMass_ = neutralMass;
    mass_ = new Hashtable<String,Double>();
    expMass_ = new Hashtable<String,Double>();
    charge_ = new Hashtable<String,Integer>();
    retentionTime_ = new Hashtable<String,Double>();
    areas_ = new Hashtable<String,Vector<Double>>();
    moreThanOnePeak_ = new Hashtable<String,Vector<Boolean>>();
    rtOriginal_ = null;
    rt_ = null;
    percentalSplit_ = 1f;
    if(percentalSplit>=1) percentalSplit_ = percentalSplit/100f;
    internalStandard_ = internalStandard;
    externalStandard_ = externalStandard;
    chainInformationTotal_ = new LinkedHashMap<String,Double>();
    msnEvidence_ = false;
  }
  
  
  /**
   * adds a result belonging to the same analyte species (e.g. another adduct/modification)
   * @param modName the modification/adduct name
   * @param modFormula the chemical formula of the modification/adduct
   * @param mass the theoretical m/z value
   * @param expMass the measured m/z value
   * @param charge the presumed charge
   * @param rt the retention time
   * @throws ChemicalFormulaException
   */
  public void addResultPart(String modName, String modFormula,double mass,double expMass,int charge, String rt) throws ChemicalFormulaException{
    String modToAdd = getNotNullName(modName);
    // this is truly a new modification, otherwise this hit is just a peak with a different retention time; no new modification
    Hashtable<String,String> rts = new Hashtable<String,String>();
    if (!modFormulas_.containsKey(modToAdd)){
      String modFormToAdd = getNotNullName(modFormula);
      modFormulas_.put(modToAdd, StaticUtils.categorizeFormula(modFormToAdd));
      mass_.put(modToAdd, mass);
      expMass_.put(modToAdd, expMass);
      charge_.put(modToAdd, charge);
      Vector<Double> isoAreas = new Vector<Double>();
      areas_.put(modToAdd, isoAreas);
      Vector<Boolean> isoMoreThanOne = new Vector<Boolean>();
      moreThanOnePeak_.put(modToAdd, isoMoreThanOne);
    }else if (rt!=null) {
      rts = allOriginalRts_.get(modName);
    }
    if (rt!=null){
      rts.put(rt, rt);
      allOriginalRts_.put(modToAdd, rts);
    }
  }

  /**
   * adds information about detected chains
   * @param modName the modification name the chains were detected for
   * @param areas a hash table of the areas for each coeluting species
   * @param returns the nr of chains detected for this species
   * @throws LipidCombinameEncodingException thrown if there is something wrong with the lipid name encoding
   */
  public int addChainInformation(String modName, Hashtable<String,Double> areas) throws LipidCombinameEncodingException {
    double area;
    int nrChains = 0;
    int nrChainsDecoded;
    LinkedHashMap<String,Double> chainInformation = new LinkedHashMap<String,Double>();
    if (chainInformationForEachModification_.containsKey(modName))
      chainInformation = chainInformationForEachModification_.get(modName);
    List<DoubleStringVO> modValues = new ArrayList<DoubleStringVO>();
    List<DoubleStringVO> totalValues = new ArrayList<DoubleStringVO>();
    for (String combiName : areas.keySet()) {
      area = 0d;
      String name = StaticUtils.encodeLipidCombi(StaticUtils.sortChainVOs(StaticUtils.decodeLipidNamesFromChainCombi(combiName)));
      //String name = combiName;
      //find out if there is already a combination with this name
      if (chainInformation.containsKey(name)) 
        area = chainInformation.get(name);
//      }else {
//        for (String otherCombi : chainInformation.keySet()) {
//          if (StaticUtils.isAPermutedVersion(name, otherCombi, LipidomicsConstants.CHAIN_COMBI_SEPARATOR)) {
//            name = otherCombi;
//            area = chainInformation.get(name);
//            break;
//          }
//        }
//      }
      area+=areas.get(combiName);
      nrChainsDecoded = StaticUtils.decodeLipidNamesFromChainCombi(combiName).size();
      if (nrChainsDecoded>nrChains)
        nrChains = nrChainsDecoded;
      modValues.add(new DoubleStringVO(name,area));
      //do the same for the total areas
//      String totalName = name;
      area=0;
      if (chainInformationTotal_.containsKey(name))
        area = chainInformationTotal_.get(name);
//      if (chainInformationTotal_.containsKey(totalName)) {
//        area = chainInformationTotal_.get(totalName);
//      } else {
//        for (String otherCombi : chainInformation.keySet()) {
//          if (StaticUtils.isAPermutedVersion(totalName, otherCombi, LipidomicsConstants.CHAIN_COMBI_SEPARATOR)) {
//            totalName = otherCombi;
//            area = chainInformation.get(totalName);
//            break;
//          }
//        }       
//      }
      area+=areas.get(combiName);
      totalValues.add(new DoubleStringVO(name,area));      
    }
    chainInformationForEachModification_.put(modName, getAreaSortedLinkedHashMap(modValues));
    chainInformationTotal_ = getAreaSortedLinkedHashMap(totalValues);
    return nrChains;
  }
  
  
  private String getNotNullName(String name){
    if (name!=null&&name.length()>0) return name;
    else return "";
  }
  
  public String getMoleculeName()
  {
    return StaticUtils.generateLipidNameString(name_, dbs_, rt_);
  } 
  
  public String getMoleculeNameWoRT()
  {
    return StaticUtils.generateLipidNameString(name_, dbs_,-1);
  }
  
  public String getExpName()
  {
    return expName_;
  }  
//  public String getChemicalFormula()
//  {
//    return chemicalFormula;
//  }
  public double getTheoreticalIsotopeValue(ElementConfigParser elementParser_, int isosDesired){
    double result = 0;
    for (String modString : areas_.keySet()){
      String chemicalFormula = getChemicalFormula(modString);
//      double refValue = currentValue;
      try {
        Vector<Double> distris = StaticUtils.calculateChemicalFormulaIntensityDistribution(elementParser_, chemicalFormula, isosDesired, false);;
        Vector<Double> areas = areas_.get(modString);
        Vector<Double> isoValues = new Vector<Double>();
        for (int i=0; i!=isosDesired;i++){
          if (i<areas.size())
            isoValues.add(areas.get(i));
          else{
            isoValues.add(isoValues.get(0)*distris.get(i));
          }  
          //refValue += zeroIsoValue*distris.get(i);
        }
        result+=getTotalArea(isoValues,isosDesired);
      }
      catch (SpectrummillParserException e) {
        e.printStackTrace();
      }
    }    
    return result;
  }
  
  private String getChemicalFormula(String modString){
    String formula = "";
    Hashtable<String,Integer> modFormula = new Hashtable<String,Integer>();
    if (modFormulas_.containsKey(modString)) modFormula = modFormulas_.get(modString);
    Vector<String> elementsOrder = new Vector<String>(); 
    for (String element: chemicalFormula_.keySet()){
      if (element.equalsIgnoreCase("C")) elementsOrder.add(0,element);
      else elementsOrder.add(element);
    }
    for (String element: elementsOrder){
      if (formula.length()>0) formula+=" ";
      int amount = chemicalFormula_.get(element);
      if (modFormula.containsKey(element)) amount+=modFormula.get(element);
      formula+=element+String.valueOf(amount);
    }
    for (String element : modFormula.keySet()){
      if (!chemicalFormula_.containsKey(element)){
        if (formula.length()>0) formula+=" ";
        formula+=element+String.valueOf(modFormula.get(element));
      }
    }
    return formula;
  }
  
  public String getChemicalFormulaBase(){
    return getChemicalFormula("");
  }
  
  /**
   * @return the chemical formula as a hash table
   */
  public Hashtable<String,Integer> getChemicalFormulaElements(){
    return this.chemicalFormula_;
  }
  
  public Vector<Double> getWeightedNeutralMass()
  {
    int maxIsotpe = getMaxIsotope();
    Vector<Double> weightedMasses = new Vector<Double>();
    for (int i=0;i!=maxIsotpe;i++){
      double totalArea = 0;
      double totalMassArea = 0;
//      for (String modName : areas_.keySet()){
//        double area = getTotalArea(areas_.get(modName),i+1);
//        double mass = mass_.get(modName);
//        totalArea += area;
//        totalMassArea += area*mass;
//      }
      for (String modName : areas_.keySet()){
        Vector<Double> areas = areas_.get(modName);
        for (int j=0; j!=(i+1)&&j!=areas.size();j++){
          double area = areas.get(j);
          double mass = neutralMass_+LipidomicsConstants.getNeutronMass()*j;
          totalArea += area;
          totalMassArea += area*mass;
        }
      }
      weightedMasses.add(totalMassArea/totalArea);
    }
    return weightedMasses;
  }

//  public Vector<Double> getAreas()
//  {
//    return areas;
//  }
  
  public Hashtable<Integer,Boolean> addArea(String modificationName, int iso, double area){
    String modName = getNotNullName(modificationName);
    Vector<Double> areas = areas_.get(modName);
    Vector<Boolean> moreThanOnePeak = moreThanOnePeak_.get(modName);
    boolean isAdditionalPeak = false;
    int isotope = iso;
    if (isotope<0) isotope = isotope*-1;
    if (areas.size()<=isotope){
      areas.add(area*percentalSplit_);
      moreThanOnePeak.add(false);
    }else{
      double totalArea = areas.get(isotope)+area*percentalSplit_;
      areas.remove(isotope);
      areas.add(isotope, totalArea);
      moreThanOnePeak.remove(isotope);
      moreThanOnePeak.add(isotope,true);
      isAdditionalPeak = true;
    }
    areas_.put(modName, areas);
    moreThanOnePeak_.put(modName, moreThanOnePeak);
    if (isAdditionalPeak){
      Hashtable<Integer,Boolean> mtp = new Hashtable<Integer,Boolean>();
      for (int i=0;i!=moreThanOnePeak.size();i++) mtp.put(i, moreThanOnePeak.get(i));
      return mtp;
    }
    return null;
  }
  
  public double getTotalArea(int amountOfIsotopes){
    double totalArea = 0;
    for (Vector<Double> areas : areas_.values())
      totalArea+=getTotalArea(areas,amountOfIsotopes);
    return totalArea;
  }
  
  public double getTotalAreaOfModification(String mod, int amountOfIsotopes){
    return this.getTotalArea(areas_.get(mod),amountOfIsotopes);
  }
  
  private double getTotalArea(Vector<Double> areas, int amountOfIsotopes){
    double totalArea = 0;
    if (areas!=null){
      for (int i=0; i!=areas.size()&&i<amountOfIsotopes; i++){
        totalArea += areas.get(i);
      }
    }
    return totalArea;
  }

  public double getRetentionTime(String modificationName)
  {
    return retentionTime_.get(getNotNullName(modificationName));
  }

  public void setRetentionTime(String modificationName, double retentionTime)
  {
    retentionTime_.put(getNotNullName(modificationName), retentionTime);
  }
  
  public Hashtable<String,Double>  getRetentionTimes()
  {
    return retentionTime_;
  }


  public Hashtable<String,Boolean> getMoreThanOnePeak(int amountOfIsotopes)
  {
//    boolean moreThanOnePeak = false;
//    for (Vector<Boolean> moreThanOnePeaks : moreThanOnePeak_.values())
//      for (int i=0; i!=moreThanOnePeaks.size()&&i<amountOfIsotopes; i++){
//        if (moreThanOnePeaks.get(i))
//          moreThanOnePeak = true;
//    }
//    return moreThanOnePeak;
    Hashtable<String,Boolean> moreThanOnePeakHash = new Hashtable<String,Boolean>();
    for (String modName : moreThanOnePeak_.keySet()){
      boolean moreThanOnePeak = false;
      Vector<Boolean> moreThanOnePeaks = moreThanOnePeak_.get(modName);
      for (int i=0; i!=moreThanOnePeaks.size()&&i<amountOfIsotopes; i++){
        if (moreThanOnePeaks.get(i))
          moreThanOnePeak = true;
      }
      moreThanOnePeakHash.put(modName, moreThanOnePeak);
    }
    return moreThanOnePeakHash;
  }
  
  public void setMoreThanOnePeak(String modificationName, Hashtable<Integer,Boolean> moreThanOnePeak){
    List<Integer> isotopes = new ArrayList<Integer>(moreThanOnePeak.keySet());
    Vector<Boolean> moreThanOnePeaks = new Vector<Boolean>();
    Collections.sort(isotopes);
    for (int i=0;i!=isotopes.size();i++){
      moreThanOnePeaks.add(moreThanOnePeak.get(isotopes.get(i)));
    }
    moreThanOnePeak_.put(modificationName, moreThanOnePeaks);
  }
  
  public int getMaxIsotope(){
    int maxIsotope = 0;
    for (Vector<Double> areas : areas_.values()){
      int size = areas.size();
      if (maxIsotope<size)maxIsotope=size;
    }
    return maxIsotope;
  }

  public void setRt(String rt)
  {
    this.rt_ = rt;
  }

  public String getRt()
  {
    return rt_;
  }

  public String getRtOriginal()
  {
    return rtOriginal_;
  }
  
  public void setRtOriginal(String rt){
    this.rtOriginal_ = rt;
    this.rt_ = rt;
  }
  
  public float getHighestZeroIsoArea(String modification){
    if (areas_.get(modification)!=null && areas_.get(modification).get(0)!=null)
        return areas_.get(modification).get(0).floatValue();
    else
      return 0f;
  }
  
  public boolean hasModification(String mod){
    return modFormulas_.containsKey(mod);
  }
  
  public String getModificationFormula(String mod){
    String modFormula = "";
    if (hasModification(mod)){
      Hashtable<String,Integer> formula = modFormulas_.get(mod);

      for (String element : formula.keySet()){
        String amount = String.valueOf(formula.get(element));
        if (amount.startsWith("-")){
          amount = amount.substring(1);
          modFormula+="-";
        }
        modFormula+=element+amount;
      }
    }
    return modFormula;
  }
  
  public void combineVOs(ResultAreaVO other){
    double highestArea = 0;
    for (String mod : areas_.keySet()){
      if (areas_.get(mod).size()>0 && areas_.get(mod).get(0)>highestArea){
        highestArea = areas_.get(mod).get(0);
      }
    }

    for (String mod : other.mass_.keySet()){
      float highestZeroArea = getHighestZeroIsoArea(mod);
      if (!this.mass_.containsKey(mod)){
        try {
          this.addResultPart(mod, other.getChemicalFormula(mod), other.mass_.get(mod), other.expMass_.get(mod), other.charge_.get(mod), null);
        }
        catch (ChemicalFormulaException e) {
          e.printStackTrace();
        }
      }
      Vector<Double> areas = other.areas_.get(mod);
      Hashtable<Integer,Boolean> moreThanOnePeak = new Hashtable<Integer,Boolean>();
      for (int i=0; i!=areas.size(); i++){
        Hashtable<Integer,Boolean> mtp = addArea(mod,i,areas.get(i));
        if (mtp!=null) moreThanOnePeak = mtp;
        if (moreThanOnePeak.containsKey(i))mtp.put(i, true);
        else moreThanOnePeak.put(i, false);
        if (i==0 && areas.get(i)>highestZeroArea){
          setRetentionTime(mod,other.getRetentionTime(mod));
        }
        if (i==0 && areas.get(i)>highestArea){
          setRtOriginal(other.rtOriginal_);
        }
      }
      setMoreThanOnePeak(mod,moreThanOnePeak);
      
      Hashtable<String,String> rts = new Hashtable<String,String>();
      if (allOriginalRts_.containsKey(mod))
        rts = allOriginalRts_.get(mod);
      for (String rt : other.allOriginalRts_.get(mod).values())
        rts.put(rt, rt);
      allOriginalRts_.put(mod, rts);
    }
  }

  public Integer getCharge(String modification){
    if (hasModification(modification)) return charge_.get(modification);
    else return null;
  }
  
  public Double getTheoreticalMass(String modification){
    if (hasModification(modification)) return mass_.get(modification);
    else return null;
  }
  
  public Double getExperimentalMass(String modification){
    if (hasModification(modification)) return expMass_.get(modification);
    else return null;
  }
  
  /**
   * checks if all of the modification names in the hash table are really present in this VO
   * @param mods the modification names stored in a hash
   * @return true if all modifications are present
   */
  public boolean containsAllModifications(Hashtable<String,String> mods){
    boolean all = true;
    for (String mod : mods.keySet()){
      if (!this.areas_.containsKey(mod)){
        all = false;
        break;
      }
    }
    return all;
  }

  
  /**
   * checks whether this retention time belongs to this ResultAreaVO (one rectangle in the heat map)
   * @param rt the retention time to check
   * @param mod the modification belonging to this retention time
   * @return true when this retention time belongs to this ResultAreaVO
   */
  public boolean belongsRtToThisAreaVO(String rt, String mod){
    if (allOriginalRts_.containsKey(mod) && allOriginalRts_.get(mod).containsKey(rt))
      return true;
    else
      return false;
  }
  
  /**
   * 
   * @return true when this belongs to an internal or an external standard
   */
  public boolean isAStandard(){
    return (internalStandard_ || this.externalStandard_);
  }
  
  
  /**
   * returns a list of the chain information sorted by the area (strongest one first)
   * @param areasInput the unsorted list of areas
   * @return a list of the chain information sorted by the area (strongest one first)
   */
  @SuppressWarnings("unchecked")
  private LinkedHashMap<String,Double> getAreaSortedLinkedHashMap(List<DoubleStringVO> areasInput) {
    List<DoubleStringVO> areas = new ArrayList<DoubleStringVO>(areasInput);
    Collections.sort(areas,new GeneralComparator("at.tugraz.genome.lda.vos.DoubleStringVO", "getValue", "java.lang.Double"));
    LinkedHashMap<String,Double> sorted = new LinkedHashMap<String,Double>();
    for (int i=(areas.size()-1); i!=-1; i--) {
      sorted.put(areas.get(i).getKey(), areas.get(i).getValue());
    }
    return sorted;
  }
  
  
  /**
   * 
   * @return the strongest chain identification
   */
  public String getStrongestChainIdentification() {
    if (chainInformationTotal_!=null&&chainInformationTotal_.size()>0)
      return this.chainInformationTotal_.keySet().iterator().next();
    else
      return null;
  }

  /**
   * 
   * @return whether MSn evidence is present
   */
  public boolean isMsnEvidenceThere()
  {
    return msnEvidence_;
  }

  /**
   * set whether MSn evidence is present
   * @param msnEvidence is MSn evidence present
   */
  public void setMsnEvidence(boolean msnEvidence)
  {
    this.msnEvidence_ = msnEvidence;
  }

  /**
   * 
   * @return sum chain information over all adducts
   */
  public LinkedHashMap<String,Double> getChainInformationTotal()
  {
    return chainInformationTotal_;
  }
  
}
