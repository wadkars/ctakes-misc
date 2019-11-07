echo 'RAW_FILES_PATH=' $1
echo 'SEQUENCE_FILE_PATH=' $2
echo 'NO_OF_REDUCERS=' $3
echo 'MAX_FILE_SPLIT_SIZE' = $4
hadoop jar ctakes-misc-4.0.0-jar-with-dependencies.jar com.cloudera.ctakes.SmallFilesToSequenceFile \
-D mapreduce.map.memory.mb=2000 \
-D mapreduce.reduce.memory.mb=2000 \
-D mapreduce.map.java.opts.max.heap=1800 \
-D mapreduce.map.java.opts=-Xmx1800m \
-D mapreduce.reduce.java.opts=-Xmx1800m \
-D mapreduce.job.heap.memory-mb.ratio=0.8 \
-D mapreduce.task.timeout=21600000 \
$1 $2 $3 $4
