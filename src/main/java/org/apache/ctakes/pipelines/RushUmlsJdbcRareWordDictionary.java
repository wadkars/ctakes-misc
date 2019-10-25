package org.apache.ctakes.pipelines;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Properties;

import org.apache.ctakes.dictionary.lookup2.dictionary.JdbcRareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
import org.apache.ctakes.dictionary.lookup2.util.FastLookupToken;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
public class RushUmlsJdbcRareWordDictionary implements RareWordDictionary{
	   static private final Logger LOGGER = Logger.getLogger( "RushUmlsJdbcRareWordDictionary" );

	   final private RareWordDictionary _delegateDictionary;


	   public RushUmlsJdbcRareWordDictionary( final String name, final UimaContext uimaContext, final Properties properties )
	         throws SQLException {
	      /*
		   final boolean isValidUser = UmlsUserApprover.getInstance().isValidUMLSUser( uimaContext, properties );
	      if ( !isValidUser ) {
	         throw new SQLException( "Invalid User for UMLS dictionary " + name );
	      }
	      */
	      _delegateDictionary = new RushJDBCRareWordDictionary( name, uimaContext, properties );
	   }


	   /**
	    * {@inheritDoc}
	    */
	   @Override
	   public String getName() {
	      return _delegateDictionary.getName();
	   }

	   /**
	    * {@inheritDoc}
	    */
	   @Override
	   public Collection<RareWordTerm> getRareWordHits( final FastLookupToken fastLookupToken ) {
	      Collection<RareWordTerm> ret = _delegateDictionary.getRareWordHits( fastLookupToken );
	      return ret;
	   }

	   /**
	    * {@inheritDoc}
	    */
	   @Override
	   public Collection<RareWordTerm> getRareWordHits( final String rareWordText ) {
		   Collection<RareWordTerm> ret = _delegateDictionary.getRareWordHits( rareWordText );
		   return ret;
	   }

	   public void close() {
		  ((RushJDBCRareWordDictionary) _delegateDictionary).close();
	   }
}
