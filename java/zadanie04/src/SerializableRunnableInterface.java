import java.io.Serializable;

/**
 * Interfejs bedacy odpowiednikiem interfejsu Runnable, ale z 
 * wlasnoscia Serializable
 * @author oramus
 *
 */
public interface SerializableRunnableInterface extends Serializable {
	/**
	 * Metoda do wykonania.
	 */
	void run();
}
