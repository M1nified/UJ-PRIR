import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class PMO_Start {
	private TaskExecutorInterface tei;

	public PMO_Start() {
		try {
			tei = new TaskExecutor();
		} catch (Exception e) {
			e.printStackTrace();
			PMO_SystemOutRedirect.println("Nie udalo sie utworzyc obiektu TaskExecutor");
			System.exit(0);
		}
	}

	private class Pair {
		RemoteTask rt;
		CallbackCode cb;

		Pair(RemoteTask rt, CallbackCode cb) {
			this.rt = rt;
			this.cb = cb;
		}
	}

	private class TaskSubmitter implements Runnable {
		private CyclicBarrier cb;
		private Pair codeToSubmit;

		@Override
		public void run() {
			try {
				cb.await();
				tei.execute(codeToSubmit.rt, codeToSubmit.cb, codeToSubmit.rt.keepRunning);
			} catch (InterruptedException | BrokenBarrierException e) {
				PMO_SystemOutRedirect.println("Wystapil wyjatek w trakcie await() lub execute() " + e.getMessage());
			}
		}

		public TaskSubmitter(CyclicBarrier cb, Pair codeToSubmit) {
			this.cb = cb;
			this.codeToSubmit = codeToSubmit;
		}

	}

	private List<Pair> tasks = new ArrayList<>();
	private List<Thread> threads = new ArrayList<>();

	private void prepareTestTasks() {
		while (true) {
			CallbackTaskConfig ctc = CallbackTasksFabric.create();
			if (ctc == null)
				return;
			tasks.add(new Pair(new RemoteTask(ctc), new CallbackCode(ctc)));
		}
	}

	private void prepareAndStartThreads() {
		PMO_SystemOutRedirect.println("Uruchamiamy " + tasks.size() + " watkow");
		CyclicBarrier cb = new CyclicBarrier(tasks.size());
		for (Pair tp : tasks) {
			Thread th = new Thread(new TaskSubmitter(cb, tp));
			th.setDaemon(true);
			th.start();
			threads.add(th);
		}
	}

	private void join(long maxWait) {
		long start = 0;
		for (Thread th : threads) {
			start = System.currentTimeMillis();
			do {
				try {
					th.join(500);
				} catch (InterruptedException e) {
					PMO_SystemOutRedirect.println("W trakcie join() wystapil wyjatek " + e.getLocalizedMessage());
				}
			} while (th.isAlive() && ((System.currentTimeMillis() - start) < maxWait));
		}
		PMO_SystemOutRedirect.println("Koniec join");
		if ((System.currentTimeMillis() - start) > maxWait) {
			PMO_SystemOutRedirect
					.println("Przekroczono limit czasu ( " + maxWait + " msec) przeznaczonego na wykonanie testu");
		}

	}

	private boolean test() {
		boolean result = true;

		for (Pair p : tasks) {
			result &= p.cb.test();
		}

		return result;
	}

	public static void main(String[] args) {
		PMO_SystemOutRedirect.startRedirectionToNull();
		int reps = 10;
		boolean result = false;
		int i;
		for (i = 0; i < reps; i++) {
			PMO_Start start = new PMO_Start();

			start.prepareTestTasks();
			start.prepareAndStartThreads();
			start.join(3000); // czas na odebranie zadan

			TimeHelper.sleep(12500); // ekstra czas na odeslanie odpowiedzi

			result = start.test();
			if (!result) {
				PMO_SystemOutRedirect.println("Dalszy testu nie ma juz sensu...");
				Verdict.show(false);
				break;
			}
		}

		if (i == reps) {
			PMO_SystemOutRedirect.println("Test wykonano " + reps + "x");
			Verdict.show(result);
		}

		PMO_SystemOutRedirect.println("");
		PMO_SystemOutRedirect.println("------  THE END  ------");
		PMO_SystemOutRedirect.println("");

		System.exit(0);
		java.lang.Runtime.getRuntime().halt(0);

	}

}
