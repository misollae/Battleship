package jogo;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Servidor {

	private static final int DEFAULT_PORT = 80;
	private Document docServidor;
	private Map<String, Socket> connected     = new HashMap<String, Socket>(); 
	private ArrayList<Tabuleiro> jogoEmEspera = new ArrayList<Tabuleiro>(2);
	private Map<UUID, Semaphore> semaforos    = new HashMap<UUID, Semaphore>(); 
	private int emEspera = 0;
	private static String protocoloXSD = "C:\\Users\\letic\\OneDrive\\Ambiente de Trabalho\\isel\\sem iv\\iecd\\TP1_IECD\\TP1_IECD\\WebContent\\xml\\xsdProtocolo.xsd";


	public Servidor() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(DEFAULT_PORT);
			Socket newSock = null;
			try {
	            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	            DocumentBuilder builder = factory.newDocumentBuilder();
	            docServidor = builder.parse("C:\\Users\\letic\\OneDrive\\Ambiente de Trabalho\\isel\\sem iv\\iecd\\TP1_IECD\\TP1_IECD\\WebContent\\xml\\users.xml");
	        } catch (Exception e) {
	            e.printStackTrace(System.out);
	            System.out.print("Erro ao analisar o documento XML.");
	        }

			for (;;) {
				System.out.println("Servidor TCP concorrente aguarda ligacao no porto " + DEFAULT_PORT + "...");
				newSock = serverSocket.accept();
				System.out.println("Cliente a conectar-se...");
				Thread threadLogIn = new ThreadLogin(newSock, docServidor, this);
				threadLogIn.start();
			}
		} catch (IOException e) {
			System.err.println("Excep��o no servidor: " + e);
		}
	} 
	
	public synchronized void adicionarLigacao(String nickname, Socket socket) {
		connected.put(nickname, socket);
	}	
	
	public synchronized boolean isConnected(String nickname) {
		return connected.containsKey(nickname);
	}
	
	public synchronized void removerLigacao(String nickname) {
		try {
			connected.get(nickname).close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		connected.remove(nickname);
		System.out.println("Desconectou");
	}
	
	public synchronized void atualizarVitoria(String nickname) {
		Element servidor = docServidor.getDocumentElement();
		Element utilizadores = (Element) servidor.getElementsByTagName("jogadores").item(0);
		NodeList jogadores = utilizadores.getElementsByTagName("jogadorInscrito");
		for (int i = 0; i < jogadores.getLength(); i++) {
			if (jogadores.item(i).getAttributes().getNamedItem("nome").getNodeValue().equals(nickname)){
				int vitorias = Integer.valueOf(jogadores.item(i).getAttributes().getNamedItem("vitorias").getNodeValue());
				jogadores.item(i).getAttributes().getNamedItem("vitorias").setNodeValue((Integer.toString(vitorias + 1)));  
			}		
		}
	}
	
	public synchronized void espera(Tabuleiro tabuleiro) {
		if (estaEmJogo(tabuleiro.getNickname())) {
			restartJogo(tabuleiro.getNickname());
			return;
		}	
		emEspera++;
		jogoEmEspera.add(tabuleiro);
		if (emEspera < 2) {
			try {
				System.out.println("Começa espera...");
				wait();
			} catch (InterruptedException e) {}
		} else {
			adicionarNovoJogo();
			notifyAll();	
		}
	}
	
	public synchronized void adicionarNovoJogo() {
		UUID uuid = UUID.randomUUID();
		emEspera = 0;
		Semaphore gameSemaphore = new Semaphore(0);
		semaforos.put(uuid, gameSemaphore);
		Thread threadJogador1 = new ClientGameThread(uuid, connected.get(jogoEmEspera.get(0).getNickname()), jogoEmEspera.get(0), jogoEmEspera.get(1), gameSemaphore, docServidor, this);
		Thread threadJogador2 = new ClientGameThread(uuid, connected.get(jogoEmEspera.get(1).getNickname()), jogoEmEspera.get(1), jogoEmEspera.get(0), gameSemaphore, docServidor, this);
		threadJogador1.start();
		threadJogador2.start();
		
		Element root = docServidor.getDocumentElement();
		Element jogosAtivos = (Element) root.getElementsByTagName("jogosAtivos").item(0);
		Element novoJogo = docServidor.createElement("jogoAtivo");
		novoJogo.setAttribute("IDJogo", uuid.toString());
		novoJogo.setAttribute("proximo", jogoEmEspera.get(0).getNickname());
		
		Element jogo1 = docServidor.createElement("jogador");
		jogo1.setAttribute("nome", jogoEmEspera.get(0).getNickname());		
		Element jogo2 = docServidor.createElement("jogador");
		jogo2.setAttribute("nome", jogoEmEspera.get(1).getNickname());

		novoJogo.appendChild(jogo1);
		novoJogo.appendChild(jogo2);
		jogosAtivos.appendChild(novoJogo);
		
		atualizarJogo(uuid, jogoEmEspera.get(0), jogoEmEspera.get(1), jogoEmEspera.get(0).getNickname());
		jogoEmEspera.clear();	
	}
	
	public synchronized void atualizarJogo(UUID uuid, Tabuleiro t1, Tabuleiro t2, String proximoAJogar) {
		Element root = docServidor.getDocumentElement();
		NodeList jogosAtivos = ((Element) root.getElementsByTagName("jogosAtivos").item(0)).getElementsByTagName("jogoAtivo");
		Element jogoAAdicionar = null;
		for (int i = 0; i < jogosAtivos.getLength() ; i++) {
			if (jogosAtivos.item(i).getAttributes().getNamedItem("IDJogo").getNodeValue().equals(uuid.toString())) {
				jogoAAdicionar = (Element) jogosAtivos.item(i);
			}	
		}
		
		jogoAAdicionar.setAttribute("proximo", proximoAJogar);
		jogoAAdicionar.removeChild(jogoAAdicionar.getElementsByTagName("jogador").item(0));
		jogoAAdicionar.removeChild(jogoAAdicionar.getElementsByTagName("jogador").item(0));
		
		Element jogador1 = docServidor.createElement("jogador");
		jogador1.setAttribute("nome", t1.getNickname());
		jogoAAdicionar.appendChild(jogador1);
		Element jogador2 = docServidor.createElement("jogador");
		jogador2.setAttribute("nome", t2.getNickname());
		jogoAAdicionar.appendChild(jogador2);
				
		Element tabuleiro1 = docServidor.createElement("tabuleiro");
		for (String tipoBarco : t1.getBarcos().keySet()) {
        	Element novoBarco = docServidor.createElement("barco");
        	novoBarco.setAttribute("tipo", tipoBarco);
        	for (String pos : t1.getBarcos().get(tipoBarco)) {
	            Element posicao = docServidor.createElement("posicao");
	            posicao.setTextContent(pos);
	            novoBarco.appendChild(posicao);
        	}
        	tabuleiro1.appendChild(novoBarco);
        }	
        for (String tiroSofrido : t1.getTirosSofridos()) {
        	Element novoTiroSofrido = docServidor.createElement("tiroSofrido");
        	novoTiroSofrido.setAttribute("posicao", tiroSofrido);
        	novoTiroSofrido.setTextContent(t1.getTiroSofrido(tiroSofrido));
        	tabuleiro1.appendChild(novoTiroSofrido);
        } 

		
		Element tabuleiro2 = docServidor.createElement("tabuleiro");
		for (String tipoBarco : t2.getBarcos().keySet()) {
        	Element novoBarco = docServidor.createElement("barco");
        	novoBarco.setAttribute("tipo", tipoBarco);
        	for (String pos : t2.getBarcos().get(tipoBarco)) {
	            Element posicao = docServidor.createElement("posicao");
	            posicao.setTextContent(pos);
	            novoBarco.appendChild(posicao);
        	}
        	tabuleiro2.appendChild(novoBarco);
        }	
        for (String tiroSofrido : t2.getTirosSofridos()) {
        	Element novoTiroSofrido = docServidor.createElement("tiroSofrido");
        	novoTiroSofrido.setAttribute("posicao", tiroSofrido);
        	novoTiroSofrido.setTextContent(t2.getTiroSofrido(tiroSofrido));
        	tabuleiro2.appendChild(novoTiroSofrido);
        } 
        
        
        jogador1.appendChild(tabuleiro1);
        jogador2.appendChild(tabuleiro2);
	}
	
	public synchronized void removerJogo(UUID uuid) {
		Element root = docServidor.getDocumentElement();
		NodeList jogosAtivos = ((Element) root.getElementsByTagName("jogosAtivos").item(0)).getElementsByTagName("jogoAtivo");
		for (int i = 0; i < jogosAtivos.getLength() ; i++) {
			if (jogosAtivos.item(i).getAttributes().getNamedItem("IDJogo").getNodeValue().equals(uuid.toString())) {
				((Element) root.getElementsByTagName("jogosAtivos").item(0)).removeChild(jogosAtivos.item(i));
			}	
		}
	}
	
	public synchronized void restartJogo(String nickname) {
		
		Element root = docServidor.getDocumentElement();
		NodeList jogosAtivos = ((Element) root.getElementsByTagName("jogosAtivos").item(0)).getElementsByTagName("jogoAtivo");
		Element jogo = null;
		Element tabJogador  = null;
		Element tabOponente = null;
		String nicknameOponente = "";
		
		for (int i = 0; i < jogosAtivos.getLength() ; i++) {
			NodeList jogadores = ((Element) jogosAtivos.item(i)).getElementsByTagName("jogador");
			for (int j = 0; j < jogadores.getLength() ; j++) {
				if (jogadores.item(j).getAttributes().getNamedItem("nome").getNodeValue().equals(nickname)) {
					tabJogador = (Element) ((Element) jogadores.item(j)).getElementsByTagName("tabuleiro").item(0);
					jogo = (Element) jogosAtivos.item(i);
				} else {
					nicknameOponente = jogadores.item(j).getAttributes().getNamedItem("nome").getNodeValue();
					tabOponente = (Element) ((Element) jogadores.item(j)).getElementsByTagName("tabuleiro").item(0);
				}
				if (!nicknameOponente.equals("") && jogo != null) break;
			}
		}
		
		UUID uuid = UUID.fromString(jogo.getAttributes().getNamedItem("IDJogo").getNodeValue());		
		Map<String, ArrayList<String>> shipsJogador  = new HashMap<String, ArrayList<String>>();
		Map<String, ArrayList<String>> shipsOponente = new HashMap<String, ArrayList<String>>();
		ArrayList<String> tirosSofridosJogador  = new ArrayList<String>();
		ArrayList<String> tirosSofridosOponente = new ArrayList<String>();
		
		NodeList barcosJogador  = tabJogador.getElementsByTagName("barco");
		NodeList barcosOponente = tabOponente.getElementsByTagName("barco");
		NodeList sofridosJogador  = tabJogador.getElementsByTagName("tiroSofrido");
		NodeList sofridosOponente = tabOponente.getElementsByTagName("tiroSofrido");

		for (int i = 0 ; i < barcosJogador.getLength() ; i++) {
			NodeList posicoes1 = ((Element) barcosJogador.item(i)).getElementsByTagName("posicao");
			ArrayList<String> posicoesBarco1 = new ArrayList<String>();
			for (int j = 0 ; j < posicoes1.getLength() ; j++) {
				posicoesBarco1.add(posicoes1.item(j).getTextContent());
			}
			shipsJogador.put(barcosJogador.item(i).getAttributes().getNamedItem("tipo").getNodeValue(), posicoesBarco1);
		}
		
		for (int i = 0 ; i < barcosOponente.getLength() ; i++) {
			NodeList posicoes2 = ((Element) barcosOponente.item(i)).getElementsByTagName("posicao");
			ArrayList<String> posicoesBarco2 = new ArrayList<String>();
			for (int j = 0 ; j < posicoes2.getLength() ; j++) {
				posicoesBarco2.add(posicoes2.item(j).getTextContent());
			}
			shipsOponente.put(barcosOponente.item(i).getAttributes().getNamedItem("tipo").getNodeValue(), posicoesBarco2);
		}
		
		
		for (int i = 0 ; i < sofridosJogador.getLength() ; i++) {
			tirosSofridosJogador.add(sofridosJogador.item(i).getAttributes().getNamedItem("posicao").getNodeValue());
		}
		for (int i = 0 ; i < sofridosOponente.getLength() ; i++) {
			tirosSofridosOponente.add(sofridosOponente.item(i).getAttributes().getNamedItem("posicao").getNodeValue());
		}
		
		Tabuleiro tJogador = new Tabuleiro(nickname, shipsJogador, tirosSofridosJogador);
		Tabuleiro tOponente = new Tabuleiro(nicknameOponente, shipsOponente, tirosSofridosOponente);
		
		new ClientGameThread(uuid, connected.get(nickname), tJogador, tOponente, semaforos.get(uuid), docServidor, this).start();
	}
	
	public synchronized boolean estaEmJogo(String nickname) {
		Element root = docServidor.getDocumentElement();
		NodeList jogosAtivos = ((Element) root.getElementsByTagName("jogosAtivos").item(0)).getElementsByTagName("jogoAtivo");
		for (int i = 0; i < jogosAtivos.getLength() ; i++) {
			NodeList jogadores = ((Element) jogosAtivos.item(i)).getElementsByTagName("jogador");
			for (int j = 0; j < jogadores.getLength() ; j++) {
				if (jogadores.item(j).getAttributes().getNamedItem("nome").getNodeValue().equals(nickname)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public synchronized String getProximoAJogar(UUID uuid) {
		Element root = docServidor.getDocumentElement();
		NodeList jogosAtivos = ((Element) root.getElementsByTagName("jogosAtivos").item(0)).getElementsByTagName("jogoAtivo");
		for (int i = 0; i < jogosAtivos.getLength() ; i++) {
			if (jogosAtivos.item(i).getAttributes().getNamedItem("IDJogo").getNodeValue().equals(uuid.toString())) {
				return jogosAtivos.item(i).getAttributes().getNamedItem("proximo").getNodeValue();
			}	
		}
		return null;
	}
	
	public synchronized void adicionarRegisto(String nickname, String password) {
		Element servidor = docServidor.getDocumentElement();
		Element utilizadores = (Element) servidor.getElementsByTagName("jogadores").item(0);
		Element novoJogador = docServidor.createElement("jogador");
		novoJogador.setAttribute("nome", nickname);
		novoJogador.setAttribute("password", password);
		novoJogador.setAttribute("vitoria", "0");
		novoJogador.appendChild(docServidor.createElement("fotografia"));
		utilizadores.appendChild(novoJogador);
	}
	
	public boolean validarPedido(String pedido) throws SAXException {
		Document doc = null;
		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(pedido));
			doc = db.parse(is);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		} 
		
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = factory.newSchema(new File(protocoloXSD));
		Validator validator = schema.newValidator();
		
		try {
			validator.validate(new DOMSource(doc));
			return true;
		} catch (IOException | SAXException e) {
			return false;
		}
	}
	
	
	public static void main(String[] args) {
		new Servidor();
	}
} 

