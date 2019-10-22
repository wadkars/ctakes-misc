/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.consumers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.ctakes.pipelines.CTakesFilePart;
import org.apache.ctakes.pipelines.CTakesResult;
import org.apache.ctakes.pipelines.RushFilesCollectionReader;
import org.apache.ctakes.pipelines.RushSimplePipeline;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.Utils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.cleartk.util.ViewUriUtil;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * Read cTAKES annotations from XMI files.
 * ExtractCuisSequences.java should probably be used instead of this.
 * @author dmitriy dligach
 */
import org.apache.uima.analysis_engine.impl.PrimitiveAnalysisEngine_impl;
public class RushExtractCuis {

  private String cuis;

  public static void main(String[] args) throws Exception {

   // Options options = CliFactory.parseArguments(Options.class, args);
    String inputDir = "/tmp/cTakesExample/cOut3/";
    String outputDir = "/tmp/cTakesExample/cOut2/";
    
    /*
    File inputDirectory = new File(inputDir);
    CollectionReader cuisReader = RushFilesCollectionReader.getCollectionReader(inputDir);
    for(File file : inputDirectory.listFiles()) {
    	String t = FileUtils.readFileToString(file);
    	CTakesFilePart part = new CTakesFilePart(file.getName(),1,t);
    	cuisReader.setConfigParameterValue("ctakesFilePart", part);
    }
    */
    //CollectionReader collectionReader = Utils.getCollectionReader(options.getInputDirectory());
    //AnalysisEngine annotationConsumer = AnalysisEngineFactory.createEngine(CuiPrinter.class, "OutputDir", options.getOutputDir());
    //CollectionReader reader = RushFilesCollectionReader.getCollectionReader(inputDir);
    
    
    CTakesResult result = new CTakesResult();
    String data = FileUtils.readFileToString(new File("/tmp/cTakesExample/cOut3/10380.txt"));
    //CollectionReader collectionReader = Utils.getCollectionReader(new File(inputDir));
    CollectionReader xmlCollectionReader = Utils.getCollectionReader(data);
    AnalysisEngine annotationConsumer = AnalysisEngineFactory.createEngine(CuisWriter.class);
    //((PrimitiveAnalysisEngine_impl)annotationConsumer).getAnalysisComponent();
    //annotationConsumer.setConfigParameterValue("result", result);
    String result2 = RushSimplePipeline.runPipeline(xmlCollectionReader, annotationConsumer);
    System.err.println("yyy");
    System.err.println(result2);
  }
  
  /**
   * Print events and entities.
   */
  public static class CuiWriter extends JCasAnnotator_ImplBase {
	  /*
    @ConfigurationParameter(
        name = "result",
        mandatory = false,
        description = "result of cuis")
        */
    private String result;

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
    
      JCas systemView;
      try {
        systemView = jCas.getView("_InitialView");
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      List<String> cuis = new ArrayList<>();
      for (EventMention eventMention : JCasUtil.select(systemView, EventMention.class)) {
        // String text = eventMention.getCoveredText().toLowerCase().replaceAll(" ", "_");
        // String semanticType = eventMention.getClass().getSimpleName();
        // int polarity = eventMention.getPolarity();
        for(String code : getOntologyConceptCodes(eventMention)) {
          // String output = String.format("%s|%s|%s", code, text, semanticType);
          String output = String.format("%s", code);
          cuis.add(output);
        }
      }

      for (EntityMention entityMention : JCasUtil.select(systemView, EntityMention.class)) {
        // String text = entityMention.getCoveredText().toLowerCase().replaceAll(" ", "_");
        // String semanticType = entityMention.getClass().getSimpleName();
        // int polarity = entityMention.getPolarity();
        for(String code : getOntologyConceptCodes(entityMention)) {
          // String output = String.format("%s|%s|%s", code, text, semanticType);
          String output = String.format("%s", code);
          cuis.add(output);
        }
      }

      File noteFile = new File(ViewUriUtil.getURI(jCas).toString());
      String fileName = noteFile.getName();
      String outputString = String.join(" ", cuis);
      //result.setCuis(outputString);
      result = outputString;
      //
      try {
		jCas.createView("result").setDocumentText(outputString);
		//System.err.println(outputString);
	} catch (CASRuntimeException | CASException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      //jCas.setDocumentText(outputString);
      //jCas.setSofaDataString(outputString, null);
    

    }

    /**
     * Get the CUIs, RxNorm codes, etc.
     */
    public static Set<String> getOntologyConceptCodes(IdentifiedAnnotation identifiedAnnotation) {

      Set<String> codes = new HashSet<String>();

      FSArray fsArray = identifiedAnnotation.getOntologyConceptArr();
      if(fsArray == null) {
        return codes;
      }

      for(FeatureStructure featureStructure : fsArray.toArray()) {
        OntologyConcept ontologyConcept = (OntologyConcept) featureStructure;

        if(ontologyConcept instanceof UmlsConcept) {
          UmlsConcept umlsConcept = (UmlsConcept) ontologyConcept;
          String code = umlsConcept.getCui();
          codes.add(code);
        } else { // SNOMED or RxNorm
          String code = ontologyConcept.getCodingScheme() + ontologyConcept.getCode();
          codes.add(code);
        }
      }

      return codes;
    }
  }
}
