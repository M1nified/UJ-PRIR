import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

public class ConversionManagement implements ConversionManagementInterface {

	private class Info {
		public int cores = 0;
	    public ConverterInterface converter;
	    public ConversionReceiverInterface receiver;
	}
	
	private class Result {
		public ConverterInterface.DataPortionInterface in;
		public long out;
		
		public Result(ConverterInterface.DataPortionInterface in, long out){
			this.in = in;
			this.out = out;
		}
	}
	
	private class Worker extends Thread{
		
		@SuppressWarnings("unused")
		private int id;
		
		public Worker(int id){
			this.id = id;
		}
		
		@Override
		public void run(){
			ConverterInterface.DataPortionInterface data = null;
			while(!Thread.currentThread().isInterrupted()){
				while(!Thread.currentThread().isInterrupted() && (data = dataPortions.pollFirst()) != null){	
					long converted = info.converter.convert(data);
					results.add(new Result(data, converted));
//					System.out.println(id + " " + data.id() + " " + converted);
				}				
				try {
					synchronized(lockWorker){
						lockWorker.wait();						
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
//					e.printStackTrace();
				}
			}
		}
	}
	
	private class Sender extends Thread{
		
		private int currentResultId = 1;
		
		@Override
		public void run(){
//	    	System.out.println("sender");
			while(!Thread.currentThread().isInterrupted()){
				trySend();					
			}
		}
		
	    private void trySend(){
//	    	System.out.println(currentResultId);
			if(		leftRes[currentResultId] != null
					&& rightRes[currentResultId] != null
			){
				Result left = leftRes[currentResultId],
						right = rightRes[currentResultId];
				ConversionManagementInterface.ConversionResult result = new ConversionManagementInterface.ConversionResult(
						left.in,
						right.in,
						left.out,
						right.out
						);
//				synchronized(resCountLock){
//					info.resCount-=2;
//				}
//		    	System.out.println("sent " + currentResultId);
				info.receiver.result(result);
				currentResultId++;
			}
	    }
	    
	}
	
	private class Collector extends Thread{
		@Override
		public void run(){
			Thread.currentThread();
			while(!Thread.interrupted()){
				Result result = null;
				while((result = results.poll()) != null){
					int id = result.in.id();
//					System.out.println("collect " + id);
//					synchronized(resCountLock){
//						info.resCount++;
//					}
					if(result.in.channel() == ConverterInterface.Channel.LEFT_CHANNEL){
						leftRes[id] = result;
//						System.out.println("collected left " + id);
					} else {
						rightRes[id] = result;
//						System.out.println("collected right " + id);
					}
				}
			}
		}
	}
	
	private class Prioritizer extends Thread{
		@Override
		public void run(){
			Thread.currentThread();
			while(!Thread.interrupted()){
				ConverterInterface.DataPortionInterface data = null;
				while((data = dataPortionsIn.poll()) != null){
					dataPortions.add(data);
					try {
			    		synchronized(lockWorker){
			    			lockWorker.notify();     			
			    		}
			    	} catch (Exception e) {
						e.printStackTrace();
			    	}
				}
				try {
					synchronized(lockPrioritizer){
						lockPrioritizer.wait();						
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
//					e.printStackTrace();
				}
			}
			
		}
	}
	
	public class DataPortionComparator implements Comparator<ConverterInterface.DataPortionInterface> {
		  
	    @Override
	    public int compare(ConverterInterface.DataPortionInterface first, ConverterInterface.DataPortionInterface second) {
	    	int diff = first.id() - second.id();
	    	if(diff == 0){
	    		return first.channel() == ConverterInterface.Channel.LEFT_CHANNEL ? -1 : 1;
	    	}
	       return diff;
	    }
	}
	
	final private Info info = new Info();
    private Sender sender;
    private Prioritizer prioritizer;
    private List<Collector> collectors = new ArrayList<Collector>(); 
//    private WorkersRunner workersRunner;

    private final Object lockWorker = new Object();
    private final Object lockPrioritizer = new Object();
    
    final private List<Worker> workers = new ArrayList<Worker>();
    final private ConcurrentLinkedQueue<ConverterInterface.DataPortionInterface> dataPortionsIn = new ConcurrentLinkedQueue<ConverterInterface.DataPortionInterface>();
    final private ConcurrentSkipListSet<ConverterInterface.DataPortionInterface> dataPortions = new ConcurrentSkipListSet<ConverterInterface.DataPortionInterface>(new DataPortionComparator());
    final private ConcurrentLinkedQueue<Result> results = new ConcurrentLinkedQueue<Result>();

    final private Result[] leftRes = new Result[1000];
    final private Result[] rightRes = new Result[1000];
    
    public ConversionManagement(){
    	this.sender = new Sender();
    	this.sender.start();
//    	this.workersRunner = new WorkersRunner();
//    	this.workersRunner.start();
    	for(int i=0; i<2; i++){
    		Collector collector = new Collector();
    		this.collectors.add(collector);
    		collector.start();    		
    	}
    	this.prioritizer = new Prioritizer();
    	this.prioritizer.start();
    }
    
    @Override
    public void finalize() throws Throwable{
    	try{
    		
    		while(dataPortions.size() > 0);
    		for(Worker worker : this.workers){
    			worker.join();
    		}
    		for(Worker worker : this.workers){
    			worker.interrupt();
    		}
    		
    		while(results.size() > 0);
    		for(Collector collector : collectors){
    			collector.interrupt();
    		}
    		for(Collector collector : collectors){
    			collector.join();
    		}
    		
    		this.sender.interrupt();
    		this.sender.join();
    		
    	}catch(Exception e){
    		e.printStackTrace();
    	}finally{
    		super.finalize();
    	}
    }
    
    public void setCores(int cores){
    	if(cores > this.info.cores){
    		while(workers.size() < cores){
    			Worker w = new Worker(workers.size());
    			w.start();
    			workers.add(w);
    		}
    	} else {
    		List<Worker> removed = new LinkedList<Worker>();
    		while(workers.size() > cores){
    			Worker w = workers.remove(workers.size() - 1);
    			w.interrupt();
    			removed.add(w);
    		}
			for(Worker worker : removed){
				try {
					worker.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
    	}
        this.info.cores = cores;
    }

    public void setConverter(ConverterInterface converter){
        this.info.converter = converter;
    }

	public void setConversionReceiver(ConversionReceiverInterface receiver){
        this.info.receiver = receiver;
    }

    public void addDataPortion(ConverterInterface.DataPortionInterface data){
    	this.dataPortionsIn.add(data);
    	try {
    		synchronized(lockPrioritizer){
    			lockPrioritizer.notify();     			
    		}
    	} catch (Exception e) {
			e.printStackTrace();
    	}
    }
    
}