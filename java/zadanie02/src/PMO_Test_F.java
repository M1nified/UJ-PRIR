import java.time.Duration;

/**
 * Klasa testuje problem braku danych. Oczekuje się, że wszystkie konwersje
 * zostaną wykonane, ale nie wszystkie wyniki dostarczone.
 */
public class PMO_Test_F extends PMO_Test_A {

    protected static final int RESULTS_EXPECTED = 1;
    protected static final int DATA_PORTIONS_SENDERS = 1;
    protected static final int DATA_PORTIONS_PER_SENDER = 1000;
    protected static final int MAX_CORES = 1;

    @Override
    public long getRequiredTime() {
        return 10000;
    }

    protected void prepareInitialDataPortions() {
        generator.add(1, 1); // można dostarczyć wynik tylko jednej konwersji
        generator.add(10, 999);
    }

    protected void prepareSenders() {
        prepareSenders(DATA_PORTIONS_PER_SENDER, DATA_PORTIONS_SENDERS);
    }

    protected boolean parametricTest() {
        return parametricTest(MAX_CORES, RESULTS_EXPECTED);
    }

    protected void prepareConversionManagement() {
        prepareConversionManagement(MAX_CORES);
    }

    protected void prepareTestEnvironment() {
        prepareTestEnvironment(MAX_CORES);
    }

    @Override
    public boolean testOK() {

        ProcessHandle.Info info = ProcessHandle.current().info();
        Duration duration = info.totalCpuDuration().get();
        long seconds = duration.getSeconds();
        PMO_SystemOutRedirect.println("Total CpuDuration = " + seconds + "s");

        if (seconds > 12) {
            error("BŁĄD: zbyt duże użycie CPU");
            return false;
        }

        return true;
    }
}