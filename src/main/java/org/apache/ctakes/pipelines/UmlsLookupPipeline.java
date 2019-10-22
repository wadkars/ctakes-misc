package org.apache.ctakes.pipelines;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.ctakes.assertion.medfacts.cleartk.PolarityCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.UncertaintyCleartkAnalysisEngine;
import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.chunker.ae.DefaultChunkCreator;
import org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster;
import org.apache.ctakes.clinicalpipeline.ClinicalPipelineFactory;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.OverlapAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.lvg.ae.LvgAnnotator;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypePrioritiesFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.XMLSerializer;
import org.cleartk.util.ViewUriUtil;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.FilesCollectionReader;
import org.cleartk.util.cr.UriCollectionReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class UmlsLookupPipeline {

  static interface Options {

    @Option(longName = "input-dir")
    public File getInputDirectory();

    @Option(longName = "output-dir")
    public File getOutputDirectory();
    
    @Option(longName = "lookupXml")
    public File getLookupXml();
  }

  public static File inputDirectory;  // text files to process
  public static File outputDirectory; // directory to output xmi files
  public static File lookupXml;
  public static void main2(String[] args) throws Exception {

    Options options = CliFactory.parseArguments(Options.class, args);
    inputDirectory = options.getInputDirectory();
    outputDirectory = options.getOutputDirectory();
    lookupXml = options.getLookupXml();

    List<File> files = new ArrayList<File>();
    for(File file : inputDirectory.listFiles()) {
      files.add(file);
    }

    CollectionReader reader = UriCollectionReader.getCollectionReaderFromFiles(files);
    CollectionReader reader2 = RushFilesCollectionReader.getCollectionReader(inputDirectory.getAbsolutePath());
    CollectionReader reader4 = FilesCollectionReader.getCollectionReader(inputDirectory.getAbsolutePath());
    
    //SingleFileCollectionReader reader5 = RushFilesCollectionReader.createReaderDescription(SingleFileCollectionReader.class,null,null)
    
    SingleFileCollectionReader reader3 = new SingleFileCollectionReader();
   
    String t = FileUtils.readFileToString(new File("/tmp/cTakesExample/cData/","10380.txt"));
    CTakesFilePart part = new CTakesFilePart("10380.txt",1,t);
    reader3.setFileToProcess(part);
    //Cloudera
    reader2.setConfigParameterValue("FileContent", part.getInput());
    AnalysisEngine engine = getXMIWritingPreprocessorAggregateBuilder().createAggregate();
    
    Map<String,String> output = RushPipeline.runPipeline(reader2, engine);
    for(String k:output.keySet()) {
    	System.err.println(k);
    	FileUtils.write(new File(outputDirectory,(new File(k).getName())), output.get(k));
    }
  
    
    /*
    for(File f:files) {
    	System.err.println("Processing File "  + f.getAbsolutePath());
    	String content = FileUtils.readFileToString(f);
    	String fName = f.getAbsolutePath();
    	CTakesFilePart part = new CTakesFilePart(fName,1,content);
    	SingleFileCollectionReader reader2 = new SingleFileCollectionReader();
    	reader2.setFileToProcess(part);
        AnalysisEngine engine2 = getXMIWritingPreprocessorAggregateBuilder().createAggregate();
        // AnalysisEngine engine = getFastPipeline().createAggregate();
        //reader2.getNext();
        RushPipeline.runPipeline(reader2, engine2);
    }
	*/
  }
  public static void main(String[] args) throws Exception {

	    Options options = CliFactory.parseArguments(Options.class, args);
	    inputDirectory = options.getInputDirectory();
	    outputDirectory = options.getOutputDirectory();
	    lookupXml = options.getLookupXml();

	    //List<File> files = new ArrayList<File>();
	    AnalysisEngine engine = getXMIWritingPreprocessorAggregateBuilder().createAggregate();
	    
	    CollectionReader reader = RushFilesCollectionReader.getCollectionReader(inputDirectory.getAbsolutePath());
	    CAS cas = RushPipeline.initializeCas(reader, engine);
	    for(File file : inputDirectory.listFiles()) {
	    	String t = FileUtils.readFileToString(file);
	    	CTakesFilePart part = new CTakesFilePart(file.getName(),1,t);
	    	reader.setConfigParameterValue("ctakesFilePart", part);
	    	CTakesResult result = RushPipeline.processCas(cas,reader,engine);
	    	FileUtils.write(new File(outputDirectory,file.getName()), result.getOutput());
	    	cas.reset();
	      //files.add(file);
	    }

	    
	  
	   
	    //String t = FileUtils.readFileToString(new File("/tmp/cTakesExample/cData/","10380.txt"));
	    //CTakesFilePart part = new CTakesFilePart("10380.txt",1,t);
	   
	    //Cloudera
	    //reader2.setConfigParameterValue("FileContent", part.getInput());
	    
	    /*
	    Map<String,String> output = RushPipeline.runPipeline(reader2, engine);
	    for(String k:output.keySet()) {
	    	System.err.println(k);
	    	FileUtils.write(new File(outputDirectory,(new File(k).getName())), output.get(k));
	    }
	  */
	    
	    /*
	    for(File f:files) {
	    	System.err.println("Processing File "  + f.getAbsolutePath());
	    	String content = FileUtils.readFileToString(f);
	    	String fName = f.getAbsolutePath();
	    	CTakesFilePart part = new CTakesFilePart(fName,1,content);
	    	SingleFileCollectionReader reader2 = new SingleFileCollectionReader();
	    	reader2.setFileToProcess(part);
	        AnalysisEngine engine2 = getXMIWritingPreprocessorAggregateBuilder().createAggregate();
	        // AnalysisEngine engine = getFastPipeline().createAggregate();
	        //reader2.getNext();
	        RushPipeline.runPipeline(reader2, engine2);
	    }
		*/
	  }

  protected static AggregateBuilder getFastPipeline() throws Exception {

    AggregateBuilder aggregateBuilder = new AggregateBuilder();

    aggregateBuilder.add(ClinicalPipelineFactory.getFastPipeline());

    return aggregateBuilder;
  }

  protected static AggregateBuilder getXMIWritingPreprocessorAggregateBuilder()
      throws Exception {
    AggregateBuilder aggregateBuilder = new AggregateBuilder();
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RushURIToDocumentTextAnnotator.class));

    // add document id annotation 
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(DocumentIDAnnotator.class));

    // identify segments 
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(SimpleSegmentAnnotator.class));

    // identify sentences
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        SentenceDetector.class,
        SentenceDetector.SD_MODEL_FILE_PARAM,
        "org/apache/ctakes/core/sentdetect/sd-med-model.zip"));
    // identify tokens
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(TokenizerAnnotatorPTB.class));
    // merge some tokens
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ContextDependentTokenizerAnnotator.class));

    // identify part-of-speech tags
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        POSTagger.class,
        TypeSystemDescriptionFactory.createTypeSystemDescription(),
        TypePrioritiesFactory.createTypePriorities(Segment.class, Sentence.class, BaseToken.class),
        POSTagger.POS_MODEL_FILE_PARAM,
        "org/apache/ctakes/postagger/models/mayo-pos.zip"));

    // originally we had FileLocator.locateFile( "org/apache/ctakes/chunker/models/chunker-model.zip" )
    // but this failed to locate the chunker model, so using the absolute path
    
    
    //String absolutePathToChunkerModel = System.getenv("CTAKES_HOME") +
    		
    String absolutePathToChunkerModel = "/tmp/ctakes-trunk/trunk/" +		
        "ctakes-chunker-res/src/main/resources/org/apache/ctakes/chunker/models/chunker-model.zip";
       // "ctakes-chunker-res/src/main/resources/org/apache/ctakes/chunker/models/chunk-model.claims-1.5.zip";
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        Chunker.class,
        Chunker.CHUNKER_MODEL_FILE_PARAM,
        FileLocator.locateFile(absolutePathToChunkerModel),
        Chunker.CHUNKER_CREATOR_CLASS_PARAM,
        DefaultChunkCreator.class ) );

    // identify UMLS named entities

    // adjust NP in NP NP to span both
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        ChunkAdjuster.class,
        ChunkAdjuster.PARAM_CHUNK_PATTERN,
        new String[] { "NP", "NP" },
        ChunkAdjuster.PARAM_EXTEND_TO_INCLUDE_TOKEN,
        1 ) );
    // adjust NP in NP PP NP to span all three
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        ChunkAdjuster.class,
        ChunkAdjuster.PARAM_CHUNK_PATTERN,
        new String[] { "NP", "PP", "NP" },
        ChunkAdjuster.PARAM_EXTEND_TO_INCLUDE_TOKEN,
        2 ) );
    // add lookup windows for each NP
    aggregateBuilder
    .add( AnalysisEngineFactory.createEngineDescription( CopyNPChunksToLookupWindowAnnotations.class ) );
    // maximize lookup windows
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        OverlapAnnotator.class,
        "A_ObjectClass",
        LookupWindowAnnotation.class,
        "B_ObjectClass",
        LookupWindowAnnotation.class,
        "OverlapType",
        "A_ENV_B",
        "ActionType",
        "DELETE",
        "DeleteAction",
        new String[] { "selector=B" } ) );
    // add UMLS on top of lookup windows
    aggregateBuilder.add( DefaultJCasTermAnnotator.createAnnotatorDescription(lookupXml.getAbsolutePath()) );

    aggregateBuilder.add( LvgAnnotator.createAnnotatorDescription() );

    // the following two AEs slow down the pipeline significantly when input file are large
    // aggregateBuilder.add( PolarityCleartkAnalysisEngine.createAnnotatorDescription() );
    // aggregateBuilder.add( UncertaintyCleartkAnalysisEngine.createAnnotatorDescription() );  
    
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        "desc/NegationAnnotator"));    
            
    // write out the CAS after all the above annotations //Write to Spark Output context directly from here
    /*
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
    		HiveWriter.class,
    		HiveWriter.PARAM_XMI_DIRECTORY,
    		        outputDirectory);
  
    aggregateBuilder.add(writer);
   ß */
    return aggregateBuilder;
  }
  
  /*
   * Add document id annotation
   */
  public static class DocumentIDAnnotator extends JCasAnnotator_ImplBase {

    @Override
    public void process( JCas jCas ) throws AnalysisEngineProcessException {
      String documentID = new File( ViewUriUtil.getURI( jCas ) ).getPath();
      System.out.println("\nprocessing: " + documentID);
      DocumentID documentIDAnnotation = new DocumentID( jCas );
      documentIDAnnotation.setDocumentID( documentID );
      documentIDAnnotation.addToIndexes();
    }
  }

  /* 
   * The following class overrides a ClearTK utility annotator class for reading
   * a text file into a JCas. The code is copy/pasted so that one tiny modification
   * can be made for this corpus -- replace a single odd character (0xc) with a 
   * space since it trips up xml output.  
   */
