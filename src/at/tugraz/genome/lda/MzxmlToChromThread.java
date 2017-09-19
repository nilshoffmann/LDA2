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

package at.tugraz.genome.lda;

import java.io.File;

import at.tugraz.genome.maspectras.quantification.RawToChromatogramTranslator;

/**
 * 
 * @author Juergen Hartler
 *
 */
public class MzxmlToChromThread extends Thread
{
  String filePath_;
  boolean finished_ = false;
  String errorString_;
  
  public MzxmlToChromThread(String filePath){
    this.filePath_ = filePath;
  }
  
  public void run(){
    try{
      MzxmlToChromThread.translateToChrom(filePath_);
    } catch (Exception ex){
      ex.printStackTrace();
      errorString_ = ex.toString();     
    }
    finished_ = true;
  }
  
  public boolean finished(){
    return this.finished_;
  }
  
  public String getErrorString(){
    return this.errorString_;
  }
  
  public static void translateToChrom(String filePath) throws Exception{
      RawToChromatogramTranslator translator = new RawToChromatogramTranslator(filePath,"mzXML", MzxmlToChromThread.getPiecesForChromTranslation(filePath),
          LipidomicsConstants.getChromMultiplicationFactorForInt(),LipidomicsConstants.getChromLowestResolution(),LipidomicsConstants.isMS2());
      translator.translateToChromatograms();
  }
  
  private static int getPiecesForChromTranslation(String filePath){
    File file = new File (filePath);
    long maxFilePieceInByte = LipidomicsConstants.getmMaxFileSizeForChromTranslationAtOnceInMB()*1024*1024;
    int pieces = Integer.parseInt(Long.toString(file.length()/maxFilePieceInByte))+1;
    return pieces;
  }
  
}
