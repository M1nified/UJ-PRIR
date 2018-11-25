import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

class TaskExecutor implements TaskExecutorInterface {
    
    private final static boolean DEBUG = false;
    
    private class CallbackRMIExecutor extends UnicastRemoteObject implements Serializable, CallbackRMIExecutorInterface {

        private Runnable callbackCode;
        private boolean keepCallbackRunning = true;
        private AtomicBoolean shallRun = new AtomicBoolean(true);
        
        private int count = 0;
        
        protected CallbackRMIExecutor(Runnable callbackCode, boolean keepCallbackRunning) throws RemoteException {
            super();
            this.callbackCode = callbackCode;
            this.keepCallbackRunning = keepCallbackRunning;
        }
        
        private static final long serialVersionUID = -5939344516658690964L;

        @Override
        public void callback() throws RemoteException {
            if(shallRun.getAndSet(keepCallbackRunning)) {
                if(DEBUG) System.out.println(count++ + " " + keepCallbackRunning);                
                callbackCode.run();
            }
        }
        
    }
    
    private Registry registry = null;
    private TaskRMIExecutorInterface taskRMIExecutorInterface;
    
    public TaskExecutor() throws RemoteException, NotBoundException {
        registry = LocateRegistry.getRegistry();
        if(DEBUG) System.out.println(Arrays.toString(registry.list()));
        taskRMIExecutorInterface = (TaskRMIExecutorInterface) registry.lookup("SERVER");
    }
    
    @Override
    public void execute(SerializableRunnableInterface codeToRun, Runnable callbackCode, boolean keepCallbackRunning) {
        if(DEBUG) System.out.println("execute " + codeToRun + " " + keepCallbackRunning);
        if(DEBUG) System.out.println(registry);
        
        String serviceName = UUID.randomUUID().toString();
        try {
            CallbackRMIExecutor callbackRMIExecutor = new CallbackRMIExecutor(callbackCode, keepCallbackRunning);
            registry.bind(serviceName, callbackRMIExecutor);
            taskRMIExecutorInterface.execute(codeToRun, serviceName);
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }
        
    }
}
