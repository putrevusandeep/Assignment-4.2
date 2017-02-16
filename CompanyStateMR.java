package mapReduce;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class CompanyStateMR {
	
	// first mapper -Company
	public static class CompanyMapper extends Mapper<Object, Text, Text, IntWritable> {

		private final static IntWritable one = new IntWritable(1);
		private Text company = new Text();

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			StringTokenizer itr = new StringTokenizer(value.toString());
			boolean isNA = false;
			while (itr.hasMoreTokens()) {
				String[] parts = itr.nextToken().split("\\|");
				for (int i = 0; i < 2; i++) {
					if (parts[i].equalsIgnoreCase("na")) {
						isNA = true;
					}
				}

				if (!isNA) {
					company.set(parts[0]);
					context.write(company, one);

				}

			}

		}
	}
	
	
	// second mapper -State mapper
	public static class StateMapper extends Mapper<Object, Text, Text, IntWritable> {

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String[] parts = value.toString().split("\\|");
			boolean isNA = false;
			for (int i = 0; i < 2; i++) {
				if (parts[i].equalsIgnoreCase("na")) {
					isNA = true;
				}
			}

			if (!isNA) {
				String product = parts[0];
				String state = parts[3];
				if (product.equalsIgnoreCase("Onida")) {
					context.write(new Text(state), new IntWritable(1));
				}

			}

		}
	}
	
	//
	public static class TokenizerReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
		private IntWritable result = new IntWritable();

		public void reduce(Text company, Iterable<IntWritable> counts, Context con)
				throws IOException, InterruptedException {

			int sum = 0;
			for (IntWritable count : counts) {
				sum = sum + count.get();
			}

			result.set(sum);
			con.write(company, result);

		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "Company Mapper");
		job.setJarByClass(CompanyStateMR.class);
		job.setMapperClass(CompanyMapper.class);
		job.setCombinerClass(TokenizerReducer.class);
		job.setReducerClass(TokenizerReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		Job job1 = Job.getInstance(conf, "State Mapper");
		job1.setJarByClass(CompanyStateMR.class);
		job1.setMapperClass(StateMapper.class);
		job1.setCombinerClass(TokenizerReducer.class);
		job1.setReducerClass(TokenizerReducer.class);
		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(job1, new Path(args[0]));
		FileOutputFormat.setOutputPath(job1, new Path(args[2]));

		int jobStatus = job.waitForCompletion(true) ? 0 : 1;
		if (jobStatus == 0) {
			System.exit(job1.waitForCompletion(true) ? 0 : 1);
		}
	}
}
