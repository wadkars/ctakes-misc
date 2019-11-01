
## Preliminary steps

* Create a fake folder to allow the Files Collection Reader to get initialized by executing the following command  `mkdir /tmp/random/` 
* Create a folder `mkdir /tmp/cTakesExample/cData` . Put all the input files into this folder
* Create a folder `mkdir /tmp/ctakes-config` and copy all the contents of "./resources/" into this folder
* Create a folder `mkdir /tmp/ctakes-config2`
* Run the following command

`
mvn exec:java -Dexec.mainClass="org.apache.ctakes.pipelines.RushEndToEndPipeline" -Dexec.args="--input-dir /tmp/cTakesExample/cData --output-dir /tmp/cTakesExample/ --masterFolder /tmp/ctakes-config/ --tempMasterFolder /tmp/ctakes-config2/" true
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
	$ mvn clean install
	$ mkdir /opt/pig
	$ cp ./target/ctakes-misc-4.0.0-jar-with-dependencies.jar /opt/pig/
	$ cp ./scripts/* /opt/pig/
	
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
	$ hdfs dfs -put ~/src/ctakes-misc/sample_data/data/* ./sample_data_txt
	$ hdfs dfs -ls ./sample_data_txt

## Create the following folders on all datanodes in the cluster
All of the folders below must have 777 permissions set on them on each data node

	$ mkdir /tmp/random
	$ mkdir /tmp/ctakes-config
	$ mkdir /logs/ctakes-config
	

	
1. Folder 1 is needed because the Files Collections Reader used inside the source code needs a folder to look into. It is a dummy folder. Leave it empty. Filling it with a lot of files slows down the process as the Reader 
lists all the files.

2. Folder 2 is the folder where all the config files for LVG Annotator and the Lookup Annotators are placed. These should be copied from ./resources/ folder of the project. Simply copy the entire folder contents into the /tmp/ctakes-config folder. Copy this from the edge-node /tmp/ctakes-config in the installation steps above.

3. Folder 3 should be created on a disk with enough space. This is the folder in which each Pig Task creates a subfolder (with a randomly generated number) and copies all the contents of the /tmp/ctakes-config into. When the Pig Task finishes it cleans up the sub-folder created. This is needed because the HSQLDB stored in the lookup dictionary paths creates a lock file which cannot be reused and multiple pig tasks running on the same data-node endup failing on this lock file if we use the /tmp/ctakes-config folder.

## Changing the custom dictionary

1. The Dictionary in maintained in the ${PROJECT_HOME}/resources/lookupdict/sno_rx_16ab/sno_rx_16ab.script . Also note the .properties file with the same name in the same folder

2. It is referenced from ${PROJECT_HOME}/resources/sno_rx_16ab-test.xml. If you create another similar script file, remember to change the references in this XML

3. Also remember to copy the contents of the ${PROJECT_HOME}/resources/ folder into /tmp/ctakes-config on all the nodes after doing that

## Changing the Pipeline

1. Most of the pipeline configuration is in the class RushEndToEndPipeline.java. The method is getXMIWritingPreprocessorAggregateBuilder().

2. The CUIS extraction happens in the following two classes:
   a. RushSimplePipeline.java
   b. CuisWriter.java



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
First convert all the small files into a smaller set of sequence files. Followed by consuming the sequence files and writing them to HIVE. The parameters passed to hive process are

1. Location of the sequence file
2. Dummy HIVE Table
3. Actual HIVE Table
4. Location of the master config files (on all data nodes). This is where the dictionary tables are stored
5. The location on each data node where the master files from step 4 are copied by each Pig Task and deleted when Pig Task finishes
6. Boolean flag which indicates which of the two negation modes are used. "True" implies default and "False" implies the "desc/negation" file is used 

	$ cd ~/pig
	$ export NO_OF_REDUCERS=0
	$ export FILE_SPLIT_SIZE=40000
	$ ./convert_to_sequence_files.sh ./sample_data_txt ./sample_data_seq $NO_OF_REDUCERS $FILE_SPLIT_SIZE
	$ ./process_ctakes_hive.sh ./sample_data_seq/ default.ctakes_annotations_docs_dummy default.ctakes_annotations_docs /tmp/ctakes-config /logs/ctakes-config true



The pig job will run to completion and let you know that 10 records were written.  Now we'll make sure everything looks as it should, and confirm that the pages were parsed and placed in our wikipedia_pages table.

	$ hive -e 'select cuis,annotations from default.ctakes_annotations_docs'
	…
	<cuis and annotations here>
	Time taken: 11.106 seconds, Fetched: 10 row(s)

## Additional Considerations

1. The raw files to sequence file generation takes a parameter $NO_OF_REDUCERS. You can leave it as zero and this will produce as many files as the raw files. One of each raw file with multiple paths defined by the split size (default is 40KB)

2. It will help to run it with number of reducers 250 as the default. If you get timeout errors try to break the raw files into batches and run the above process twice, once for each batch.
