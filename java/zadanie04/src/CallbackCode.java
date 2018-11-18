
public class CallbackCode extends CallbackTaskConfig implements Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8653070529331074005L;
	private AtomicCounter ac = new AtomicCounter();

	public CallbackCode(CallbackTaskConfig taskConfig) {
		super(taskConfig);
		if ( keepRunning ) { // serwis ma byc podtrzymany
			ac.setFailPredicate((i) -> (i != super.numberOfExpectedCallbackCalls));
			ac.setOKPredicate((i) -> (i == super.numberOfExpectedCallbackCalls));			
		} else { // serwis ma miec tylko jedno wykonanie
			ac.setFailPredicate((i) -> (i != 1 ));
			ac.setOKPredicate((i) -> (i == 1 ));
		}
	}

	@Override
	public void run() {
		ac.inc();
	}

	boolean test() {
		if (ac.isFail().get()) {
			if ( super.keepRunning ) {
				PMO_SystemOutRedirect.println(
						"BLAD: dla serwisu z keepRunning=true; spodziewano sie " + super.numberOfExpectedCallbackCalls + " wywolan, a bylo " + ac.get());
				
			} else {
				PMO_SystemOutRedirect.println(
						"BLAD: dla serwisu z keepRunning=false; spodziewano sie jednego wywolania, a naliczono ich " + ac.get());				
			}
			return false;
		}
		return true;
	}

}
