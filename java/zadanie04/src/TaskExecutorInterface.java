/**
 * Interfejs lokalnego systemu wykonywania zadan z obsluga wywolan zwrotnych
 * @author oramus
 *
 */
public interface TaskExecutorInterface {
	/**
	 * Metoda zlecania zadan do wykonania przez zdalny serwis oraz 
	 * obslugi wywolan zwrotnych.
	 * 
	 * @param codeToRun kod do wykonania przez serwer
	 * @param callbackCode kod do wykonania jako wywolanie zwrotne
 	 * @param keepCallbackRunning true - serwis callback ma byc utrzymywany bez ograniczen czasowych,
 	 * false - serwis callback ma zostac skasowany po pierwszym uzyciu i system nie moze pozwolic
 	 * na wiecej niz jednokrotne wykonanie kodu przekazanego jako callbackCode.
	 */
	void execute( SerializableRunnableInterface codeToRun, Runnable callbackCode, boolean keepCallbackRunning );
}
