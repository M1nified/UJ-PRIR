import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

public class TaskRMIExecutor extends UnicastRemoteObject implements TaskRMIExecutorInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6698491225443017835L;

	// powiazanie kodu z oczekiwna liczba wywolan zwrotnych
	static ConcurrentHashMap<SerializableRunnableInterface, Integer> callbacks = new ConcurrentHashMap<>();

	@Override
	public void execute(final SerializableRunnableInterface codeToRun, final String callbackServiceName)
			throws RemoteException {
		codeToRun.run();

		// odszukac serwis

		int callbacksToSend = callbacks.get(codeToRun);
		// PMO_SystemOutRedirect.println( "Liczba callbacks do wygenerowania: "
		// + callbacks.get( codeToRun ));

		// przygotowac i uruchomic watki wg. konfiguracji zadania

		CyclicBarrier cb = new CyclicBarrier(callbacksToSend); // wszystkie
																// jednoczesnie!

		class CallbackCall implements Runnable {
			CyclicBarrier cb;

			CallbackCall(CyclicBarrier cb) {
				this.cb = cb;
			}

			@Override
			public void run() {
				try {
					cb.await();
					CallbackRMIExecutorInterface callbackService = Narrow.find(callbackServiceName);
					if (callbackService != null)
						callbackService.callback();
				} catch (Exception e) {
//					e.printStackTrace();
				} 
			}
		}

		Thread th = null;
		for (int i = 0; i < callbacksToSend; i++) {
			th = new Thread(new CallbackCall(cb));
			th.setDaemon(true);
			th.start();
		}

	}

	public TaskRMIExecutor() throws RemoteException {
		super(0);
	}

	public static void main(String[] args) throws Exception {
		PMO_SystemOutRedirect.startRedirectionToNull();
		TaskRMIExecutor tre = new TaskRMIExecutor();

		java.rmi.Naming.rebind("SERVER", tre);
	}

}
