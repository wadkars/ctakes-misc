package org.apache.ctakes.pipelines;


import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.util.LifeCycleUtil.collectionProcessComplete;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.internal.ResourceManagerFactory;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;
import org.cleartk.util.ViewUriUtil;

/**
 *
 */
public  class RushPipeline {
  private RushPipeline() {
    // This class is not meant to be instantiated
  }

  /**
   * <p>
   * Run the CollectionReader and AnalysisEngines as a pipeline. After processing all CASes provided
   * by the reader, the method calls {@link AnalysisEngine#collectionProcessComplete()
   * collectionProcessComplete()} on the engines and {@link Resource#destroy() destroy()} on all
   * engines.
   * </p>
   * <p>
   * Note that with this method, external resources cannot be shared between the reader and the
   * analysis engines. They can be shared amongst the analysis engines.
   * </p>
   * 
   * @param reader
   *          The CollectionReader that loads the documents into the CAS.
   * @param descs
   *          Primitive AnalysisEngineDescriptions that process the CAS, in order. If you have a mix
   *          of primitive and aggregate engines, then please create the AnalysisEngines yourself
   *          and call the other runPipeline method.
   * @throws UIMAException
   *           if there is a problem initializing or running the CPE.
   * @throws IOException
   *           if there is an I/O problem in the reader
   */
  public static void runPipeline(final CollectionReader reader,
          final AnalysisEngineDescription... descs) throws UIMAException, IOException {
    // Create AAE
    final AnalysisEngineDescription aaeDesc = createEngineDescription(descs);

    // Instantiate AAE
    final AnalysisEngine aae = createEngine(aaeDesc);

    // Create CAS from merged metadata
    ResourceManager resMgr = ResourceManagerFactory.newResourceManager();
    final CAS cas = CasCreationUtils.createCas(asList(reader.getMetaData(), aae.getMetaData()), 
            null, resMgr);
    reader.typeSystemInit(cas.getTypeSystem());

    try {
      // Process
      while (reader.hasNext()) {
        reader.getNext(cas);
        aae.process(cas);
        cas.reset();
      }

      // Signal end of processing
      aae.collectionProcessComplete();
    } finally {
      // Destroy
      aae.destroy();
    }
  }

  /**
   * <p>
   * Run the CollectionReader and AnalysisEngines as a pipeline. After processing all CASes provided
   * by the reader, the method calls {@link AnalysisEngine#collectionProcessComplete()
   * collectionProcessComplete()} on the engines, {@link CollectionReader#close() close()} on the
   * reader and {@link Resource#destroy() destroy()} on the reader and all engines.
   * </p>
   * <p>
   * External resources can be shared between the reader and the analysis engines.
   * </p>
   * 
   * @param readerDesc
   *          The CollectionReader that loads the documents into the CAS.
   * @param descs
   *          Primitive AnalysisEngineDescriptions that process the CAS, in order. If you have a mix
   *          of primitive and aggregate engines, then please create the AnalysisEngines yourself
   *          and call the other runPipeline method.
   * @throws UIMAException
   *           if there is a problem initializing or running the CPE.
   * @throws IOException
   *           if there is an I/O problem in the reader
   */
  public static void runPipeline(final CollectionReaderDescription readerDesc,
          final AnalysisEngineDescription... descs) throws UIMAException, IOException {
    ResourceManager resMgr = ResourceManagerFactory.newResourceManager();
    
    // Create the components
    final CollectionReader reader = UIMAFramework.produceCollectionReader(readerDesc, resMgr, null);

    // Create AAE
    final AnalysisEngineDescription aaeDesc = createEngineDescription(descs);

    // Instantiate AAE
    final AnalysisEngine aae = UIMAFramework.produceAnalysisEngine(aaeDesc, resMgr, null);

    // Create CAS from merged metadata
    final CAS cas = CasCreationUtils.createCas(asList(reader.getMetaData(), aae.getMetaData()),
            null, resMgr);
    reader.typeSystemInit(cas.getTypeSystem());

    try {
      // Process
      while (reader.hasNext()) {
        reader.getNext(cas);
        aae.process(cas);
        cas.reset();
      }

      // Signal end of processing
      aae.collectionProcessComplete();
    } finally {
      // Destroy
      aae.destroy();
    }
  }

