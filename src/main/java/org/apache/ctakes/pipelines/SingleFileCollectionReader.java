package org.apache.ctakes.pipelines;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.ctakes.typesystem.type.structured.DocumentID;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

/**
 * Component to plug into cTAKES for processing Tuples from Pig.
 * 
 * @author Paul Codding - paul@hortonworks.com
 * 
 */
public class SingleFileCollectionReader extends JCasCollectionReader_ImplBase {
	CTakesFilePart filePart = null;

	private  boolean processed = false;
	private int numProcessed = 0;

	public  void getNext(JCas jCas) throws IOException, CollectionException{
	
		try {
			
			//jcas = aCas.getJCas();
			DocumentID documentIDAnnotation = new DocumentID(jCas);
			String docID = this.filePart.getFileName() + "-" + this.filePart.getPart();
			documentIDAnnotation.setDocumentID(docID);
			documentIDAnnotation.addToIndexes();

			String text = this.filePart.getInput();
			jCas.setDocumentText(text);
		}  finally {
			numProcessed++;
			processed = true;
		}
	}

	public void close() throws IOException {
	}

	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(numProcessed,
				1, Progress.ENTITIES) };
	}

	public boolean hasNext() throws IOException, CollectionException {
		if (!processed)
			return true;
		else
			return false;
		
	}

	public CTakesFilePart getCtakesFilePart() {
		return filePart;
	}

	public void setFileToProcess(CTakesFilePart filePart) {
		this.filePart = filePart;
	}
}
