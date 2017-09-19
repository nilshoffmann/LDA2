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

package at.tugraz.genome.lda.swing;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

import at.tugraz.genome.lda.TooltipTexts;
import at.tugraz.genome.lda.WarningMessage;
import at.tugraz.genome.lda.utils.StaticUtils;
import at.tugraz.genome.lda.vos.ResultCompVO;
import at.tugraz.genome.lda.vos.ResultDisplaySettingsVO;

/**
 * 
 * @author Juergen Hartler
 *
 */
public class ResultDisplaySettings extends JDialog implements ActionListener
{
  private static final long serialVersionUID = -3256022094343148530L;
  
  protected static final String CHANGE_IS_STATUS = "changeIStatus";
  protected static final String CHANGE_ES_STATUS = "changeESStatus";
  protected static final String CHANGE_DIL_STATUS = "changeDilutionStatus";
  
  private JComboBox<String> displayType_;
  protected JCheckBox considerInternalStandards_;
  private JComboBox<String> isType_;
  protected JCheckBox considerExternalStandards_;
  private JComboBox<String> esType_;
  protected JCheckBox considerDilution_;
  protected JComboBox<String> unitMagnitude_;
  protected JCheckBox useAU_;
  
  protected JLabel isLabel_;
  protected JLabel esLabel_;
  protected JLabel dilLabel_;
  protected JLabel unitLabel_;
  protected JLabel useAULabel_;

  protected Hashtable<String,Integer> isLookup_;
  protected Hashtable<String,Integer> esLookup_;
  
  protected boolean isAvailability_; 
  protected boolean esAvailability_; 
  protected boolean absoluteSettings_;
  protected boolean hasSampleWeight_;
  protected boolean hasProtein_;
  protected boolean hasNeutralLipid_;
  protected Vector<ActionListener> parents_;
  

