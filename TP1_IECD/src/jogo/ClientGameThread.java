package jogo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class ClientGameThread extends Thread {

	private Socket playerConnection;
	private Semaphore semaphore;
	private Tabuleiro tJogador;
	private Tabuleiro tOponente;
	private boolean emJogo;
	private Document docServer;
	private Servidor servidor;
	private UUID uuid;

	public ClientGameThread(UUID uuid, Socket playerConnection, Tabuleiro tJogador, Tabuleiro tOponente, Semaphore semaphore, Document docServer, Servidor servidor) {
		this.playerConnection = playerConnection;
		this.tJogador         = tJogador;
		this.tOponente        = tOponente;
		this.semaphore        = semaphore;
		this.docServer        = docServer;
		this.servidor         = servidor;
		this.emJogo           = true;
		this.uuid             = uuid;
		try {
			playerConnection.setSoTimeout(1000 * 60);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public String apresentar(String nickname) throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = dbf.newDocumentBuilder();
		Document doc = builder.newDocument();
        
    	Element protocolo = doc.createElement("protocolo");
    	doc.appendChild(protocolo);
    	Element metodo = doc.createElement("apresentar");
    	protocolo.appendChild(metodo);
    	Element reply = doc.createElement("resposta");
    	metodo.appendChild(reply);
    	
    	if (tOponente.fimDeJogo()) {
    		emJogo = false;
    		Element fim = doc.createElement("fimJogo");
    		fim.setTextContent(tJogador.getNickname());
        	reply.appendChild(fim);
    	}
    	if (tJogador.fimDeJogo()) {
    		emJogo = false;
    		Element fim = doc.createElement("fimJogo");
    		fim.setTextContent(tOponente.getNickname());
        	reply.appendChild(fim);
    	}
		
        for (String tipoBarco : tJogador.getBarcos().keySet()) {
        	Element novoBarco = doc.createElement("barco");
        	novoBarco.setAttribute("tipo", Character.toString(tipoBarco.charAt(0)));
        	for (String pos : tJogador.getBarcos().get(tipoBarco)) {
	            Element posicao = doc.createElement("posicao");
	            posicao.setTextContent(pos);
	            novoBarco.appendChild(posicao);
        	}
        	reply.appendChild(novoBarco);
        }	
        
        for (String tiroSofrido : tJogador.getTirosSofridos()) {
        	Element novoTiroSofrido = doc.createElement("tiroSofrido");
        	novoTiroSofrido.setAttribute("posicao", tiroSofrido);
        	novoTiroSofrido.setTextContent(tJogador.getTiroSofrido(tiroSofrido));
        	reply.appendChild(novoTiroSofrido);
        }
        
        for (String tiroDado : tOponente.getTirosSofridos()) {
        	Element novoTiroDado = doc.createElement("tiroDado");
        	novoTiroDado.setAttribute("posicao", tiroDado);
        	novoTiroDado.setTextContent(tOponente.getTiroSofrido(tiroDado));
        	reply.appendChild(novoTiroDado);
        }
        
        String replyString = "";
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);
			replyString = result.getWriter().toString();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
        
		return replyString;
	}
	
	public String disparar(String nickname, String tiro) throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = dbf.newDocumentBuilder();
		Document doc = builder.newDocument();
        
    	Element protocolo = doc.createElement("protocolo");
    	doc.appendChild(protocolo);
    	Element metodo = doc.createElement("disparar");
    	protocolo.appendChild(metodo);
    	Element reply = doc.createElement("resposta");
    	metodo.appendChild(reply);
		
        reply.setTextContent(tOponente.sofrerTiro(tiro));
        if (tOponente.fimDeJogo()) {
        	servidor.atualizarVitoria(tJogador.getNickname());
        	servidor.removerJogo(uuid);
        	emJogo = false;
        	if (semaphore.getQueueLength() == 1) {
        		semaphore.release();
        	}
        }
        
        String replyString = "";
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);
			replyString = result.getWriter().toString();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		
		if (emJogo) servidor.atualizarJogo(uuid, tJogador, tOponente, tOponente.getNickname());
        
		return replyString;
	}
	

	public void run() {
		BufferedReader is = null;
		PrintWriter os    = null;
		
		try { 
			is = new BufferedReader(new InputStreamReader(playerConnection.getInputStream()));
			os = new PrintWriter(playerConnection.getOutputStream(), true);
						
			while(emJogo) {
				String pedido   = is.readLine();
				String resposta = processarPedido(pedido);
				os.println(resposta);		
				os.println("Aguardar turno...");
				try {
					if (!tJogador.getNickname().equals(servidor.getProximoAJogar(uuid))) {
						semaphore.acquire();
					}
					if(!emJogo) break;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				pedido   = is.readLine();
				resposta = processarPedido(pedido);
				os.println(resposta);
				if(!emJogo) break;
				os.println("Inserir Jogada: ");
				String jogadaJogador1 = is.readLine();
				resposta = processarPedido(jogadaJogador1);
				os.println(resposta);	
				semaphore.release();
			}
		} catch (IOException e) {
			servidor.removerLigacao(tJogador.getNickname());
			System.out.println("Desconectado...");
			return;
		}
		
		try {
			playerConnection.setSoTimeout(0);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		new EsperaThread(playerConnection, docServer, servidor, tJogador.getNickname()).start();
	}


	private String processarPedido(String pedido) {
		
		Document doc = null;
	    try {
	    	DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    	InputSource is = new InputSource();
	    	is.setCharacterStream(new StringReader(pedido));
			doc = db.parse(is);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		} 
	    
	    Node protocolo     = doc.getElementsByTagName("protocolo").item(0);
	    Node metodo        = protocolo.getFirstChild();
	    String metodoNome  = metodo.getNodeName();
	    String nickname      = metodo.getFirstChild().getFirstChild().getTextContent();
	    String reply = "";
	    
	    try {
	    	if (!servidor.validarPedido(pedido)) {
	    		if (metodoNome.equals("disparar")) {
	    			Random rand = new Random();
	    			return disparar(nickname,  String.valueOf( (char) (64 + rand.nextInt(10) + 1))  + String.valueOf(rand.nextInt(10) + 1));
	    		}
	    		return "Erro na estruturação da mensagem! Por favor repetir pedido...";
	    	};
	    } catch (SAXException | ParserConfigurationException e) {
	    }
	    
	   switch (metodoNome) {
			case "apresentar":
				try {reply = apresentar(nickname);} catch (ParserConfigurationException e) {e.printStackTrace();}
				break;
			case "disparar":
				String tiro = metodo.getFirstChild().getLastChild().getTextContent();
				try {reply = disparar(nickname, tiro);} catch (ParserConfigurationException e) {e.printStackTrace();}
				break;
	   }
	   return reply;
	} 
} 