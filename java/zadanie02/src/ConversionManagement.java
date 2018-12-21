import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

class ConversionManagement implements ConversionManagementInterface {

	private static final boolean DEBUG = false;

	private class ConversionJob implements Runnable {

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
			if (DEBUG)
				System.out.println(data.id() + " " + result);
			currentTasksCount.decrementAndGet();
			synchronized (managerIdlingMonitor) {
				managerIdlingMonitor.notify();
			}
		}

	}

	private class ConversionResultKeeper {

		public ConverterInterface.DataPortionInterface input;
		public long result;

		public ConversionResultKeeper(ConverterInterface.DataPortionInterface input, long result) {
			this.input = input;
			this.result = result;
		}

	}

	private class DataPortionPriorityComparator implements Comparator<ConversionJob> {

		@Override
		public int compare(ConversionJob o1, ConversionJob o2) {
			if (o1.getData().id() == o2.getData().id()) {
				return 1;
			}
			return o1.getData().id() - o2.getData().id();
		}

	}

	private class Manager extends Thread {

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
				if (DEBUG)
					System.out.println("SENT " + left.input.id());
				currentResultId++;
				return true;
			}
			return false;
		}

	}

	private Manager manager = new Manager();

	private AtomicInteger currentTasksCount = new AtomicInteger(0);

	private ExecutorService executor = Executors.newFixedThreadPool(1);
	private ConverterInterface converter = null;
	private ConversionReceiverInterface conversionReceiver = null;

	private ConcurrentSkipListSet<ConversionJob> dataQueue = new ConcurrentSkipListSet<ConversionJob>(
			new DataPortionPriorityComparator());

	private int cores = 1;

	private ConcurrentHashMap<Integer, ConversionResultKeeper> leftResults = new ConcurrentHashMap<Integer, ConversionResultKeeper>();
	private ConcurrentHashMap<Integer, ConversionResultKeeper> rightResults = new ConcurrentHashMap<Integer, ConversionResultKeeper>();

	private final static Object managerIdlingMonitor = new Object();

	public ConversionManagement() {
		manager.start();
	}

	@Override
	public void setCores(int cores) {
		this.cores = cores;
		if (DEBUG)
			System.out.println("setCores: " + cores);
		executor.shutdownNow();
		executor = Executors.newFixedThreadPool(cores);
	}

	@Override
	public void setConverter(ConverterInterface converter) {
		this.converter = converter;
	}

	@Override
	public void setConversionReceiver(ConversionReceiverInterface receiver) {
		this.conversionReceiver = receiver;
	}

	@Override
	public void addDataPortion(ConverterInterface.DataPortionInterface data) {
		this.dataQueue.add(new ConversionJob(data));
		synchronized (managerIdlingMonitor) {
			managerIdlingMonitor.notify();
		}
	}
}
