package com.cloudera.ctakes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.ctakes.pipelines.CTakesResult;
import org.apache.ctakes.pipelines.RushEndToEndPipeline;
import org.apache.ctakes.utils.RushConfig;
import org.apache.ctakes.utils.RushFileUtils;
import org.apache.curator.shaded.com.google.common.base.Throwables;
import org.apache.hadoop.conf.Configuration;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.apache.pig.impl.logicalLayer.schema.Schema.FieldSchema;
import org.apache.pig.impl.util.UDFContext;
import org.apache.tools.ant.util.StringUtils;
import org.apache.uima.collection.metadata.CpeDescription;

/**
 * Pig UDF to process text through cTAKES.
 * 
 * @author Paul Codding - paul@hortonworks.com
 * 
 */
public class CTakesExtractor extends EvalFunc<Tuple> {
	
	
	public static final String MASTER_CONFIG_FOLDER_KEY = "MASTER_CONFIG_FOLDER";
	public static final String TMP_CONFIG_FOLDER_KEY = "TMP_CONFIG_FOLDER";
	
	TupleFactory tf = TupleFactory.getInstance();
	BagFactory bf = BagFactory.getInstance();
	long numTuplesProcessed = 0;
	CpeDescription cpeDesc = null;
	Properties myProperties = null;
	private transient RushEndToEndPipeline pipeline = null;
	private transient RushConfig config = null;
	private String masterConfigPath = "";
	private String tmpConfigPath = "";
	private boolean useDefaultNegationAnnotators = true;
	// String pipelinePath = "";
	/**
	 * Initialize the CpeDescription class.
	 * @throws Exception 
	 */
	
	public CTakesExtractor(String masterConfigPath, String tmpConfigPath, String useDefaultNegationAnnotators) {
		this.masterConfigPath = masterConfigPath;
		this.tmpConfigPath = tmpConfigPath;
		this.useDefaultNegationAnnotators = Boolean.parseBoolean(useDefaultNegationAnnotators);
	}

