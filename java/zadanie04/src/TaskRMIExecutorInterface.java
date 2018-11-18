import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfejs zdalnego systemu wykonywania kodu z obsluga
 * wywolan zwrotnych
 * 
 * @author oramus
 *
 */
public interface TaskRMIExecutorInterface extends Remote {
	/**
	 * Metoda wykonywania zdalnego kodu z powiadomieniem zwrotnym.
	 * 
	 * @param codeToRun
	 *            kod do wykonania
	 * @param callbackServiceName
	 *            nazwa serwisu udostepniajacego usluge opisana przez interfejs
	 *            CallbackRMIExecutorInterface
	 * @throws RemoteException
	 *             blad wykonania
	 */
	void execute(SerializableRunnableInterface codeToRun, String callbackServiceName) throws RemoteException;
}
