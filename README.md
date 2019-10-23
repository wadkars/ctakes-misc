
## Preliminary steps

* Create a fake folder to allow the Files Collection Reader to get initialized by executing the following command  `mkdir /tmp/random/` 
* Create a folder `mkdir /tmp/cTakesExample/cData` . Put all the input files into this folder
* Create a folder `mkdir /tmp/ctakes-config` and copy all the contents of "./resources/" into this folder
* Run the following command

`
mvn exec:java -Dexec.mainClass="org.apache.ctakes.pipelines.RushEndToEndPipeline" -Dexec.args="--input-dir /tmp/cTakesExample/cData --output-dir /tmp/cTakesExample/ --lookupXml ./resources/sno_rx_16ab-local.xml"
`
* This will produce two folder /tmp/cTakesExample/xmis and /tmp/cTakesExample/cuis/ and place the respect XMI and CUI's into it.


=============

This project contains code that will allow cTAKES to be invoked on clinical document data presented as Tuples to [cTAKES](http://ctakes.apache.org).  There are two UDF's:


# Deploying to the CDH 6

## Preparation

Because this code should be run as a non-privileged user, one must first be created on the sandbox, and in HDFS.

	# useradd -G hdfs,hadoop  ctakesuser
	# passwd ctakesuser
	# su - hdfs -c "hdfs dfs -mkdir /user/ctakesuser"
	# su - hdfs -c "hdfs dfs -chown ctakesuser:hadoop /user/ctakesuser"
	# su - hdfs -c "hdfs dfs -chmod 755 /user/ctakesuser"
	
We'll also need to add SVN,MVN,GIT to checkout the Apache cTAKES code.
	# yum -y install svn
	# yum -y install maven
	# yum install git
	
## Install Hadoop CTakes
	# su - ctakesuser 
	# sudo yum install maven
	# sudo yum install svn
	$ mvn -version
	$ mkdir ~/src
	$ cd ~/src
	$ git clone https://github.com/wadkars/ctakes-misc.git
	$ cd ctakes-misc
	$ cd ~/src
	
	$ mkdir /tmp/ctakes_config
	$ cp -r resources/* /tmp/ctakes_config/
	$ chmod -R 777 /tmp/ctakes_config
	$ cd ~/src/ctakes-misc
	$  mvn -Dmaven.test.skip=true install
	

## Creating the HCatalog tables

	$ hive
	hive> drop table if exists ctakes_annotations_docs_dummy;
	hive> drop table if exists ctakes_annotations_docs;
	hive> CREATE TABLE ctakes_annotations_docs_dummy(fname STRING, part STRING, parsed BOOLEAN, text STRING, annotations STRING, cuis STRING) PARTITIONED BY (loaded STRING) STORED AS SEQUENCEFILE;

	hive> CREATE TABLE ctakes_annotations_docs(fname STRING, part STRING, parsed BOOLEAN, text STRING, annotations STRING, cuis STRING) PARTITIONED BY (loaded STRING) STORED AS SEQUENCEFILE;

	hive> quit;

	
## Copy Sample Data Into Cluster

A few sample articles are included in the project under ./sample_data/data .  We'll add this data to the cluster using the following commands.

	$ hdfs dfs -mkdir ./sample_data_txt
	$ hdfs dfs -put ~/src/hadoop2_ctakes/sample_data/data/* ./sample_data_txt
	$ hdfs dfs -ls ./sample_data_txt
## Copy /tmp/ctakes_config to all datanodes in the cluster

The PIG UDF's will run on the data nodes. Copy the folder "ctakes_config" to all the data nodes in the cluster. My command to do that is. Ensure that the "/tmp/ctakes-config" on all machines has 777 permissions

	$ scp -r /tmp/ctakes_config <user_name>@<server_name>:/tmp/


## Running the Pig Scripts on Sample Data

To create an area in which we can stage our Pig scripts and dependent Jars, we are going to create a pig directory and copy in our scripts and jars to it.

	$ mkdir ~/pig
	$ cd ~/pig
	$ cp ~/src/hadoop2_ctakes/pig/* .
	$ chmod 755 *.sh
	$ cp ~/src/ctakes-misc/target/ctakes-misc-4.0.0-jar-with-dependencies.jar .
	
	
## Running the Pig Scripts on Sample Data
First convert all the small files into a smaller set of sequence files

	$ cd ~/pig
	$ export NO_OF_REDUCERS=0
	$ export FILE_SPLIT_SIZE=40000
	$ ./convert_to_sequence_files.sh ./sample_data_txt ./sample_data_seq $NO_OF_REDUCERS $FILE_SPLIT_SIZE
	$ ./process_ctakes_hive.sh ./sample_data_seq/ default.ctakes_annotated_docs_dummy default.ctakes_annotated_docs



The pig job will run to completion and let you know that 10 records were written.  Now we'll make sure everything looks as it should, and confirm that the pages were parsed and placed in our wikipedia_pages table.

	$ hive -e 'select cuis,annotations from default.ctakes_annotations_docs'
	…
	<cuis and annotations here>
	Time taken: 11.106 seconds, Fetched: 10 row(s)


