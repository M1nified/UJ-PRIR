
public class CountersFactory {

	private static AtomicCounter maxCounter;

	public static AtomicCounter executeOnce() {
		AtomicCounter result = new AtomicCounter();

		result.setFailPredicate(i -> i != 1);
		result.setOKPredicate(i -> i == 1);

		return result;
	}

	public static AtomicCounter neverExecute() {
		AtomicCounter result = new AtomicCounter();

		result.setFailPredicate(i -> i != 0);
		result.setOKPredicate(i -> i == 0);

		return result;
	}

	public static AtomicCounter prepareCommonMaxStorageCounter() {
		maxCounter = new AtomicCounter();
		return maxCounter;
	}

	public static AtomicCounter prepareCounterWithMaxStorageSet() {
		AtomicCounter ac = new AtomicCounter();
		ac.setMaxStorage(maxCounter);
		return ac;
	}
}
