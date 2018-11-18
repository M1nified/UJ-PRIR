import java.util.Random;

public class CallbackTasksFabric {
	private final static int TASKS = 150;
	private final static int MIN_CALLBACKs = 5; // co najmnie tyle prob wyslania
												// odpowiedzi
	private final static int DELTA_CALLBACKs = 15; // plus troche losowo

	private final static Random rnd = new Random();
	private static int tasksCounter = TASKS;

	public static CallbackTaskConfig create() {
		if (tasksCounter == 0) {
			tasksCounter = TASKS; // przygotowania nastepnej porcji bedzie
									// mozliwe
			return null;
		}
		tasksCounter--;

		return new CallbackTaskConfig(rnd.nextBoolean(), MIN_CALLBACKs + rnd.nextInt(DELTA_CALLBACKs));
	}
}
