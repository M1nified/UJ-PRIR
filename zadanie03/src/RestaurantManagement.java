import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class RestaurantManagement implements RestaurantManagementInterface {
	
	private final static boolean DEBUG = false;

	private class Receiver implements ReceiverInterface{

		@Override
		public void mealReady(int orderID) {
			if(DEBUG) System.out.println("Receiver/mealReady " + orderID);
			int tableID = orderTableMap.get(orderID);
			WaiterInterface waiterInterface = null;
			while(( waiterInterface = availableWaiters.poll()) == null);
			ordersInPrep.decrementAndGet();
			orderServigWaiterMap.put(orderID, waiterInterface);
//			waiterExecutorService.submit(new ServingHandler(waiterInterface, orderID, tableID));
			waiterInterface.go(orderID, tableID);
		}
		
	}
	
	private class Order implements OrderInterface{

		@Override
		public void newOrder(int orderID, int tableID) {
			if(DEBUG) System.out.println("Order/newOrder " + orderID + " " + tableID);
			orderTableMap.put(orderID, tableID);
//			kitchenExecutorService.submit(new CookingHandler(orderID));
			boolean done = false;
			while(!done){
				boolean runIt = false;
				synchronized (ordersInPrep) {
					if(ordersInPrep.get() < waitersCount.get() && ordersInPrep.get() < kitchen.getNumberOfParallelTasks()){
						ordersInPrep.incrementAndGet();
						runIt = true;
					}
				}
				if(runIt){
					if(DEBUG) System.out.println("CookingHandler/run cook " + orderID);
					kitchen.prepare(orderID);					
					done = true;
				}
			}
		}

		@Override
		public void orderComplete(int orderID, int tableID) {
			if(DEBUG) System.out.println("Order/orderComplete " + orderID + " " + tableID);
			WaiterInterface waiterInterface = orderServigWaiterMap.remove(orderID);
			if(!leavingWaiters.containsKey(waiterInterface.getID())){
				waiters.remove(waiterInterface);
				availableWaiters.add(waiterInterface);				
			}
		}
		
	}
	
	private class CookingHandler implements Runnable{
		
		private int orderID;
		
		public CookingHandler(int orderID) {
			this.orderID = orderID;
		}
		
		@Override
		public void run() {
			if(DEBUG) System.out.println("CookingHandler/run " + orderID);
			boolean done = false;
			while(!done){
				boolean runIt = false;
				synchronized (ordersInPrep) {
					if(ordersInPrep.get() < waitersCount.get() && ordersInPrep.get() < kitchen.getNumberOfParallelTasks()){
						ordersInPrep.incrementAndGet();
						runIt = true;
					}
				}
				if(runIt){
					if(DEBUG) System.out.println("CookingHandler/run cook " + orderID);
					kitchen.prepare(orderID);					
					done = true;
				}
				try {
					cookingMonitor.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Thread.currentThread().interrupt();
					e.printStackTrace();
				}
			}
		}
		
	}
	
	private class ServingHandler implements Runnable{
		
		private WaiterInterface waiterInterface = null;
		private int orderID;
		private int tableID;
		
		public ServingHandler(WaiterInterface waiterInterface, int orderID, int tableID) {
			this.waiterInterface = waiterInterface;
			this.orderID = orderID;
			this.tableID = tableID;
		}

		@Override
		public void run() {
			waiterInterface.go(orderID, tableID);
		}
				
	}
	
	private final Receiver receiver = new Receiver();
	private final Order order = new Order();
	
	private KitchenInterface kitchen = null;
	private ExecutorService kitchenExecutorService = null;
	private ExecutorService waiterExecutorService = null;
	
	private final LinkedList<WaiterInterface> waiters = new LinkedList<>();

	private final ConcurrentHashMap<Integer, Integer> orderTableMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, WaiterInterface> orderServigWaiterMap = new ConcurrentHashMap<>();
	private final BlockingQueue<WaiterInterface> availableWaiters = new LinkedBlockingDeque<>();
	private final ConcurrentHashMap<Integer, WaiterInterface> leavingWaiters = new ConcurrentHashMap<>();

	private final AtomicInteger ordersInPrep = new AtomicInteger(0);
	private final AtomicInteger waitersCount = new AtomicInteger(0);
	
	private final Object cookingMonitor = new Object();
	private final Object cookingCmpsync = new Object();
	
	public RestaurantManagement() {
		waiterExecutorService = Executors.newCachedThreadPool();
	}
	
	@Override
	public void addWaiter(WaiterInterface waiter) {
		if(DEBUG) System.out.println("addWaiter " + waiter.getID());
		waiter.registerOrder(this.order);
		waiters.add(waiter);
		availableWaiters.add(waiter);
		waitersCount.incrementAndGet();
	}

	@Override
	public void removeWaiter(WaiterInterface waiter) {
		if(DEBUG) System.out.println("removeWaiter " + waiter.getID());
		waitersCount.decrementAndGet();
		leavingWaiters.put(waiter.getID(), waiter);
	}

	@Override
	public void setKitchen(KitchenInterface kitchen) {
		if(DEBUG) System.out.println("setKitchen " + kitchen.getNumberOfParallelTasks());
		this.kitchen = kitchen;
		this.kitchen.registerReceiver(this.receiver);
		this.kitchenExecutorService = Executors.newFixedThreadPool(this.kitchen.getNumberOfParallelTasks());
	}

}