  /**
   * <p>
   * Provides a simple way to run a pipeline for a given collection reader and sequence of analysis
   * engines. After processing all CASes provided by the reader, the method calls
   * {@link AnalysisEngine#collectionProcessComplete() collectionProcessComplete()} on the engines.
   * </p>
   * <p>
   * External resources can only be shared between the reader and/or the analysis engines if the
   * reader/engines have been previously instantiated using a shared resource manager.
   * </p>
   * 
   * @param reader
   *          a collection reader
   * @param engines
   *          a sequence of analysis engines
   * @throws UIMAException
   *           if there is a problem initializing or running the CPE.
   * @throws IOException
   *           if there is an I/O problem in the reader
   */
  public static Map<String,String> runPipeline(final CollectionReader reader, final AnalysisEngine... engines)
          throws UIMAException, IOException {
    final List<ResourceMetaData> metaData = new ArrayList<ResourceMetaData>();
    metaData.add(reader.getMetaData());
    for (AnalysisEngine engine : engines) {
      metaData.add(engine.getMetaData());
    }

    ResourceManager resMgr = ResourceManagerFactory.newResourceManager();
    final CAS cas = CasCreationUtils.createCas(metaData, null, resMgr);
    reader.typeSystemInit(cas.getTypeSystem());
    Map<String,String> output = new HashMap<>();
    while (reader.hasNext()) {
      reader.getNext(cas);
      String outputStr = runPipeline(cas, engines);
      output.put(ViewUriUtil.getURI(cas.getJCas()).getPath(), outputStr); 
      cas.reset();
      //System.err.println(cas.getSofa().getLocalStringData());
    }
    collectionProcessComplete(engines);
    return output;
  }

 // private  CAS myCas;
  //private CTakesFilePart cTakesFilePart;
  public static CAS initializeCas(final CollectionReader reader, final AnalysisEngine... engines) throws ResourceInitializationException {
	  final List<ResourceMetaData> metaData = new ArrayList<ResourceMetaData>();
	    metaData.add(reader.getMetaData());
	    for (AnalysisEngine engine : engines) {
	      metaData.add(engine.getMetaData());
	    }

	    ResourceManager resMgr = ResourceManagerFactory.newResourceManager();
	    CAS myCas = CasCreationUtils.createCas(metaData, null, resMgr);
	    reader.typeSystemInit(myCas.getTypeSystem());
	    return myCas;
  }
  
  public static CTakesResult processCas(final CAS myCas, final CollectionReader reader, final AnalysisEngine... engines) throws Exception{
	  CTakesFilePart cTakesFilePart = (CTakesFilePart) reader.getConfigParameterValue("ctakesFilePart");
	  Map<String,String> output = new HashMap<>();
      reader.getNext(myCas);
      String outputStr = runPipeline(myCas, engines);

      //output.put(ViewUriUtil.getURI(myCas.getJCas()).getPath(), outputStr);
      output.put(cTakesFilePart.toString(), outputStr);
      CTakesResult result = new CTakesResult();
      result.setOutput(outputStr);
     // myCas.reset();
      return result;
     
  }
  
  public static CTakesResult processCuisCas(final CAS myCas, final CollectionReader reader, final AnalysisEngine engine) throws Exception{
	  CTakesFilePart cTakesFilePart = (CTakesFilePart) reader.getConfigParameterValue("ctakesFilePart");
	  Map<String,String> output = new HashMap<>();
      reader.getNext(myCas);
      String outputStr = runCuisPipeline(myCas, engine);

      //output.put(ViewUriUtil.getURI(myCas.getJCas()).getPath(), outputStr);
      output.put(cTakesFilePart.toString(), outputStr);
      CTakesResult result = new CTakesResult();
      result.setOutput(outputStr);
     // myCas.reset();
      return result;
     
  }
  public static void close(final AnalysisEngine... engines) throws AnalysisEngineProcessException {
	  collectionProcessComplete(engines);
  }
  /**
   * <p>
   * Run a sequence of {@link AnalysisEngine analysis engines} over a {@link JCas}. The result of
   * the analysis can be read from the JCas.
   * </p>
   * <p>
   * External resources can be shared between the analysis engines.
   * </p>
   * 
   * @param aCas
   *          the CAS to process
   * @param aDescs
   *          a sequence of analysis engines to run on the jCas
   * @throws ResourceInitializationException
   *           if there is a problem initializing the components
   * @throws AnalysisEngineProcessException
   *           if there is a problem during the execution of the components
   */
  public static void runPipeline(final CAS aCas, final AnalysisEngineDescription... aDescs)
          throws ResourceInitializationException, AnalysisEngineProcessException {
    // Create aggregate AE
    final AnalysisEngineDescription aaeDesc = createEngineDescription(aDescs);

    // Instantiate
    final AnalysisEngine aae = createEngine(aaeDesc);
    try {
      // Process
      aae.process(aCas);

      // Signal end of processing
      aae.collectionProcessComplete();
    } finally {
      // Destroy
      aae.destroy();
    }
  }

