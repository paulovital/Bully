import java.rmi.Remote;
import java.rmi.RemoteException;

public interface P extends Remote{
   
	//Peço uma nova eleição
	public boolean startElection() throws RemoteException;
	
	//Indico quem é o novo Líder
	public void setLeader(long id_do_lider) throws RemoteException;
	
	//Peço o PID de um processo
	public long getPid() throws RemoteException;
	
	//Para informar aos demais sobre um novo processo
	public void registrar(long idDoNovoProcesso, P novoProcesso) throws RemoteException;
	
}
