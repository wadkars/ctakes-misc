
##Preliminary steps

* Create a fake folder to allow the Files Collection Reader to get initialized by executing the following command  `mkdir /tmp/random/` 
* Create a folder `mkdir /tmp/cTakesExample/cData` . Put all the input files into this folder
* Run the following command

`
mvn exec:java -Dexec.mainClass="org.apache.ctakes.pipelines.RushEndToEndPipeline" -Dexec.args="--input-dir /tmp/cTakesExample/cData --output-dir /tmp/cTakesExample/ --lookupXml ./resources/sno_rx_16ab-local.xml"
`
* This will produce two folder /tmp/cTakesExample/xmis and /tmp/cTakesExample/cuis/ and place the respect XMI and CUI's into it.