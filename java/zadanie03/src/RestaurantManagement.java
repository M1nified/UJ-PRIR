import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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
			waiterInterface.go(orderID, tableID);
		}
		
	}
	
	private class Order implements OrderInterface{

		@Override
		public void newOrder(int orderID, int tableID) {
			if(DEBUG) System.out.println("Order/newOrder " + orderID + " " + tableID);
			orderTableMap.put(orderID, tableID);
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
				availableWaiters.add(waiterInterface);				
			}
		}
		
	}
	
	private final Receiver receiver = new Receiver();
	private final Order order = new Order();
	
	private KitchenInterface kitchen = null;

	private final ConcurrentHashMap<Integer, Integer> orderTableMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, WaiterInterface> orderServigWaiterMap = new ConcurrentHashMap<>();
	private final BlockingQueue<WaiterInterface> availableWaiters = new LinkedBlockingDeque<>();
	private final ConcurrentHashMap<Integer, WaiterInterface> leavingWaiters = new ConcurrentHashMap<>();

	private final AtomicInteger ordersInPrep = new AtomicInteger(0);
	private final AtomicInteger waitersCount = new AtomicInteger(0);
	
	@Override
	public void addWaiter(WaiterInterface waiter) {
		if(DEBUG) System.out.println("addWaiter " + waiter.getID());
		waiter.registerOrder(this.order);
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
	}

}
