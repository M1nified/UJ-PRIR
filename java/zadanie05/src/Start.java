import org.omg.CORBA.IntHolder;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.PortableServer.POA;

import com.sun.corba.se.org.omg.CORBA.ORB;

class OptimizationImpl extends optimizationPOA implements optimizationOperations {

	@Override
	public void register(short ip, int timeout, IntHolder id) {
		System.out.println("OptimizationImpl/register " + ip + " " + timeout + " " + id);
	}

	@Override
	public void hello(int id) {
		System.out.println("OptimizationImpl/hello " + id);
	}

	@Override
	public void best_range(rangeHolder r) {
		System.out.println("OptimizationImpl/best_range " + r);
		r.value = new range((short)1,(short)2);
	}
}

class Start {

	public static void main(String[] args) {
		try {
			org.omg.CORBA.ORB orb = ORB.init(args, null);
			POA rootpoa = (POA) orb.resolve_initial_references("RootPOA");
			rootpoa.the_POAManager().activate();

			OptimizationImpl optimizationImpl = new OptimizationImpl();
			org.omg.CORBA.Object ref = rootpoa.servant_to_reference(optimizationImpl);

			System.out.println(orb.object_to_string(ref));

			org.omg.CORBA.Object namingContextObj = orb.resolve_initial_references("NameService");
			NamingContext nCont = NamingContextHelper.narrow(namingContextObj);
			NameComponent[] path = { new NameComponent("Optymalizacja", "OptimizationImpl") };

			nCont.rebind(path, ref);
			orb.run();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