  public ResultDisplaySettings(boolean isAvailability, boolean esAvailability, Hashtable<String,Integer>isLookup,Hashtable<String,Integer>esLookup,
      boolean absoluteSettings, boolean hasSampleWeight, boolean hasProtein,
      boolean hasNeutralLipid/*, ActionListener parent*/){
    this.setLayout(new GridBagLayout());
    this.isAvailability_ = isAvailability;
    this.esAvailability_ = esAvailability;
    this.isLookup_ = isLookup;
    this.esLookup_ = esLookup;
    this.absoluteSettings_ = absoluteSettings;
    hasSampleWeight_ = hasSampleWeight;
    this.hasProtein_ = hasProtein;
    this.hasNeutralLipid_ = hasNeutralLipid;
    parents_ = new Vector<ActionListener>();
////    parent_ = parent;
    
    setLocation(380,240);
    JLabel label = new JLabel("value type: ");
    label.setToolTipText(TooltipTexts.HEATMAP_VALUE_TYPE);
    this.add(label,new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
        ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));   
    displayType_ = new JComboBox<String>();
    if (this.absoluteSettings_){
      if (this.isAvailability_){ 
        this.addAllDisplayTypes();
      } else if (this.esAvailability_){
        this.addWithoutEndVolume();
      } else {
        this.addJustRelative();
      }
      if (hasSampleWeight_)
        displayType_.addItem("relative to sample weight");
      if (hasProtein_)
        displayType_.addItem("relation to protein content");
      if (hasNeutralLipid_)
        displayType_.addItem("relation to neutral lipid content");
    }else{
      this.addJustRelative();
    }
    displayType_.addItemListener(new SelectionItemListener());
    displayType_.setSelectedItem("Relative Value");
    displayType_.setToolTipText(TooltipTexts.HEATMAP_VALUE_TYPE);
    this.add(displayType_,new GridBagConstraints(1, 0, 2, 1, 0.0, 0.0
        ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
    if (this.isAvailability_){
      isLabel_ = new JLabel("internal standard correction: ");
      isLabel_.setToolTipText(TooltipTexts.HEATMAP_IS_CORRECTION);
      this.add(isLabel_,new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
          ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
      considerInternalStandards_  = new JCheckBox();
      considerInternalStandards_.setActionCommand(CHANGE_IS_STATUS);
      considerInternalStandards_.addActionListener(this);
      considerInternalStandards_.setSelected(true);
      considerInternalStandards_.setToolTipText(TooltipTexts.HEATMAP_IS_CORRECTION);
      this.add(considerInternalStandards_,new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
        ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
      isType_ = new JComboBox<String>();
      isType_.addItem("most reliable standard");
      isType_.addItem("median");
      for (String isName : isLookup_.keySet())
        isType_.addItem(isName);
      isType_.setToolTipText(TooltipTexts.HEATMAP_STANDARD_CORRECTION_TYPE);
      this.add(isType_,new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
          ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
    }
    if (this.esAvailability_){
      esLabel_ = new JLabel("external standard correction: ");
      esLabel_.setToolTipText(TooltipTexts.HEATMAP_ES_CORRECTION);
      this.add(esLabel_,new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
        ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
      considerExternalStandards_  = new JCheckBox();
      considerExternalStandards_.setActionCommand(CHANGE_ES_STATUS);
      considerExternalStandards_.addActionListener(this);
      considerExternalStandards_.setSelected(true);
      considerExternalStandards_.setToolTipText(TooltipTexts.HEATMAP_ES_CORRECTION);
      this.add(considerExternalStandards_,new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
          ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
      esType_ = new JComboBox<String>();
      esType_.addItem("most reliable standard");
      esType_.addItem("median");
      for (String esName : esLookup_.keySet())
        esType_.addItem(esName);
      esType_.setToolTipText(TooltipTexts.HEATMAP_STANDARD_CORRECTION_TYPE);
      this.add(esType_,new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
          ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));

    }
    if (this.absoluteSettings_){
      dilLabel_ = new JLabel("consider dilution: ");
      dilLabel_.setToolTipText(TooltipTexts.HEATMAP_CONSIDER_DILUTION);
      this.add(dilLabel_,new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
        ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
      considerDilution_  = new JCheckBox();
      considerDilution_.setSelected(true);
      considerDilution_.setActionCommand(CHANGE_DIL_STATUS);
      considerDilution_.addActionListener(this);
      considerDilution_.setToolTipText(TooltipTexts.HEATMAP_CONSIDER_DILUTION);
      this.add(considerDilution_,new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0
        ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
      unitLabel_ = new JLabel("divisor unit: ");
      unitLabel_.setToolTipText(TooltipTexts.HEATMAP_DIVISOR_UNIT);
      unitLabel_.setEnabled(false);
      this.add(unitLabel_,new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
        ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
      unitMagnitude_ = new JComboBox<String>(StaticUtils.physicalMagnitudes_);
      unitMagnitude_.setPreferredSize(new Dimension(42,18));
      unitMagnitude_.setToolTipText(TooltipTexts.HEATMAP_DIVISOR_UNIT);
      unitMagnitude_.setSelectedItem("m");
      unitMagnitude_.setEnabled(false);
      this.add(unitMagnitude_,new GridBagConstraints(1, 4, 2, 1, 0.0, 0.0
          ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
      if (this.isAvailability_ || this.esAvailability_){
        useAULabel_ = new JLabel("use AU: ");
        useAULabel_.setEnabled(false);
        useAULabel_.setToolTipText(TooltipTexts.HEATMAP_USE_AU);
        this.add(useAULabel_,new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
        useAU_  = new JCheckBox();
        useAU_.setSelected(true);
        useAU_.setEnabled(false);
        useAU_.setToolTipText(TooltipTexts.HEATMAP_USE_AU);
        this.add(useAU_,new GridBagConstraints(1, 5, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
      }
    }
    JButton button = new JButton("OK");
    button.setActionCommand("AcceptDisplaySettings");
    button.setToolTipText(TooltipTexts.ACCEPT_GENERAL);
    this.add(button,new GridBagConstraints(0, 6, 3, 1, 0.0, 0.0
        ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
    button.addActionListener(this);
    setVisible(false);
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    pack(); 
  }
  
  
  
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equalsIgnoreCase(CHANGE_IS_STATUS)){
      if (considerInternalStandards_.isSelected())
        this.isType_.setEnabled(true);
      else
        this.isType_.setEnabled(false);
      checkAUEnable();
    }else if (e.getActionCommand().equalsIgnoreCase(CHANGE_ES_STATUS)){
      if (considerExternalStandards_.isSelected()){
        this.esType_.setEnabled(true);
      }else{
        this.esType_.setEnabled(false);
      }
      checkAUEnable();
    }else if (e.getActionCommand().equalsIgnoreCase("AcceptDisplaySettings")){
      String type = (String)displayType_.getSelectedItem();
      boolean acceptSettings = true;
      if (type.equalsIgnoreCase("amount sample-volume") || type.equalsIgnoreCase("conc. sample-volume") || type.equalsIgnoreCase("weight sample-volume") ||
          type.equalsIgnoreCase("relation to measured neutral lipid")){
        if (considerInternalStandards_!=null && considerExternalStandards_!=null){
          if (!considerInternalStandards_.isSelected()&&!considerExternalStandards_.isSelected()){
            new WarningMessage(new JFrame(),"Warning","For the display of \""+type+"\" the internal- or external standard has to be selected!");
            acceptSettings = false;
          }
        } else if (considerInternalStandards_!=null){
          if (!considerInternalStandards_.isSelected()){
            new WarningMessage(new JFrame(),"Warning","For the display of \""+type+"\" the internal standard has to be selected!");
            acceptSettings = false;            
          }
        } else if (considerExternalStandards_!=null){
          if (!considerExternalStandards_.isSelected()){
            new WarningMessage(new JFrame(),"Warning","For the display of \""+type+"\" the external standard has to be selected!");
            acceptSettings = false;            
          }
        } 
      }
      if (acceptSettings){
        setVisible(false);
        for (ActionListener parent : parents_)parent.actionPerformed(e);
      }
    }
  }
  
  private void addJustRelative(){
    displayType_.removeAllItems();
    displayType_.addItem("relative value");
    displayType_.addItem("relative to base peak");
    displayType_.addItem("relative to measured class amount");
    displayType_.addItem("relative to highest total peak");
    displayType_.addItem("relative to total amount");
  }
  
  private void addAllDisplayTypes(){
    addJustRelative();
    displayType_.addItem("amount end-volume");
    displayType_.addItem("conc. end-volume");
    displayType_.addItem("weight end-volume");
    addProbeVolumeSettings();
  }
  
  private void addWithoutEndVolume(){
    addJustRelative();
    addProbeVolumeSettings();    
  }
  
  private void addProbeVolumeSettings(){
    displayType_.addItem("amount sample-volume");
    displayType_.addItem("conc. sample-volume");
    displayType_.addItem("weight sample-volume");
    displayType_.addItem("relation to measured neutral lipid");
  }
  
  private class SelectionItemListener implements java.awt.event.ItemListener
  {

    public void itemStateChanged(ItemEvent e)  {
      if (e.getStateChange()==ItemEvent.SELECTED){
        if (((String)displayType_.getSelectedItem()).equalsIgnoreCase("relative value")){
          enableSettings();
          enableMagnitudeSetting(false);
          disableAUSettings(true);
        } else if (((String)displayType_.getSelectedItem()).equalsIgnoreCase("relative to base peak")||
            ((String)displayType_.getSelectedItem()).equalsIgnoreCase("relative to measured class amount")||
            ((String)displayType_.getSelectedItem()).equalsIgnoreCase("relative to highest total peak")||
            ((String)displayType_.getSelectedItem()).equalsIgnoreCase("relative to total amount")){
          disableSettings();         
        }else if (((String)displayType_.getSelectedItem()).equalsIgnoreCase("amount end-volume")||
            ((String)displayType_.getSelectedItem()).equalsIgnoreCase("conc. end-volume") ||
            ((String)displayType_.getSelectedItem()).equalsIgnoreCase("weight end-volume")){
          enableIntStandSetting(true);
          if (considerInternalStandards_!=null)
            considerInternalStandards_.setSelected(true);
          enableIntStandSetting(false);
          isType_.setEnabled(true);
          enableExtStandSetting(true);
          if (considerExternalStandards_!=null)
            considerExternalStandards_.setSelected(false);
          enableExtStandSetting(false);        
          enableDilutionSetting(true);
          considerDilution_.setSelected(false);
          enableDilutionSetting(false);
          if (((String)displayType_.getSelectedItem()).equalsIgnoreCase("conc. end-volume"))
            enableMagnitudeSetting(true);
          else
            enableMagnitudeSetting(false);
          disableAUSettings(false);
        }else if (((String)displayType_.getSelectedItem()).equalsIgnoreCase("amount sample-volume") ||
            ((String)displayType_.getSelectedItem()).equalsIgnoreCase("conc. sample-volume") ||
            ((String)displayType_.getSelectedItem()).equalsIgnoreCase("weight sample-volume")){
          enableProbeSettings();
          disableAUSettings(false);
          if (((String)displayType_.getSelectedItem()).equalsIgnoreCase("conc. sample-volume"))
            enableMagnitudeSetting(true);
          else
            enableMagnitudeSetting(false);
        }else if (((String)displayType_.getSelectedItem()).equalsIgnoreCase("relative to sample weight")){
          enableProbeSettings();
          enableMagnitudeSetting(true);
          disableAUSettings(false);
        }else if (((String)displayType_.getSelectedItem()).equalsIgnoreCase("relation to protein content")){
          enableProbeSettings();
          enableMagnitudeSetting(true);
          checkAUEnable();
        }else if (((String)displayType_.getSelectedItem()).equalsIgnoreCase("relation to neutral lipid content")){
          enableProbeSettings();
          enableMagnitudeSetting(true);
          checkAUEnable();
        }else if (((String)displayType_.getSelectedItem()).equalsIgnoreCase("relation to measured neutral lipid")){
          enableProbeSettings();
          enableMagnitudeSetting(true);
          disableAUSettings(false);
//          checkAUEnable();
        }
      }
    }
  }
  
  private void enableProbeSettings(){
    enableSettings();
    enableDilutionSetting(true);
    considerDilution_.setSelected(true);
    enableDilutionSetting(false); 
  }
  
  private void enableStandSettings(){
    enableIntStandSetting(true);
    enableExtStandSetting(true);    
  }
  
  private void enableSettings(){
    enableStandSettings();
    enableDilutionSetting(true);
  }
  
  private void disableSettings(){
    if (considerInternalStandards_!=null)
      considerInternalStandards_.setSelected(false);
    if (considerExternalStandards_!=null)
      considerExternalStandards_.setSelected(false);
    if (considerDilution_!=null)
      considerDilution_.setSelected(false);
    enableIntStandSetting(false);
    enableExtStandSetting(false);
    enableDilutionSetting(false);
    enableMagnitudeSetting(false);
    disableAUSettings(false);
  }
  
  private void enableIntStandSetting(boolean enable){
    if (considerInternalStandards_!=null){
      isLabel_.setEnabled(enable);
      considerInternalStandards_.setEnabled(enable);
      if (considerInternalStandards_.isSelected())
        isType_.setEnabled(enable);
      else
        isType_.setEnabled(false);
    }
  }
  
  private void enableExtStandSetting(boolean enable){
    if (considerExternalStandards_!=null){
      esLabel_.setEnabled(enable);
      considerExternalStandards_.setEnabled(enable);
      esType_.setEnabled(enable);
      if (considerExternalStandards_.isSelected())
        esType_.setEnabled(enable);
      else
        esType_.setEnabled(false);
    }
  }
  
  private void enableDilutionSetting(boolean enable){
    if (considerDilution_!=null){
      dilLabel_.setEnabled(enable);
      considerDilution_.setEnabled(enable);        
    }
  }
  
  private void enableMagnitudeSetting(boolean enable){
    if (unitMagnitude_!=null){
      unitLabel_.setEnabled(enable);
      unitMagnitude_.setEnabled(enable);
    }
  }
  
  private void checkAUEnable(){
    if (useAU_ != null){
      String displayType = (String)displayType_.getSelectedItem(); 
      if ((displayType.equalsIgnoreCase("relation to protein content") || 
          displayType.equalsIgnoreCase("relation to neutral lipid content"))){ //||
          //displayType.equalsIgnoreCase("relation to measured neutral lipid"))){
        if ((considerInternalStandards_!=null && considerInternalStandards_.isSelected()) || 
          (considerExternalStandards_!=null && considerExternalStandards_.isSelected())){      
          useAU_.setEnabled(true);
          useAULabel_.setEnabled(true);
        } else {
          useAU_.setSelected(true);
          useAU_.setEnabled(false);
          useAULabel_.setEnabled(false);
        }
      }else{
        useAU_.setEnabled(false);
        useAULabel_.setEnabled(false);
      }
    }
  }
  
  private void disableAUSettings(boolean selected){
    if (useAU_ != null){
      useAU_.setSelected(selected);
      useAU_.setEnabled(false);
      useAULabel_.setEnabled(false);
    }
  }
  
  public ResultDisplaySettingsVO getSettingsVO(){
    int useIs = ResultCompVO.NO_STANDARD_CORRECTION;
    int useEs = ResultCompVO.NO_STANDARD_CORRECTION;
    boolean useDilution = false;
    boolean useAU = false;
    if (considerInternalStandards_!=null && considerInternalStandards_.isSelected())
      if (((String)isType_.getSelectedItem()).equalsIgnoreCase("most reliable standard")){
        useIs = ResultCompVO.STANDARD_CORRECTION_INTERNAL;
      }else if (((String)isType_.getSelectedItem()).equalsIgnoreCase("median")){
        useIs = ResultCompVO.STANDARD_CORRECTION_MEDIAN;
      }else
        useIs = isLookup_.get((String)isType_.getSelectedItem());
    if (considerExternalStandards_!=null && considerExternalStandards_.isSelected()){
      if (((String)esType_.getSelectedItem()).equalsIgnoreCase("most reliable standard")){
        useEs = ResultCompVO.STANDARD_CORRECTION_INTERNAL;
      }else if (((String)esType_.getSelectedItem()).equalsIgnoreCase("median")){
        useEs = ResultCompVO.STANDARD_CORRECTION_MEDIAN;
      }else
        useEs = esLookup_.get((String)esType_.getSelectedItem());
    }  
    if (considerDilution_!=null && considerDilution_.isSelected())
      useDilution = true;
    if (useAU_!=null && useAU_.isSelected())
      useAU = true;
    //here I have to add a new input field for the weight magnifier
    String unitMagnitude = "";
    if (unitMagnitude_!=null && unitMagnitude_.getSelectedItem()!=null)
      unitMagnitude = (String)unitMagnitude_.getSelectedItem();
    return new ResultDisplaySettingsVO((String)displayType_.getSelectedItem(),useIs,useEs,
        useDilution,useAU,unitMagnitude);
  }
  
  public void addActionListener(ActionListener parent){
    this.parents_.add(parent);
  }
}
