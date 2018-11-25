import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Kod do wykonania przez serwis zdalny
 * 
 * @author oramus
 *
 */
public class RemoteTask extends CallbackTaskConfig implements SerializableRunnableInterface {

	private static final long serialVersionUID = -3202161663978300955L;

	public RemoteTask(CallbackTaskConfig taskConfig) {
		super(taskConfig);
	}

	@Override
	synchronized public void run() {
		TaskRMIExecutor.callbacks.put( this, numberOfExpectedCallbackCalls );
	}

}
