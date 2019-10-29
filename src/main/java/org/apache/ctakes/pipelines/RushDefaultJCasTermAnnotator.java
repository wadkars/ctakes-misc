package org.apache.ctakes.pipelines;
/*
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


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.core.util.collection.HashSetMap;
import org.apache.ctakes.dictionary.lookup2.ae.AbstractJCasTermAnnotator;
import org.apache.ctakes.dictionary.lookup2.concept.Concept;
import org.apache.ctakes.dictionary.lookup2.concept.ConceptFactory;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
import org.apache.ctakes.dictionary.lookup2.textspan.DefaultTextSpan;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.ctakes.dictionary.lookup2.util.FastLookupToken;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * A direct string match using phrase permutations
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/19/13
 */
@PipeBitInfo(
      name = "Dictionary Lookup (Default)",
      description = "Annotates clinically-relevant terms.  Terms must match dictionary entries exactly.",
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.BASE_TOKEN },
      products = PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION
)
final public class RushDefaultJCasTermAnnotator extends AbstractJCasTermAnnotator {
	 // LOG4J logger based on interface name
	   final static private Logger LOGGER = Logger.getLogger( "RushDefaultJCasTermAnnotator" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void findTerms( final RareWordDictionary dictionary,
                          final List<FastLookupToken> allTokens,
                          final List<Integer> lookupTokenIndices,
                          final CollectionMap<TextSpan, Long, ? extends Collection<Long>> termsFromDictionary ) {
      Collection<RareWordTerm> rareWordHits;
      for ( Integer lookupTokenIndex : lookupTokenIndices ) {
         final FastLookupToken lookupToken = allTokens.get( lookupTokenIndex );
         rareWordHits = dictionary.getRareWordHits( lookupToken );
         if ( rareWordHits == null || rareWordHits.isEmpty() ) {
            continue;
         }
         for ( RareWordTerm rareWordHit : rareWordHits ) {
            if ( rareWordHit.getText().length() < _minimumLookupSpan ) {
               continue;
            }
            if ( rareWordHit.getTokenCount() == 1 ) {
               // Single word term, add and move on
               termsFromDictionary.placeValue( lookupToken.getTextSpan(), rareWordHit.getCuiCode() );
               continue;
            }
            final int termStartIndex = lookupTokenIndex - rareWordHit.getRareWordIndex();
            if ( termStartIndex < 0 || termStartIndex + rareWordHit.getTokenCount() > allTokens.size() ) {
               // term will extend beyond window
               continue;
            }
            final int termEndIndex = termStartIndex + rareWordHit.getTokenCount() - 1;
            if ( isTermMatch( rareWordHit, allTokens, termStartIndex, termEndIndex ) ) {
               final int spanStart = allTokens.get( termStartIndex ).getStart();
               final int spanEnd = allTokens.get( termEndIndex ).getEnd();
               termsFromDictionary.placeValue( new DefaultTextSpan( spanStart, spanEnd ), rareWordHit.getCuiCode() );
            }
         }
      }

	  //RushUmlsJdbcRareWordDictionary dictionary2 = (RushUmlsJdbcRareWordDictionary) dictionary;
      //dictionary2.close();
      LOGGER.info("Done findTerms");

   }

   /**
    * Hopefully the jit will inline this method
    *
    * @param rareWordHit    rare word term to check for match
    * @param allTokens      all tokens in a window
    * @param termStartIndex index of first token in allTokens to check
    * @param termEndIndex   index of last token in allTokens to check
    * @return true if the rare word term exists in allTokens within the given indices
    */
   public static boolean isTermMatch( final RareWordTerm rareWordHit, final List<FastLookupToken> allTokens,
                                      final int termStartIndex, final int termEndIndex ) {
      final String[] hitTokens = rareWordHit.getTokens();
      int hit = 0;
      for ( int i = termStartIndex; i < termEndIndex + 1; i++ ) {
         if ( hitTokens[ hit ].equals( allTokens.get( i ).getText() )
              || hitTokens[ hit ].equals( allTokens.get( i ).getVariant() ) ) {
            // the normal token or variant matched, move to the next token
            hit++;
            continue;
         }
         // the token normal didn't match and there is no matching variant
         return false;
      }
      // some combination of token and variant matched
      return true;
   }


   static public AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( RushDefaultJCasTermAnnotator.class );
   }

   static public AnalysisEngineDescription createAnnotatorDescription( final String descriptorPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( RushDefaultJCasTermAnnotator.class,
            DICTIONARY_DESCRIPTOR_KEY, descriptorPath );
   }
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
	   LOGGER.info( "Starting Rush processing" );
      super.process(jcas);
      Collection<RareWordDictionary> dicts = getDictionaries();
      for(RareWordDictionary rwd:dicts) {
    	  if(rwd instanceof RushJDBCRareWordDictionary) {
    		  LOGGER.info( "Closing Connections" );
    		  RushJDBCRareWordDictionary r = (RushJDBCRareWordDictionary) rwd;
    		  r.close();
    	  }
    	
    	  
      }
      LOGGER.info( "Finished Rush processing" );
      
	 
     
      
   }
}
