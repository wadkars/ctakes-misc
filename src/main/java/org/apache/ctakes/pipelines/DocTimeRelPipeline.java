package org.apache.ctakes.pipelines;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.chunker.ae.DefaultChunkCreator;
import org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster;
import org.apache.ctakes.constituency.parser.ae.ConstituencyParser;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.OverlapAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.dependency.parser.ae.ClearNLPSemanticRoleLabelerAE;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.lvg.ae.LvgAnnotator;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.temporal.ae.BackwardsTimeAnnotator;
import org.apache.ctakes.temporal.ae.DocTimeRelAnnotator;
import org.apache.ctakes.temporal.ae.EventAnnotator;
import org.apache.ctakes.temporal.pipelines.FullTemporalExtractionPipeline.CopyPropertiesToTemporalEventAnnotator;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
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
import org.cleartk.util.cr.UriCollectionReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class DocTimeRelPipeline {

  static interface Options {

    @Option(longName = "input-dir")
    public File getInputDirectory();

    @Option(longName = "output-dir")
    public File getOutputDirectory();
  }

  public static File inputDirectory;  // text files to process
  public static File outputDirectory; // directory to output xmi files

  public static void main(String[] args) throws Exception {

    Options options = CliFactory.parseArguments(Options.class, args);
    inputDirectory = options.getInputDirectory();
    outputDirectory = options.getOutputDirectory();

    List<File> files = new ArrayList<File>();
    for(File file : inputDirectory.listFiles()) {
      files.add(file);
    }

    CollectionReader reader = UriCollectionReader.getCollectionReaderFromFiles(files);
    AnalysisEngine engine = getXMIWritingPreprocessorAggregateBuilder().createAggregate();
    SimplePipeline.runPipeline(reader, engine);
  }

  protected static AggregateBuilder getXMIWritingPreprocessorAggregateBuilder()
      throws Exception {

    AggregateBuilder aggregateBuilder = new AggregateBuilder();
    
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(UriToDocumentTextAnnotator.class));

    // add document id annotation 
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(DocumentIDAnnotator.class));

    // identify segments 
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(SimpleSegmentAnnotator.class));
 
    // identify sentences
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        SentenceDetector.class,
        SentenceDetector.SD_MODEL_FILE_PARAM,
        "org/apache/ctakes/core/sentdetect/sd-med-model.zip" ) );

    // identify tokens
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(TokenizerAnnotatorPTB.class));
    // merge some tokens
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ContextDependentTokenizerAnnotator.class));

    // identify part-of-speech tags
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        POSTagger.class,
        TypeSystemDescriptionFactory.createTypeSystemDescription(),
        TypePrioritiesFactory.createTypePriorities(Segment.class, Sentence.class, BaseToken.class ),
        POSTagger.POS_MODEL_FILE_PARAM,
        "org/apache/ctakes/postagger/models/mayo-pos.zip" ) );
   
    // identify chunks
    /*
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        Chunker.class,
        Chunker.CHUNKER_MODEL_FILE_PARAM,
        FileLocator.getFile("org/apache/ctakes/chunker/models/chunker-model.zip"),
        Chunker.CHUNKER_CREATOR_CLASS_PARAM,
        DefaultChunkCreator.class));
*/
    // identify UMLS named entities

    // adjust NP in NP NP to span both
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        ChunkAdjuster.class,
        ChunkAdjuster.PARAM_CHUNK_PATTERN,
        new String[] { "NP", "NP" },
        ChunkAdjuster.PARAM_EXTEND_TO_INCLUDE_TOKEN,
        1));
    // adjust NP in NP PP NP to span all three
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        ChunkAdjuster.class,
        ChunkAdjuster.PARAM_CHUNK_PATTERN,
        new String[] { "NP", "PP", "NP" },
        ChunkAdjuster.PARAM_EXTEND_TO_INCLUDE_TOKEN,
        2));
    // add lookup windows for each NP
    aggregateBuilder
    .add( AnalysisEngineFactory.createEngineDescription(CopyNPChunksToLookupWindowAnnotations.class ) );
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
        new String[] { "selector=B" }));
    // add UMLS on top of lookup windows
    aggregateBuilder.add(LvgAnnotator.createAnnotatorDescription());
    aggregateBuilder.add(DefaultJCasTermAnnotator.createAnnotatorDescription());

    // add dependency parser
    aggregateBuilder.add(ClearNLPDependencyParserAE.createAnnotatorDescription());

    // add ctakes constituency parses to system view
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ConstituencyParser.class,
         ConstituencyParser.PARAM_MODEL_FILENAME,
         "org/apache/ctakes/constituency/parser/models/thyme.bin"));
    
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ClearNLPSemanticRoleLabelerAE.class));
    aggregateBuilder.add(BackwardsTimeAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/timeannotator/model.jar"));
    
    aggregateBuilder.add(EventAnnotator.createAnnotatorDescription());
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CopyPropertiesToTemporalEventAnnotator.class));
    aggregateBuilder.add(DocTimeRelAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/doctimerel/model.jar"));
    
    // write out the CAS after all the above annotations
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        XMIWriter.class,
        XMIWriter.PARAM_XMI_DIRECTORY,
        outputDirectory));

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
  }

  static File getXMIFile(File xmiDirectory, JCas jCas) throws AnalysisEngineProcessException {
    return getXMIFile(xmiDirectory, new File(ViewUriUtil.getURI(jCas).getPath()));
  }

  static File getXMIFile(File xmiDirectory, File textFile) {
    return new File(xmiDirectory, textFile.getName() + ".xmi");
  }
}
