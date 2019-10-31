echo 'DOCS PATH=' $1
echo 'DUMMY_HIVE_TBL_NAME=' $2
echo 'HIVE_TBL_NAME PATH=' $3
echo 'MASTER_CONFIG_PATH=' $4
echo 'TEMP_CONFIG_PATH=' $5
echo 'USE_DEFAULT_NEGATION_ANNOTATORS=' $6
DT=$(date +"%Y%m%d%H%M")
export DT
echo $DT
pig -useHCatalog  -param RUNDT=$DT -param DOCS_PATH=$1  -param DUMMY_HIVE_TBL_NAME=$2 -param HIVE_TBL_NAME=$3 -param MASTER_CONFIG_PATH=$4 -param TMP_CONFIG_PATH=$5 -param USE_DEFAULT_NEGATION_ANNOTATORS=$6 -f process_ctakes_hive.pig