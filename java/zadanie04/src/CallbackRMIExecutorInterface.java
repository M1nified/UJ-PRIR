import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfejs zdalnej uslugi wywolan zwrotnych
 * @author oramus
 *
 */
public interface CallbackRMIExecutorInterface extends Remote {
	/**
	 * Metoda wykonujaca zdalna usluge callback.
	 * 
	 * @throws RemoteException
	 */
	void callback() throws RemoteException;
}
