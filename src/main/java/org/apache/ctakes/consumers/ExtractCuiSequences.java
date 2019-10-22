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
import java.util.Set;

import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.Utils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
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
 * Read cTAKES annotations from XMI files and write CUI sequences to files.
 * This needs to be confirmed, but most likely this class prints
 * CUIs in the same order in which they occur in the text.
 * TODO: Think about the 'ascending colon cancer' scenario.
 * TODO: Do we output the same CUIs twice for multiple semantic types?
 * TODO: Currently returning a set of CUIs for a concept. How does it affect word2vec?
 */
public class ExtractCuiSequences {

  static interface Options {

    @Option(
        longName = "xmi-dir",
        description = "path to xmi files")
    public File getInputDirectory();

    @Option(
        longName = "output-dir",
        description = "path to feature files")
    public String getOutputDir();
  }

  public static void main(String[] args) throws Exception {

    Options options = CliFactory.parseArguments(Options.class, args);
    CollectionReader collectionReader = Utils.getCollectionReader(options.getInputDirectory());
    AnalysisEngine annotationConsumer = AnalysisEngineFactory.createEngine(CuiPrinter.class, "OutputDir", options.getOutputDir());
    SimplePipeline.runPipeline(collectionReader, annotationConsumer);
  }

  /**
   * Print events and entities.
   */
  public static class CuiPrinter extends JCasAnnotator_ImplBase {

    @ConfigurationParameter(
        name = "OutputDir",
        mandatory = true,
        description = "path to the file that stores relation data")
    private String outputDir;

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

      JCas systemView;
      try {
        systemView = jCas.getView("_InitialView");
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      List<String> cuis = new ArrayList<>();

      int currentOffset = 0;

      // extract the CUIs as a sequence-preserving list
      for(IdentifiedAnnotation identifiedAnnotation : JCasUtil.select(systemView, IdentifiedAnnotation.class)) {

        if(identifiedAnnotation.getBegin() < currentOffset) {
          System.out.println("annotation out of order:" + identifiedAnnotation);
        }
        currentOffset = identifiedAnnotation.getBegin();

        int polarity = identifiedAnnotation.getPolarity();
        for(String code : getOntologyConceptCodes(identifiedAnnotation)) {
          // polarity -1 for negated concepts and 0 for others
          String output = String.format("%s%s", (polarity == 0) ? "" : "n" , code);
          cuis.add(output);
        }
      }
      
      // turn cuis into a string and write to file
      File noteFile = new File(ViewUriUtil.getURI(jCas).toString());
      String fileName = noteFile.getName();
      String outputString = String.join(" ", cuis);
      try {
        Files.write(Paths.get(outputDir + fileName), outputString.getBytes());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    /**
     * Get the CUIs, RxNorm codes, etc.
     * Returns an empty set if no codes found for this annotation.
     */
    public static Set<String> getOntologyConceptCodes(IdentifiedAnnotation identifiedAnnotation) {

      Set<String> codes = new HashSet<String>();

      FSArray fsArray = identifiedAnnotation.getOntologyConceptArr();
      if(fsArray == null) {
        // not a umls entity, e.g. fraction annotation
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
