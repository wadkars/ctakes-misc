echo 'DOCS PATH=' $1
echo 'ANNOTATIONS_PATH=' $2
echo 'MASTER_CONFIG_PATH=' $3
echo 'TEMP_CONFIG_PATH=' $4
echo 'USE_DEFAULT_NEGATION_ANNOTATORS=' $5
hdfs dfs -rmr $2
pig  -param DOCS_PATH=$1 -param ANNOTATIONS_PATH=$2  -param MASTER_CONFIG_PATH=$3 -param TMP_CONFIG_PATH=$4 -param USE_DEFAULT_NEGATION_ANNOTATORS=$5 -f process_ctakes_hdfs.pig
