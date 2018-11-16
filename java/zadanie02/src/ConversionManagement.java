import java.util.ArrayList;
import java.util.Comparator;
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
		
		public Worker(){
		}

		@SuppressWarnings("unused")
		public Worker(int id){
			this.id = id;
		}
		
		public boolean killed(){
			if(Thread.currentThread().isInterrupted()){
				return true;
			}
			if(info.cores < workers.size()){						
				synchronized(lockWorkerKill){
					if(info.cores < workers.size()){
						Thread.currentThread().interrupt();
						workers.remove(this);
						return true;											
					}
				}
			}
			return false;
		}
		
		@Override
		public void run(){
			ConverterInterface.DataPortionInterface data = null;
			while(!this.killed()){
				while(!this.killed() && (data = dataPortions.pollFirst()) != null){	
					long converted = info.converter.convert(data);
					results.add(new Result(data, converted));
					synchronized (collectorMonitor) {
						collectorMonitor.notify();											
					}
				}				
				try {
					synchronized(workerMonitor){
						workerMonitor.wait();						
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
			while(!Thread.currentThread().isInterrupted()){
				while(trySend());
				try {
					synchronized (senderMonitor) {
						senderMonitor.wait();						
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Thread.currentThread().interrupt();
					e.printStackTrace();
				}
			}
		}
		
	    private boolean trySend(){
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
				info.receiver.result(result);
				currentResultId++;
				return true;
			}
			return false;
	    }
	    
	}
	
	private class Collector extends Thread{
		@Override
		public void run(){
			while(!Thread.currentThread().isInterrupted()){
				Result result = null;
				while((result = results.poll()) != null){
					int id = result.in.id();
					if(result.in.channel() == ConverterInterface.Channel.LEFT_CHANNEL){
						leftRes[id] = result;
					} else {
						rightRes[id] = result;
					}
					synchronized (senderMonitor) {
						senderMonitor.notify();						
					}
				}
				try {
					synchronized (collectorMonitor) {
						collectorMonitor.wait();						
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Thread.currentThread().interrupt();
					e.printStackTrace();
				}
			}
		}
	}
	
	private class DataPortionComparator implements Comparator<ConverterInterface.DataPortionInterface> {
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
    private List<Collector> collectors = new ArrayList<Collector>(); 

    private final Object workerMonitor = new Object();
    private final Object lockWorkerKill = new Object();
    private final Object collectorMonitor = new Object();
    private final Object senderMonitor = new Object();
    
    final private List<Worker> workers = new ArrayList<Worker>();
    final private ConcurrentSkipListSet<ConverterInterface.DataPortionInterface> dataPortions = new ConcurrentSkipListSet<ConverterInterface.DataPortionInterface>(new DataPortionComparator());
    final private ConcurrentLinkedQueue<Result> results = new ConcurrentLinkedQueue<Result>();

    final private Result[] leftRes = new Result[1000];
    final private Result[] rightRes = new Result[1000];
    
    public ConversionManagement(){
    	this.sender = new Sender();
    	this.sender.start();
    	for(int i=0; i<2; i++){
    		Collector collector = new Collector();
    		this.collectors.add(collector);
    		collector.start();    		
    	}
    }
    
    @Override
    public void finalize() throws Throwable{
    	try{
    		
    		while(dataPortions.size() > 0);
    		for(Worker worker : this.workers){
    			worker.interrupt();
    		}
    		for(Worker worker : this.workers){
    			worker.join();
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
//    		super.finalize();
    	}
    }
    
    public void setCores(int cores){
    	this.info.cores = cores;
    	if(workers.size() < cores){
			new Thread(new Runnable(){
				@Override
				public void run(){
					while(workers.size() < info.cores){
						Worker w = new Worker();
						w.start();
						workers.add(w);
					}
				}
			}).start();
		}
    }

    public void setConverter(ConverterInterface converter){
        this.info.converter = converter;
    }

	public void setConversionReceiver(ConversionReceiverInterface receiver){
        this.info.receiver = receiver;
    }

    public void addDataPortion(ConverterInterface.DataPortionInterface data){
	dataPortions.add(data);	
	try {
		synchronized(workerMonitor){
			workerMonitor.notify();     			
		}
	} catch (Exception e) {
		e.printStackTrace();
	}	
    }
    
}
