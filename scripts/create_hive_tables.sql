drop table if exists ctakes_annotations_docs_dummy;
drop table if exists ctakes_annotations_docs;

CREATE TABLE ctakes_annotations_docs_dummy(fname STRING, part STRING, parsed BOOLEAN, text STRING, annotations STRING, cuis STRING) PARTITIONED BY (loaded STRING) STORED AS SEQUENCEFILE;
CREATE TABLE ctakes_annotations_docs(fname STRING, part STRING, parsed BOOLEAN, text STRING, annotations STRING, cuis STRING) PARTITIONED BY (loaded STRING) STORED AS SEQUENCEFILE;
