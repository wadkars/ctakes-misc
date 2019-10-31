set mapreduce.map.memory.mb    2000
set mapreduce.reduce.memory.mb 2000
SET mapred.map.tasks 5
SET pig.maxCombinedSplitSize 20000;

set default_parallel 10;
register /opt/cloudera/parcels/CDH/lib/pig/piggybank.jar;
register ctakes-misc-4.0.0-jar-with-dependencies.jar
DEFINE PROCESSPAGE com.cloudera.ctakes.CTakesExtractor('$MASTER_CONFIG_PATH','$TMP_CONFIG_PATH','$USE_DEFAULT_NEGATION_ANNOTATORS');

A = LOAD '$DOCS_PATH' USING  org.apache.pig.piggybank.storage.SequenceFileLoader() AS (key:chararray,value:chararray);;
C = FOREACH A GENERATE FLATTEN(PROCESSPAGE($0, $1));
STORE C  INTO '$ANNOTATIONS_PATH' USING PigStorage('^');
