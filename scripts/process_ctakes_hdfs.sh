echo 'DOCS PATH=' $1
echo 'ANNOTATIONS_PATH=' $2
hdfs dfs -rmr $2
pig  -param DOCS_PATH=$1 -param ANNOTATIONS_PATH=$2  -f process_ctakes_hdfs.pig