  /**
   * <p>
   * Run a sequence of {@link AnalysisEngine analysis engines} over a {@link JCas}. The result of
   * the analysis can be read from the JCas.
   * </p>
   * <p>
   * External resources can be shared between the analysis engines.
   * </p>
   * 
   * @param jCas
   *          the jCas to process
   * @param descs
   *          a sequence of analysis engines to run on the jCas
   * @throws ResourceInitializationException
   *           if there is a problem initializing the components
   * @throws AnalysisEngineProcessException
   *           if there is a problem during the execution of the components
   */
  public static void runPipeline(final JCas jCas, final AnalysisEngineDescription... descs)
          throws AnalysisEngineProcessException, ResourceInitializationException {
    runPipeline(jCas.getCas(), descs);
  }

  /**
   * <p>
   * Run a sequence of {@link AnalysisEngine analysis engines} over a {@link JCas}. This method does
   * not {@link AnalysisEngine#destroy() destroy} the engines or send them other events like
   * {@link AnalysisEngine#collectionProcessComplete()}. This is left to the caller.
   * </p>
   * <p>
   * External resources can only be shared between the analysis engines if the engines have been
   * previously instantiated using a shared resource manager.
   * </p>
   * 
   * 
   * @param jCas
   *          the jCas to process
   * @param engines
   *          a sequence of analysis engines to run on the jCas
   * @throws AnalysisEngineProcessException
   *           if there is a problem during the execution of the components
   */
  public static void runPipeline(final JCas jCas, final AnalysisEngine... engines)
          throws AnalysisEngineProcessException {
    for (AnalysisEngine engine : engines) {
      engine.process(jCas);
    }
  }

  /**
   * <p>
   * Run a sequence of {@link AnalysisEngine analysis engines} over a {@link CAS}. This method does
   * not {@link AnalysisEngine#destroy() destroy} the engines or send them other events like
   * {@link AnalysisEngine#collectionProcessComplete()}. This is left to the caller.
   * </p>
   * <p>
   * External resources can only be shared between the analysis engines if the engines have been
   * previously instantiated using a shared resource manager.
   * </p>
   * 
   * @param cas
   *          the CAS to process
   * @param engines
   *          a sequence of analysis engines to run on the jCas
   * @throws AnalysisEngineProcessException
   *           if there is a problem during the execution of the components
 * @throws IOException 
   */
  public static String runPipeline(final CAS cas, final AnalysisEngine... engines)
          throws AnalysisEngineProcessException, IOException {
	
    for (AnalysisEngine engine : engines) {
      engine.process(cas);

      
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    //System.err.println("DD" + cas.getSofaDataString());
    CasIOUtils.save(cas, bos, SerialFormat.XMI);
    return new String(bos.toByteArray());

  }
  public static String runCuisPipeline(final CAS cas, final AnalysisEngine engine)
          throws AnalysisEngineProcessException, IOException {
	
    
      engine.process(cas);

      
    
    return cas.getDocumentText();

  }

  /**
   * <p>
   * Iterate through the {@link JCas JCases} processed by the pipeline, allowing to access each one
   * after it has been processed.
   * </p>
   * <p>
   * External resources can be shared between the reader and the analysis engines.
   * </p>
   * 
   * @param aReader
   *          the collection reader.
   * @param aEngines
   *          the analysis engines.
   * @return an {@link Iterable}&lt;{@link JCas}&gt; which can be used in an extended for-loop.
   */
  public static JCasIterable iteratePipeline(final CollectionReaderDescription aReader,
          AnalysisEngineDescription... aEngines) {
    return new JCasIterable(aReader, aEngines);
  }
}