	private void initializeFramework() {
		/*
		if (myProperties == null) {

			Configuration conf = UDFContext.getUDFContext().getJobConf();
			if (conf == null) {
				myProperties = System.getProperties();
			}else {
				myProperties.put(MASTER_CONFIG_FOLDER_KEY, conf.get(MASTER_CONFIG_FOLDER_KEY));
				myProperties.put(TMP_CONFIG_FOLDER_KEY, conf.get(TMP_CONFIG_FOLDER_KEY));
			}

			//this.pipeline = new RushEndToEndPipeline(this.myProperties.getProperty(MASTER_CONFIG_FOLDER_KEY,TMP_CONFIG_FOLDER_KEY));
		}
		*/
		if(this.pipeline==null) {
			this.config = new RushConfig(this.masterConfigPath,
                    this.tmpConfigPath);
			this.config.initialize();
			
			int failedCount = 0;
			boolean success = false;
			while(!success) {
				try {
					this.pipeline = new RushEndToEndPipeline(this.config,this.useDefaultNegationAnnotators);
					success=true;
					log.info(" Success after " + failedCount);
				}catch (Exception e) {
					try {
						log.info("Sleeping for 5 seconds" + failedCount + "=" + e.getMessage());
						Thread.currentThread().sleep(5000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					failedCount++;
					if(failedCount==10) {
						Throwables.propagate(e);
					}
				}				
			}
		}

	}

	public void finish() {
		if(this.pipeline!=null) {
			//this.initializeFramework();
			this.pipeline.close();
			this.config.close();

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.pig.EvalFunc#exec(org.apache.pig.data.Tuple)
	 */
	@Override
	public Tuple exec(Tuple input) throws IOException {
		this.initializeFramework();

		long started = System.currentTimeMillis();
		Tuple resultOnly = tf.newTuple(2);
		Tuple result = tf.newTuple(6);
		try {

			String fNameId = input.get(0).toString();
			// Now split it
			int idx = fNameId.lastIndexOf("-");
			String partName = fNameId.substring(idx + 1, fNameId.length());
			String fileName = fNameId.substring(0, idx);
			String encounterId = RushFileUtils.getEncounterId(fileName);
			result.set(0,encounterId );
			result.set(1, partName);
			result.set(2, true);
			String fileContent = (String) input.get(1);
			CTakesResult ctakesResult = this.pipeline.getResult(encounterId, Integer.parseInt(partName), fileContent);
			// inputStr = inputStr.replaceAll("\\r|\\n", "");
			// System.out.println(inputStr);
			// result.set(2, ((String)input.get(1)).replace("\n", " ").replace("\r", " "));
			// inputStr="";
			result.set(3, fileContent);
			result.set(4, ctakesResult.getOutput());
			result.set(5, ctakesResult.getCuis());

		} catch (Exception e) {
			result.set(2, false);
			result.set(4, ExceptionUtils.getStackTrace(e));
			
			log.error(e.getMessage());
			e.printStackTrace();
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.pig.EvalFunc#outputSchema(org.apache.pig.impl.logicalLayer
	 * .schema.Schema)
	 */
	@Override
	public Schema outputSchema(Schema input) {
		try {
			Schema tupleSchema = new Schema();
			tupleSchema.add(new FieldSchema("fname", DataType.CHARARRAY));
			tupleSchema.add(new FieldSchema("part", DataType.CHARARRAY));
			tupleSchema.add(new FieldSchema("parsed", DataType.BOOLEAN));
			tupleSchema.add(new FieldSchema("text", DataType.CHARARRAY));
			// tupleSchema.add(input.getField(0));
			// tupleSchema.add(input.getField(1));
			tupleSchema.add(new FieldSchema("annotations", DataType.CHARARRAY));
			tupleSchema.add(new FieldSchema("cuis", DataType.CHARARRAY));
			return new Schema(new Schema.FieldSchema(null, tupleSchema, DataType.TUPLE));
			// return tupleSchema;
		} catch (Exception e) {
			return null;
		}
	}

	public static void main(String[] args) throws Exception {
		Properties myProperties = System.getProperties();
		boolean b = Boolean.parseBoolean("true");
		System.out.println(b);
		b = Boolean.parseBoolean("false");
		System.out.println(b);
		CTakesExtractor p = new CTakesExtractor(myProperties.getProperty(MASTER_CONFIG_FOLDER_KEY),
				myProperties.getProperty(TMP_CONFIG_FOLDER_KEY),"false");
		TupleFactory tf = TupleFactory.getInstance();
		List<String> l = new ArrayList<>();
		l.add("/tmp/cTakesExample/cData/4490.txt-1");
		// l.add("Nasal trauma is an injury to your nose or the areas that surround and
		// support your nose. Internal or external injuries can cause nasal trauma. The
		// position of your nose makes your nasal bones, cartilage, and soft tissue
		// particularly vulnerable to external injuries");

		String s = FileUtils.readFileToString(new File("/tmp/cTakesExample/cData/2.txt"));
		// System.out.println(s);
		l.add(s);
		Tuple t = tf.newTuple(l);
		Tuple o = p.exec(t);
		//System.out.println(o.get(0) + "\n" + o.get(1) + "\n" + o.get(2) + "\n" + o.get(3));
		System.err.println(o.get(0));
		System.err.println(o.get(1));
		//System.err.println(o.get(2));
		//System.err.println(o.get(3));
		//System.err.println(o.get(4));
		System.err.println(o.get(5));
		// System.out.println(o.get(2));
		// System.out.println(o.get(3));
		// System.err.println(o.get(1));
		// System.err.println(o.get(2));
		// System.out.println(o.size());
		//FileUtils.writeStringToFile(new File("/tmp/CTAKES_DATA/output/test.xml"), (String) o.get(4));
		p.finish();
	}
}