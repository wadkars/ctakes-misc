package com.cloudera.ctakes;
//hadoop jar rush_ctakes-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.rush.mayo.SmallFilesToSequenceFile /user/mayoclinic/RUSH_DATA /user/mayoclinic/RUSH_DATA_10
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
 
public class SmallFilesToSequenceFile extends Configured implements Tool {
	public static final String MAX_SIZE_PARAMETER = "max_size";
	static class SequenceFileMapper extends
			Mapper<NullWritable, BytesWritable, Text, BytesWritable> {
		private Text filename;
		int maxSize=10000;
		@Override
		protected void setup(Context context) throws IOException,
				InterruptedException {
			this.maxSize = Integer.parseInt(context.getConfiguration().get(SmallFilesToSequenceFile.MAX_SIZE_PARAMETER));
			InputSplit split = context.getInputSplit();
			Path path = ((FileSplit) split).getPath();
			filename = new Text(path.toString());
		}
 
		@Override
		protected void map(NullWritable key, BytesWritable value,
				Context context) throws IOException, InterruptedException {
			//Break it up here
			
			//if(value.getBytes().length>this.maxSize) {
				
				String txtVal = new String(value.getBytes());
				List<String> splits = FileSplitter.getLines(txtVal, this.maxSize);
				int counter =1;
				for(String s:splits) {
					BytesWritable value2 = new BytesWritable();
					value2.set(s.getBytes(), 0, s.length());		
					context.write(new Text(filename.toString() +"-"+counter), value2);
					counter++;
				}
/*
			}else {
				context.write(filename, value);
			}
*/			
			 
		}
	}
 
	//@Override
	public int run(String[] args) throws Exception {
		
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf);
		
		job.setJarByClass(SmallFilesToSequenceFile.class);
		job.setJobName("smallfilestoseqfile");
		job.setInputFormatClass(FullFileInputFormat.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		
		/*
		job.getConfiguration().set("mapreduce.map.memory.mb", "2000");
		job.getConfiguration().set("mapreduce.reduce.memory.mb", "2000");
		job.getConfiguration().set("mapreduce.map.java.opts.max.heap", "1800");
		job.getConfiguration().set("mapreduce.map.java.opts", "-Xmx1800m");
		job.getConfiguration().set("mapreduce.reduce.java.opts", "-Xmx1800m");
		job.getConfiguration().set("mapreduce.job.heap.memory-mb.ratio", "0.8");
		job.getConfiguration().set("mapreduce.task.timeout", "21600000");
		*/
		//job.setNumReduceTasks(0);
		String[] args2 = new GenericOptionsParser(getConf(), args)
                .getRemainingArgs();
		job.setNumReduceTasks(Integer.parseInt(args2[2]));
		job.getConfiguration().set(MAX_SIZE_PARAMETER,args[3]);
		FileInputFormat.setInputPaths(job, new Path(args2[0]));
	    FileOutputFormat.setOutputPath(job, new Path(args2[1]));
	    
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(BytesWritable.class);
		job.setMapperClass(SequenceFileMapper.class);
		return job.waitForCompletion(true) ? 0 : 1;
	}
 
	public static void main(String[] args) throws Exception {
		int exitCode = ToolRunner.run(new SmallFilesToSequenceFile(), args);
		System.exit(exitCode);
	}
}