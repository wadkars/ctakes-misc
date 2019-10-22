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
package org.apache.ctakes.word2vec;

import java.io.File;
import java.util.HashSet;
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
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * Read cTAKES annotations from XMI files.
 *  
 * @author dmitriy dligach
 */
public class CuisForWord2Vec {

  static interface Options {

    @Option(
        longName = "xmi-dir",
        description = "path to xmi files")
    public File getInputDirectory();
  }

  public static void main(String[] args) throws Exception {

    Options options = CliFactory.parseArguments(Options.class, args);
    CollectionReader collectionReader = Utils.getCollectionReader(options.getInputDirectory());
    AnalysisEngine annotationConsumer = AnalysisEngineFactory.createEngine(CuiPrinter.class);
    SimplePipeline.runPipeline(collectionReader, annotationConsumer);
  }

  /**
   * Print CUIs
   *  
   * @author dmitriy dligach
   */
  public static class CuiPrinter extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

      JCas systemView;
      try {
        systemView = jCas.getView("_InitialView");
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      for(IdentifiedAnnotation annotation : JCasUtil.select(systemView, IdentifiedAnnotation.class)) {
        Set<String> cuis = getOntologyConceptCodes(annotation);
        for(String cui : cuis) {
          System.out.print(cui + " ");
        }
      }
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

