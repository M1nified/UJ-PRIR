package concurrentSkipListSet_Benchmark;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListSet;

class BenchmarkedConcurrentSkipListSet extends ConcurrentSkipListSet<Integer> {

	private class BenchmarkData{
		private long timeStart;
		private long elapsedTime;
		private Integer elementValue; 
		private String actionName;
		public BenchmarkData(Integer elementValue, String actionName) {
			this.elementValue = elementValue;
			this.actionName = actionName;
		}
		void start() {
			timeStart = System.nanoTime();
		}
		void stop() {
			elapsedTime = System.nanoTime() - timeStart;
		}
		@Override
		public String toString() {
			
			return this.getClass().getSimpleName() + "\t" + actionName + "\t" + elementValue + "\t" + elapsedTime;  
		}
	}
	
	private static final long serialVersionUID = 1L;
	
	private LinkedList<BenchmarkData> benchmarkDatas = new LinkedList<>(); 

	public static void main(String[] args) {
		BenchmarkedConcurrentSkipListSet listSet = new BenchmarkedConcurrentSkipListSet();
		for(int i = 0; i<100000; i++) {
			listSet.add((i + 10) * 2);
		}
		
		listSet.add(10000000);
		listSet.add(0);
		listSet.add(-10000000);
		listSet.add(10000);
		
		Random generator = new Random(); 
		
		for(int i = 0; i< 1000; i++) {
			int tmp = generator.nextInt(2000000) - 1000000;
			listSet.add(tmp);			
		}
		
		listSet.add(10000000);
		listSet.add(0);
		listSet.add(-20000000);
		listSet.add(10000);
		
		listSet.printBenchmarks();
	}
	
	public void printBenchmarks() {
		for(BenchmarkData benchmarkData: benchmarkDatas) {
			System.out.println(benchmarkData);
		}
	}
	
	@Override
	public boolean add(Integer e) {
		// TODO Auto-generated method stub
		BenchmarkData benchmarkData = new BenchmarkData(e, "add");
		benchmarkData.start();
		boolean result = super.add(e);
		benchmarkData.stop();
		benchmarkDatas.add(benchmarkData);
		return result;
	}

}
