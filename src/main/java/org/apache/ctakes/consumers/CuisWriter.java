package org.apache.ctakes.consumers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.cleartk.util.ViewUriUtil;

public  class CuisWriter extends JCasAnnotator_ImplBase {


    private String result="";

    public String getResult() {
		return result;
	}

	@Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
    
      JCas systemView;
      try {

        systemView = jCas.getView("_InitialView");

        
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      List<String> cuis = new ArrayList<>();
      for (EventMention eventMention : JCasUtil.select(jCas, EventMention.class)) {
         String text = eventMention.getCoveredText().toLowerCase().replaceAll(" ", "_");
         String semanticType = eventMention.getClass().getSimpleName();
         int polarity = eventMention.getPolarity();
        for(String code : getOntologyConceptCodes(eventMention)) {
          // String output = String.format("%s|%s|%s", code, text, semanticType);
          String output = String.format("%s", code);
         
          if(polarity==-1) {
        	  output = "N"+output;
          }
          //System.out.println(text + "=" + output);
          //System.out.println(semanticType + "=" + output);
          cuis.add(output);
        }
      }
/*
      for (EntityMention entityMention : JCasUtil.select(systemView, EntityMention.class)) {
        // String text = entityMention.getCoveredText().toLowerCase().replaceAll(" ", "_");
        // String semanticType = entityMention.getClass().getSimpleName();
         int polarity = entityMention.getPolarity();
        for(String code : getOntologyConceptCodes(entityMention)) {
          // String output = String.format("%s|%s|%s", code, text, semanticType);
          String output = String.format("%s", code);
          if(polarity==-1) {
        	  output = "N"+output;
          }
          cuis.add(output);
        }
      }
*/
      File noteFile = new File(ViewUriUtil.getURI(jCas).toString());
      String fileName = noteFile.getName();
      String outputString = String.join(" ", cuis);
      //result.setCuis(outputString);
      //result = outputString;
      //jCas.setDocumentText(outputString);
      //System.err.println("-----");
      //System.err.println(outputString);
      try {
		jCas.createView("RESULT_VIEW").setDocumentText(outputString);
	} catch (CASException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      this.result = outputString;
    }

    /**
     * Get the CUIs, RxNorm codes, etc.
     */
    public  Set<String> getOntologyConceptCodes(IdentifiedAnnotation identifiedAnnotation) {

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