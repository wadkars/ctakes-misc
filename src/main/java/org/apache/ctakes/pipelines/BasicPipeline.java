package org.apache.ctakes.pipelines;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.chunker.ae.DefaultChunkCreator;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.postagger.POSTagger;
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

import com.google.common.io.CharStreams;

public class BasicPipeline {

  public static File inputDirectory = new File("/Users/Dima/Loyola/Workspaces/cTakes/ctakes/ctakes-examples-res/src/main/resources/org/apache/ctakes/examples/notes/rtf/");
  public static String outputDirectory = "/Users/Dima/Temp/";

  public static void main(String[] args) throws Exception {

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
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(UriToDocumentTextAnnotatorCtakes.class));

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

    // identify chunks
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        Chunker.class,
        Chunker.CHUNKER_MODEL_FILE_PARAM,
        FileLocator.locateFile("/Users/Dima/Loyola/Workspaces/cTakes/ctakes/ctakes-chunker-res/src/main/resources/org/apache/ctakes/chunker/models/chunker-model.zip"),
        Chunker.CHUNKER_CREATOR_CLASS_PARAM,
        DefaultChunkCreator.class));
  
    // write out the CAS after all the above annotations
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        XMIWriter.class,
        XMIWriter.PARAM_XMI_DIRECTORY,
        outputDirectory));

    return aggregateBuilder;
  }
  
  /* 
   * The following class overrides a ClearTK utility annotator class for reading
   * a text file into a JCas. The code is copy/pasted so that one tiny modification
   * can be made for this corpus -- replace a single odd character (0xc) with a 
   * space since it trips up xml output.  
   */
  public static class UriToDocumentTextAnnotatorCtakes extends UriToDocumentTextAnnotator {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      URI uri = ViewUriUtil.getURI(jCas);
      String content;

      try {
        content = CharStreams.toString(new InputStreamReader(uri.toURL().openStream()));
        content = content.replace((char) 0xc, ' ');
        jCas.setSofaDataString(content, "text/plain");
      } catch (MalformedURLException e) {
        throw new AnalysisEngineProcessException(e);
      } catch (IOException e) {
        throw new AnalysisEngineProcessException(e);
      }
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
