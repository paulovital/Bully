import java.rmi.Remote;
import java.rmi.RemoteException;

public interface P extends Remote{
   
	//Pe�o uma nova elei��o
	public boolean startElection() throws RemoteException;
	
	//Indico quem � o novo L�der
	public void setLeader(long id_do_lider) throws RemoteException;
	
	//Pe�o o PID de um processo
	public long getPid() throws RemoteException;
	
	//Para informar aos demais sobre um novo processo
	public void registrar(long idDoNovoProcesso, P novoProcesso) throws RemoteException;
	
}
