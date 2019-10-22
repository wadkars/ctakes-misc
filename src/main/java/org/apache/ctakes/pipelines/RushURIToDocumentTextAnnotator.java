package org.apache.ctakes.pipelines;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.ViewUriUtil;

import com.google.common.io.CharStreams;
import com.mysql.jdbc.StringUtils;

/**
 * <br>
 * Copyright (c) 2012, Regents of the University of Colorado <br>
 * All rights reserved.
 * <p>
 * 
 * This Analysis Engine reads in the contents of the URI in the default sofa, and puts it in the CAS
 * 
 * @author Lee Becker
 * 
 */
public class RushURIToDocumentTextAnnotator extends JCasAnnotator_ImplBase {

  public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(RushURIToDocumentTextAnnotator.class);
  }

  /**
   * This description will read the contents into the specify view. If the view does not exist, it
   * will make it as needed.
   */
  public static AnalysisEngineDescription getDescriptionForView(String targetViewName)
      throws ResourceInitializationException {
    AggregateBuilder builder = new AggregateBuilder();
    builder.add(AnalysisEngineFactory.createEngineDescription(
        ViewCreatorAnnotator.class,
        ViewCreatorAnnotator.PARAM_VIEW_NAME,
        targetViewName));
    builder.add(RushURIToDocumentTextAnnotator.getDescription(), CAS.NAME_DEFAULT_SOFA, targetViewName);
    return builder.createAggregateDescription();
  }

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {

    URI uri = ViewUriUtil.getURI(jCas);
    String content;

    try {
      content = CharStreams.toString(new InputStreamReader(uri.toURL().openStream()));
      if(StringUtils.isNullOrEmpty(jCas.getSofaDataString())){
    	  jCas.setSofaDataString(content, "text/plain");
      }
      
    } catch (MalformedURLException e) {
      throw new AnalysisEngineProcessException(e);
    } catch (IOException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }
}