//  public static class UriToDocumentTextAnnotatorCtakes extends UriToDocumentTextAnnotator {
//
//    @Override
//    public void process(JCas jCas) throws AnalysisEngineProcessException {
//      URI uri = ViewUriUtil.getURI(jCas);
//      String content;
//
//      try {
//        content = CharStreams.toString(new InputStreamReader(uri.toURL().openStream()));
//        content = content.replace((char) 0xc, ' ');
//        jCas.setSofaDataString(content, "text/plain");
//      } catch (MalformedURLException e) {
//        throw new AnalysisEngineProcessException(e);
//      } catch (IOException e) {
//        throw new AnalysisEngineProcessException(e);
//      }
//    }  
//  }

  public static class CopyNPChunksToLookupWindowAnnotations extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      for (Chunk chunk : JCasUtil.select(jCas, Chunk.class)) {
        if (chunk.getChunkType().equals("NP")) {
          new LookupWindowAnnotation(jCas, chunk.getBegin(), chunk.getEnd()).addToIndexes();
        }
      }
    }
  }

  public static class XMIWriter extends JCasAnnotator_ImplBase {

    public static final String PARAM_XMI_DIRECTORY = "XMIDirectory";

    @ConfigurationParameter(name = PARAM_XMI_DIRECTORY, mandatory = true)
    private File xmiDirectory;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
      super.initialize(context);
      if (!this.xmiDirectory.exists()) {
        this.xmiDirectory.mkdirs();
      }
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      File xmiFile = getXMIFile(this.xmiDirectory, jCas);
      try {
        FileOutputStream outputStream = new FileOutputStream(xmiFile);
        try {
          XmiCasSerializer serializer = new XmiCasSerializer(jCas.getTypeSystem());
          ContentHandler handler = new XMLSerializer(outputStream, false).getContentHandler();
          serializer.serialize(jCas.getCas(), handler);
        } finally {
          outputStream.close();
        }
      } catch (SAXException e) {
        throw new AnalysisEngineProcessException(e);
      } catch (IOException e) {
        throw new AnalysisEngineProcessException(e);
      }
    }
    static File getXMIFile(File xmiDirectory, JCas jCas) throws AnalysisEngineProcessException {
        return getXMIFile(xmiDirectory, new File(ViewUriUtil.getURI(jCas).getPath()));
      }

      static File getXMIFile(File xmiDirectory, File textFile) {
        return new File(xmiDirectory, textFile.getName() + ".xmi");
      }
  }


}
