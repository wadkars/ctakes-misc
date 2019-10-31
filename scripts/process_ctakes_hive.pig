set mapreduce.map.memory.mb    2000
set mapreduce.reduce.memory.mb 2000
SET output.compression.enabled true;
set mapreduce.output.compress true;
set hive.exec.compress.output true;
SET pig.maxCombinedSplitSize 20000;

set default_parallel 10;
register /opt/cloudera/parcels/CDH/lib/pig/piggybank.jar;
register ctakes-misc-4.0.0-jar-with-dependencies.jar
DEFINE PROCESSPAGE com.cloudera.ctakes.CTakesExtractor('$MASTER_CONFIG_PATH','$TMP_CONFIG_PATH','$USE_DEFAULT_NEGATION_ANNOTATORS');

finalTable = LOAD '$DUMMY_HIVE_TBL_NAME' USING org.apache.hive.hcatalog.pig.HCatLoader();


A = LOAD '$DOCS_PATH' USING  org.apache.pig.piggybank.storage.SequenceFileLoader() AS (key:chararray,value:chararray);;
C = FOREACH A GENERATE FLATTEN(PROCESSPAGE($0, $1));
D = UNION finalTable,C;
store D into '$HIVE_TBL_NAME' using org.apache.hive.hcatalog.pig.HCatStorer('loaded=$RUNDT','fname: chararray, part: chararray,parsed: boolean,text: chararray,annotations: chararray,cuis: chararray');