import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class ConversionManagement implements ConversionManagementInterface {

	class ConversionJob implements Runnable {

		ConverterInterface.DataPortionInterface data;

		public ConverterInterface.DataPortionInterface getData() {
			return data;
		}

		public ConversionJob(ConverterInterface.DataPortionInterface data) {
			this.data = data;
		}

		@Override
		public void run() {
			long result = converter.convert(data);
			if (data.channel() == ConverterInterface.Channel.LEFT_CHANNEL) {
				leftResults.put(data.id(), new ConversionResultKeeper(data, result));
			} else {
				rightResults.put(data.id(), new ConversionResultKeeper(data, result));
			}
			System.out.println(data.id() + " " + result);
			synchronized (nothingToSendMonitor) {
				nothingToSendMonitor.notify();
			}
			currentTasksCount.decrementAndGet();
			synchronized (workersFullMonitor) {
				workersFullMonitor.notify();
			}
			synchronized (managerIdlingMonitor) {
				managerIdlingMonitor.notify();
			}
		}

	}

	class ConversionWorker implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			ConversionJob cj;
			while (!Thread.currentThread().isInterrupted()) {
				while ((cj = dataQueue.pollFirst()) != null) {
					cj.run();
				}
				try {
					synchronized (emptyDataQueueMonitor) {
						emptyDataQueueMonitor.wait();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println("ConversionWorker SHUTDOWN");
		}

	}

	class ConversionJobFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable r) {
			System.out.println("ConversionJobFactory/newThread:\n" + r);
			return new Thread(r);
		}

	}

	class ConversionResultKeeper {

		public ConverterInterface.DataPortionInterface input;
		public long result;

		public ConversionResultKeeper(ConverterInterface.DataPortionInterface input, long result) {
			this.input = input;
			this.result = result;
		}

	}

	class DataPortionPriorityComparator implements Comparator<ConversionJob> {

		@Override
		public int compare(ConversionJob o1, ConversionJob o2) {
			if (o1.getData().id() == o2.getData().id()) {
				return 1;
			}
			return o1.getData().id() - o2.getData().id();
		}

	}

	class Sender extends Thread {

		int currentResultId = 1;

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				while (this.send())
					;
				try {
					synchronized (nothingToSendMonitor) {
						nothingToSendMonitor.wait();
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					e.printStackTrace();
				}
			}
		}

		private boolean send() {
			ConversionResultKeeper left;
			ConversionResultKeeper right;
			if ((left = leftResults.get(currentResultId)) != null
					&& (right = rightResults.get(currentResultId)) != null) {
				ConversionResult result = new ConversionResult(left.input, right.input, left.result, right.result);
				conversionReceiver.result(result);
				System.out.println("SENT " + left.input.id());
				currentResultId++;
				return true;
			}
			return false;
		}

	}

	class Assigner extends Thread {
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				while (submit())
					;
				try {
					synchronized (workersFullMonitor) {
						workersFullMonitor.wait();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		boolean submit() {
			ConversionJob cj;
			if (currentTasksCount.get() < cores && (cj = dataQueue.pollFirst()) != null) {
				currentTasksCount.getAndIncrement();
				executor.submit(cj);
				return true;
			}
			return false;
		}
	}
	
	class Manager extends Thread{

		int currentResultId = 1;
		
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				while (submit() || send())
					;
				try {
					synchronized (managerIdlingMonitor) {
						managerIdlingMonitor.wait();
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					e.printStackTrace();
				}
			}
		}

		boolean submit() {
			ConversionJob cj;
			if (currentTasksCount.get() < cores && (cj = dataQueue.pollFirst()) != null) {
				currentTasksCount.getAndIncrement();
				executor.submit(cj);
				return true;
			}
			return false;
		}

		private boolean send() {
			ConversionResultKeeper left;
			ConversionResultKeeper right;
			if ((left = leftResults.get(currentResultId)) != null
					&& (right = rightResults.get(currentResultId)) != null) {
				ConversionResult result = new ConversionResult(left.input, right.input, left.result, right.result);
				conversionReceiver.result(result);
				System.out.println("SENT " + left.input.id());
				currentResultId++;
				return true;
			}
			return false;
		}
		
	}

	Sender sender = new Sender();
	Assigner assigner = new Assigner();
	Manager manager = new Manager();

	AtomicInteger currentTasksCount = new AtomicInteger(0);

	ExecutorService executor = Executors.newFixedThreadPool(1);
	ConverterInterface converter = null;
	ConversionReceiverInterface conversionReceiver = null;

	ConcurrentSkipListSet<ConversionJob> dataQueue = new ConcurrentSkipListSet<ConversionJob>(
			new DataPortionPriorityComparator());
	ConversionJobFactory conversionJobFactory = new ConversionJobFactory();

	int cores = 1;

	ConcurrentHashMap<Integer, ConversionResultKeeper> leftResults = new ConcurrentHashMap<Integer, ConversionResultKeeper>();
	ConcurrentHashMap<Integer, ConversionResultKeeper> rightResults = new ConcurrentHashMap<Integer, ConversionResultKeeper>();

	private final static Object emptyDataQueueMonitor = new Object();
	private final static Object nothingToSendMonitor = new Object();
	private final static Object workersFullMonitor = new Object();
	private final static Object managerIdlingMonitor = new Object();

	public ConversionManagement() {
//		sender.start();
//		assigner.start();
		manager.start();
	}

	@Override
	public void setCores(int cores) {
		this.cores = cores;
		System.out.println("setCores: " + cores);
		executor.shutdownNow();
		executor = Executors.newFixedThreadPool(cores, conversionJobFactory);
//		for (int i = 0; i < cores; i++)
//			executor.submit(new ConversionWorker());
	}

	@Override
	public void setConverter(ConverterInterface converter) {
		// TODO Auto-generated method stub
		this.converter = converter;
	}

	@Override
	public void setConversionReceiver(ConversionReceiverInterface receiver) {
		// TODO Auto-generated method stub
		this.conversionReceiver = receiver;
	}

	@Override
	public void addDataPortion(ConverterInterface.DataPortionInterface data) {
		this.dataQueue.add(new ConversionJob(data));
		synchronized (emptyDataQueueMonitor) {
			emptyDataQueueMonitor.notify();
		}
		synchronized (workersFullMonitor) {
			workersFullMonitor.notify();
		}
		synchronized (managerIdlingMonitor) {
			managerIdlingMonitor.notify();
		}
	}
}
