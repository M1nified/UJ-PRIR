
/**
* optimizationOperations.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from generator.idl
* Monday, November 26, 2018 12:39:32 AM CET
*/

public interface optimizationOperations 
{
  void register (short ip, int timeout, org.omg.CORBA.IntHolder id);

	void hello(int id);

	void best_range(rangeHolder r);
} // interface optimizationOperations
