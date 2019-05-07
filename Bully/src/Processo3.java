import java.lang.management.ManagementFactory;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class Processo3 implements P {

	private long pid; // O ID do processo corrente.
	private P lider; // Uma refer�ncia para o processo l�der.
	private HashMap<Long, P> processos; // Um hashmap relacionando cada processo com seu respectivo ID.
	private boolean startNewElection; // Para saber se algu�m solicitou nova elei��o.

	public Processo3() {
		this.pid = Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		this.processos = new HashMap<Long, P>();
		/* O processo inicia com o l�der desconhecido. Logo, sempre que um
		 * processo entra no jogo ele solicita uma nova elei��o.
		 */
		this.lider = null;
		this.startNewElection = false; // Usada para verificar, dentro do loop, e algu�m solicitou nova elei��o.
	}

	/**
	 * A fun��o startElection tem a simples fun��o de modificar a vari�vel que
	 * indica que algu�m solicitou uma nova elei��o.
	 */
	@Override
	public boolean startElection() throws RemoteException {
		this.startNewElection = true;
		return true;
	}

	/**
	 * A fun��o setLeader � usada para informar o novo l�der do grupo. A ideia �
	 * executar dentro do conjunto de processos mantidos por cada processo.
	 */
	@Override
	public void setLeader(long id_do_lider) throws RemoteException {
		this.lider = this.getProcesso(id_do_lider);
		System.out.println("(" + id_do_lider + ") � o novo l�der!");
	}

	/**
	 * Usado para capturar o id do processo. Utilizo esta mesma fun��o para
	 * saber se o l�der est� vivo. Se a comunica��o falhar, o l�der � dado como morto.
	 */
	@Override
	public long getPid() throws RemoteException {
		return this.pid;
	}

	/**
	 * Obt�m a instancia de um processo mediante o valor do seu ID.
	 */
	private P getProcesso(long pid) {
		return this.processos.get(pid);
	}

	/**
	 * Percorre o HashMap de processos e solicita aos de maior ID que iniciem
	 * nova elei��o. Se as comunica��es falharem, ou se nenhum teve o ID maior
	 * que o solicitante, o mesmo se declara como novo l�der.
	 */
	private void askForNewElection() {
		System.out.println(this.getClass().getSimpleName() + " (" + this.pid + ") solicitando nova elei��o.");

		boolean anybodyAnswered = false;

		// Pe�o aos processos de maior ID que iniciem nova elei��o.
		for (Map.Entry<Long, P> processo : this.processos.entrySet()) {
			if (this.pid < processo.getKey()) {
				try {
					processo.getValue().startElection();
					anybodyAnswered = true; // Presumo que algu�m respondeu.
				} catch (RemoteException e) {
					// e.printStackTrace();
				}
			}
		}

		// Se ningu�m respondeu ou se n�o h� ID maior que o meu, informo a todos que sou o novo l�der.
		if (!anybodyAnswered) {
			this.sendVictoryMessage();
		}
	}

	/**
	 * Informa a todos os processos mantidos no hashmap que agora sou o novo l�der.
	 */
	private void sendVictoryMessage() {
		System.out.println(this.getClass().getSimpleName() + " (" + this.pid + ") � o novo l�der!");

		this.lider = this;
		for (Map.Entry<Long, P> processo : this.processos.entrySet()) {
			try {
				processo.getValue().setLeader(this.pid);
			} catch (RemoteException e) {
				// e.printStackTrace();
			}
		}
	}

	/**
	 * Usado por cada processo para se registar no hasmap dos demais.
	 */
	@Override
	public void registrar(long idDoNovoProcesso, P novoProcesso) throws RemoteException {
		if (idDoNovoProcesso != this.pid) {
			this.processos.put(idDoNovoProcesso, novoProcesso);
		}
	}

	public static void main(String args[]) {

		Processo3 p = new Processo3();

		// Capturo o nome da classe para automatizar o que for poss�vel, tendo
		// em vista serem v�rias classes de processos.
		String nameClass = p.getClass().getSimpleName();
		System.out.println("Iniciando " + nameClass + " (" + p.pid + ")");
		//Obtenho o n�mero presente no nome da classe para automatizar o n�mero da porta e o la�o que itera entre oa processos.
		int numeroSequenciaClasse = Integer.parseInt(nameClass.split("Processo")[1]); 
		
		final long TEMPO_ANTES_DA_INICIALIZACAO = 10000; // 10 segundos
		final long TEMPO_ANTES_DE_VERIFICAR_O_LIDER = 5000; // 5 segundos
		final long TEMPO_ESPERA_DEPOIS_DE_PEDIR_NOVA_ELEICAO = 3000; // 3  segundos
		final int PORTA_DE_REGISTRO = (1100 + numeroSequenciaClasse); //A porta varia conforme a sequencia presente no n�mero da classe.

		// Aciono o servi�o de registro de nomes do RMI
		Registry reg = null;
		try {
			reg = LocateRegistry.createRegistry(1099);
		} catch (Exception ex) {
			try {
				reg = LocateRegistry.getRegistry(1099);
			} catch (RemoteException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}

		P stub = null;
		try {
			// Exporto o objeto para uma porta que varia para cada processo.
			stub = (P) UnicastRemoteObject.exportObject(p, PORTA_DE_REGISTRO);
			reg.rebind(nameClass, stub);
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}

		// Aguardo por algum tempo e ent�o me inscrevo como processo para todos os outros.
		try {
			Thread.sleep(TEMPO_ANTES_DA_INICIALIZACAO);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		/* Uma vez que o tamanho do for varia conforme o n�mero no nome da classe,
		 * este programa n�o est� restrito a apenas 5 processos como solicitado na atividade.
		 * Na verdade, podem ser criadas tantas classes de processos quantas forem desejadas, 
		 * contanto que se observe o padr�o atual: "Processo" + numero de sequ�ncia.
		 */
		for (int i = 1; i < numeroSequenciaClasse; i++) {
			try {
				if ("Processo" + i != nameClass) {
					/* Tomando o cuidado de n�o se registrar para si mesmo, cada
					 * processo comunica aos demais sobre sua exist�ncia. Para
					 * evitar que apenas o primeiro que tenha iniciado tenha
					 * consci�ncia dos demais cada processo que aciona o m�todo
					 * registrar do destinat�rio, toma o cuidado de tamb�m
					 * registrar o destinat�rio em sua lista de processos.
					 */
					P process = (P) reg.lookup("Processo" + i);
					p.registrar(process.getPid(), process); // Registro o destinat�rio em minha lista.
					process.registrar(p.pid, stub); // Registro a mim mesmo na lista dele.
				}
			} catch (RemoteException | NotBoundException e) {
				//e.printStackTrace();
			}
		}

		try {
			while (true) {

				/**
				 * Solicito nova elei��o se o processo acabou de ser criado ou se outro processo
				 * de ID inferior tiver solicitado.
				 */
				if (p.startNewElection || p.lider == null) {
					p.askForNewElection();
					p.startNewElection = false;
					Thread.sleep(TEMPO_ESPERA_DEPOIS_DE_PEDIR_NOVA_ELEICAO);

				} else {
					try {
						Thread.sleep(TEMPO_ANTES_DE_VERIFICAR_O_LIDER);
						/* Para verificar se o l�der est� ativo, simplesmente pe�o seu ID.
						 * Se a comunica��o falhar, executo o que est� no catch da exce��o.
						 */
						long pidLider = p.lider.getPid();
						System.out.println("(" + pidLider + ") � o l�der!");

					} catch (RemoteException e) {
						/* Se o l�der n�o responder, pe�o aos processos de maior
						 * ID que iniciem nova elei��o.
						 */
						System.out.println("O l�der se foi!");
						p.askForNewElection();
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
