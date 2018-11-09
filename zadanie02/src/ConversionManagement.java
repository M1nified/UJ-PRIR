import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConversionManagement implements ConversionManagementInterface {

	private class Info {
		public int cores = 0;
	    public ConverterInterface converter;
	    private ConversionReceiverInterface receiver;
	}
	
	private class Worker extends Thread{
		
		private int id;
		
		public Worker(int id){
			this.id = id;
		}
		
		@Override
		public void run(){
			ConverterInterface.DataPortionInterface data = null;
			while(!Thread.currentThread().isInterrupted()){
				while((data = dataPortions.poll()) != null && !Thread.currentThread().isInterrupted()){	
					long converted = info.converter.convert(data);
					if(data.channel() == ConverterInterface.Channel.LEFT_CHANNEL){
						mapLeftIn.put(data.id(), data);
						mapLeftOut.put(data.id(), converted);
					} else {
						mapRightIn.put(data.id(), data);
						mapRightOut.put(data.id(), converted);
					}
//					System.out.println(id + " " + converted);
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
			if(data != null)
				dataPortions.add(data);
		}
	}
	
	private class Sender extends Thread{
		
		private int currentResultId = 1;
		
		@Override
		public void run(){
	    	System.out.println("sender");
			while(!Thread.currentThread().isInterrupted()){
				trySend();					
			}
		}
		
	    private void trySend(){
//	    	System.out.println(currentResultId);
			if(		mapLeftOut.containsKey(currentResultId) 
					&& mapLeftIn.containsKey(currentResultId) 
					&& mapRightOut.containsKey(currentResultId) 
					&& mapRightIn.containsKey(currentResultId)
			){
				ConversionManagementInterface.ConversionResult result = new ConversionManagementInterface.ConversionResult(
						mapLeftIn.get(currentResultId),
						mapRightIn.get(currentResultId),
						mapLeftOut.get(currentResultId),
						mapRightOut.get(currentResultId)
						);
//		    	System.out.println(currentResultId + " sent");
				info.receiver.result(result);
				this.currentResultId++;
			}
	    }
	    
	}
	
    
	private class WorkersRunner extends Thread{
		
		@Override
		public void run(){
			while(!Thread.currentThread().isInterrupted()){
				if(info.cores != workers.size()){
					if(workers.size() < info.cores){
						while(workers.size() < info.cores){
							Worker w = new Worker(workers.size());
							w.start();
							workers.add(w);
						}
			    	} else {
			    		List<Worker> removed = new LinkedList<Worker>();
			    		while(workers.size() > info.cores){
			    			Worker w = workers.remove(workers.size() - 1);
			    			w.interrupt();
			    			removed.add(w);
			    		}
//						for(Worker worker : removed){
//							try {
//								worker.join();
//							} catch (InterruptedException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//						}
			    	}
				}
			}
		}
		
	}
	
	final private Info info = new Info();
    private Sender sender;
    private WorkersRunner workersRunner;

    private final Object lockWorker = new Object();
    private final Object lockSender = new Object();
    private final Object lockWorkersRunner = new Object();
    
    final private List<Worker> workers = new ArrayList<Worker>();
    final private ConcurrentLinkedQueue<ConverterInterface.DataPortionInterface> dataPortions = new ConcurrentLinkedQueue<ConverterInterface.DataPortionInterface>();

    final private ConcurrentHashMap<Integer, Long> mapLeftOut = new ConcurrentHashMap<Integer, Long>();
    final private ConcurrentHashMap<Integer, Long> mapRightOut = new ConcurrentHashMap<Integer, Long>();
    final private ConcurrentHashMap<Integer, ConverterInterface.DataPortionInterface> mapLeftIn = new ConcurrentHashMap<Integer, ConverterInterface.DataPortionInterface>();
    final private ConcurrentHashMap<Integer, ConverterInterface.DataPortionInterface> mapRightIn = new ConcurrentHashMap<Integer, ConverterInterface.DataPortionInterface>();
    
    public ConversionManagement(){
    	this.sender = new Sender();
    	this.sender.start();
//    	this.workersRunner = new WorkersRunner();
//    	this.workersRunner.start();
    }
    
    @Override
    public void finalize() throws Throwable{
    	try{
    		for(Worker worker : this.workers){
    			worker.join();
    		}
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
    	this.dataPortions.add(data);
    	try {
    		synchronized(lockWorker){
    			lockWorker.notify();     			
    		}
    	} catch (Exception e) {
			e.printStackTrace();
    	}
    }
    
}