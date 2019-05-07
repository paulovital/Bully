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
	private P lider; // Uma referência para o processo líder.
	private HashMap<Long, P> processos; // Um hashmap relacionando cada processo com seu respectivo ID.
	private boolean startNewElection; // Para saber se alguém solicitou nova eleição.

	public Processo3() {
		this.pid = Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		this.processos = new HashMap<Long, P>();
		/* O processo inicia com o líder desconhecido. Logo, sempre que um
		 * processo entra no jogo ele solicita uma nova eleição.
		 */
		this.lider = null;
		this.startNewElection = false; // Usada para verificar, dentro do loop, e alguém solicitou nova eleição.
	}

	/**
	 * A função startElection tem a simples função de modificar a variável que
	 * indica que alguém solicitou uma nova eleição.
	 */
	@Override
	public boolean startElection() throws RemoteException {
		this.startNewElection = true;
		return true;
	}

	/**
	 * A função setLeader é usada para informar o novo líder do grupo. A ideia é
	 * executar dentro do conjunto de processos mantidos por cada processo.
	 */
	@Override
	public void setLeader(long id_do_lider) throws RemoteException {
		this.lider = this.getProcesso(id_do_lider);
		System.out.println("(" + id_do_lider + ") é o novo líder!");
	}

	/**
	 * Usado para capturar o id do processo. Utilizo esta mesma função para
	 * saber se o líder está vivo. Se a comunicação falhar, o líder é dado como morto.
	 */
	@Override
	public long getPid() throws RemoteException {
		return this.pid;
	}

	/**
	 * Obtém a instancia de um processo mediante o valor do seu ID.
	 */
	private P getProcesso(long pid) {
		return this.processos.get(pid);
	}

	/**
	 * Percorre o HashMap de processos e solicita aos de maior ID que iniciem
	 * nova eleição. Se as comunicações falharem, ou se nenhum teve o ID maior
	 * que o solicitante, o mesmo se declara como novo líder.
	 */
	private void askForNewElection() {
		System.out.println(this.getClass().getSimpleName() + " (" + this.pid + ") solicitando nova eleição.");

		boolean anybodyAnswered = false;

		// Peço aos processos de maior ID que iniciem nova eleição.
		for (Map.Entry<Long, P> processo : this.processos.entrySet()) {
			if (this.pid < processo.getKey()) {
				try {
					processo.getValue().startElection();
					anybodyAnswered = true; // Presumo que alguém respondeu.
				} catch (RemoteException e) {
					// e.printStackTrace();
				}
			}
		}

		// Se ninguém respondeu ou se não há ID maior que o meu, informo a todos que sou o novo líder.
		if (!anybodyAnswered) {
			this.sendVictoryMessage();
		}
	}

	/**
	 * Informa a todos os processos mantidos no hashmap que agora sou o novo líder.
	 */
	private void sendVictoryMessage() {
		System.out.println(this.getClass().getSimpleName() + " (" + this.pid + ") é o novo líder!");

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

		// Capturo o nome da classe para automatizar o que for possível, tendo
		// em vista serem várias classes de processos.
		String nameClass = p.getClass().getSimpleName();
		System.out.println("Iniciando " + nameClass + " (" + p.pid + ")");
		//Obtenho o número presente no nome da classe para automatizar o número da porta e o laço que itera entre oa processos.
		int numeroSequenciaClasse = Integer.parseInt(nameClass.split("Processo")[1]); 
		
		final long TEMPO_ANTES_DA_INICIALIZACAO = 10000; // 10 segundos
		final long TEMPO_ANTES_DE_VERIFICAR_O_LIDER = 5000; // 5 segundos
		final long TEMPO_ESPERA_DEPOIS_DE_PEDIR_NOVA_ELEICAO = 3000; // 3  segundos
		final int PORTA_DE_REGISTRO = (1100 + numeroSequenciaClasse); //A porta varia conforme a sequencia presente no número da classe.

		// Aciono o serviço de registro de nomes do RMI
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

		// Aguardo por algum tempo e então me inscrevo como processo para todos os outros.
		try {
			Thread.sleep(TEMPO_ANTES_DA_INICIALIZACAO);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		/* Uma vez que o tamanho do for varia conforme o número no nome da classe,
		 * este programa não está restrito a apenas 5 processos como solicitado na atividade.
		 * Na verdade, podem ser criadas tantas classes de processos quantas forem desejadas, 
		 * contanto que se observe o padrão atual: "Processo" + numero de sequência.
		 */
		for (int i = 1; i < numeroSequenciaClasse; i++) {
			try {
				if ("Processo" + i != nameClass) {
					/* Tomando o cuidado de não se registrar para si mesmo, cada
					 * processo comunica aos demais sobre sua existência. Para
					 * evitar que apenas o primeiro que tenha iniciado tenha
					 * consciência dos demais cada processo que aciona o método
					 * registrar do destinatário, toma o cuidado de também
					 * registrar o destinatário em sua lista de processos.
					 */
					P process = (P) reg.lookup("Processo" + i);
					p.registrar(process.getPid(), process); // Registro o destinatário em minha lista.
					process.registrar(p.pid, stub); // Registro a mim mesmo na lista dele.
				}
			} catch (RemoteException | NotBoundException e) {
				//e.printStackTrace();
			}
		}

		try {
			while (true) {

				/**
				 * Solicito nova eleição se o processo acabou de ser criado ou se outro processo
				 * de ID inferior tiver solicitado.
				 */
				if (p.startNewElection || p.lider == null) {
					p.askForNewElection();
					p.startNewElection = false;
					Thread.sleep(TEMPO_ESPERA_DEPOIS_DE_PEDIR_NOVA_ELEICAO);

				} else {
					try {
						Thread.sleep(TEMPO_ANTES_DE_VERIFICAR_O_LIDER);
						/* Para verificar se o líder está ativo, simplesmente peço seu ID.
						 * Se a comunicação falhar, executo o que está no catch da exceção.
						 */
						long pidLider = p.lider.getPid();
						System.out.println("(" + pidLider + ") é o líder!");

					} catch (RemoteException e) {
						/* Se o líder não responder, peço aos processos de maior
						 * ID que iniciem nova eleição.
						 */
						System.out.println("O líder se foi!");
						p.askForNewElection();
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
