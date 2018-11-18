import java.io.Serializable;

public class CallbackTaskConfig implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8782679515014789546L;
	protected final boolean keepRunning;
	protected final int numberOfExpectedCallbackCalls;
	
	public CallbackTaskConfig( boolean keepRunning, int numberOfExpectedCallbackCalls ) {
		this.keepRunning = keepRunning;
		this.numberOfExpectedCallbackCalls = numberOfExpectedCallbackCalls;
	}
	
	public CallbackTaskConfig( CallbackTaskConfig toCopy ) {
		this( toCopy.keepRunning, toCopy.numberOfExpectedCallbackCalls );
	}
}
