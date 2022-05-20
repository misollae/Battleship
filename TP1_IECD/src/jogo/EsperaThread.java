package jogo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Socket;
import java.util.HashMap;

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
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class EsperaThread extends Thread {
	
	private Socket playerConnection;
	private Document doc;
	private BufferedReader is;
	private PrintWriter os;
	private Servidor server;
	private String nickname;
	private boolean saiu;
	private Tabuleiro ultimoRandom;

	public EsperaThread(Socket playerConnection, Document doc, Servidor server, String nickname) {
		this.ultimoRandom = new Tabuleiro(nickname);
		this.playerConnection = playerConnection;
		this.doc      = doc;
		this.server   = server;
		this.nickname = nickname;
		try {
			is = new BufferedReader(new InputStreamReader(playerConnection.getInputStream()));
			os = new PrintWriter(playerConnection.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		while(!saiu) {
			try {
				String pedido = is.readLine();
				if (pedido != null)
					os.println(processarPedido(pedido));
			} catch (IOException e1) {
				server.removerLigacao(nickname);
				System.out.println("Desconectado...");
				return;
			}
		}
	}
	
	private String logout() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		Document replyDoc = null;
		try {
			builder = dbf.newDocumentBuilder();
			replyDoc = builder.newDocument();
			
			Element protocolo = replyDoc.createElement("protocolo");
			replyDoc.appendChild(protocolo);
			Element metodo = replyDoc.createElement("logout");
			protocolo.appendChild(metodo);
			Element reply = replyDoc.createElement("resposta");
			metodo.appendChild(reply);
			reply.setTextContent("Até breve...");
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
        
        String replyString = "";
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(replyDoc);
			transformer.transform(source, result);
			replyString = result.getWriter().toString();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		
		saiu = true;
		return replyString;	
	}
	
	private String randomizar() {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		Document replyDoc = null;
		try {
			builder = dbf.newDocumentBuilder();
			replyDoc = builder.newDocument();
			
			Element protocolo = replyDoc.createElement("protocolo");
			replyDoc.appendChild(protocolo);
			Element metodo = replyDoc.createElement("randomizar");
			protocolo.appendChild(metodo);
			Element reply = replyDoc.createElement("resposta");
			metodo.appendChild(reply);
			if (server.estaEmJogo(nickname)) {
				System.out.println("esta em jogo");
				reply.setTextContent("A recuperar jogo...");
			} else {
				this.ultimoRandom = new Tabuleiro(nickname);
				for (String tipoBarco : ultimoRandom.getBarcos().keySet()) {
					Element novoBarco = replyDoc.createElement("barco");
					novoBarco.setAttribute("tipo", Character.toString(tipoBarco.charAt(0)));
					for (String pos : ultimoRandom.getBarcos().get(tipoBarco)) {
						Element posicao = replyDoc.createElement("posicao");
						posicao.setTextContent(pos);
						novoBarco.appendChild(posicao);
					}
					reply.appendChild(novoBarco);
				}	
			}
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
        
        String replyString = "";
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(replyDoc);
			transformer.transform(source, result);
			replyString = result.getWriter().toString();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return replyString;
	}
	
	private String jogar() {
		server.espera(this.ultimoRandom);
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		Document replyDoc = null;
		try {
			builder = dbf.newDocumentBuilder();
			replyDoc = builder.newDocument();
			
			Element protocolo = replyDoc.createElement("protocolo");
			replyDoc.appendChild(protocolo);
			Element metodo = replyDoc.createElement("jogar");
			protocolo.appendChild(metodo);
			Element reply = replyDoc.createElement("resposta");
			metodo.appendChild(reply);
			reply.setTextContent("Iniciar jogo...");
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
        
        String replyString = "";
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(replyDoc);
			transformer.transform(source, result);
			replyString = result.getWriter().toString();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		
		saiu = true;
		return replyString;	
	} 
	
	private String mostrarFoto() {
		Element servidor = doc.getDocumentElement();
		Element utilizadores = (Element) servidor.getElementsByTagName("jogadores").item(0);
		NodeList jogadores = utilizadores.getElementsByTagName("jogadorInscrito");
		Node foto = null;
		for (int i = 0; i < jogadores.getLength(); i++) {
			if (jogadores.item(i).getAttributes().getNamedItem("nome").getNodeValue().equals(nickname)){
				foto = ((Element) jogadores.item(i)).getElementsByTagName("fotografia").item(0);
			}		
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		Document replyDoc = null;
		try {
			builder = dbf.newDocumentBuilder();
			replyDoc = builder.newDocument();
			
			Element protocolo = replyDoc.createElement("protocolo");
			replyDoc.appendChild(protocolo);
			Element metodo = replyDoc.createElement("mostrarFoto");
			protocolo.appendChild(metodo);
			Element reply = replyDoc.createElement("resposta");
			
			reply.appendChild(replyDoc.importNode(foto, true));
			metodo.appendChild(reply);
			
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
        
        String replyString = "";
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(replyDoc);
			transformer.transform(source, result);
			replyString = result.getWriter().toString();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		
		return replyString;	
	}
	
	private String mostrarRanking() {
		HashMap<String, Integer> jogadorVitorias = new HashMap<String, Integer>();
		int maiorVitoria = 0;
		
		Element servidor = doc.getDocumentElement();
		Element utilizadores = (Element) servidor.getElementsByTagName("jogadores").item(0);
		NodeList jogadores = utilizadores.getElementsByTagName("jogadorInscrito");
		for (int i = 0; i < jogadores.getLength(); i++) {
			if (Integer.valueOf(jogadores.item(i).getAttributes().getNamedItem("vitorias").getNodeValue()) > maiorVitoria){
				maiorVitoria = Integer.valueOf(jogadores.item(i).getAttributes().getNamedItem("vitorias").getNodeValue());
			}
			jogadorVitorias.put(jogadores.item(i).getAttributes().getNamedItem("nome").getNodeValue(), 
								Integer.valueOf(jogadores.item(i).getAttributes().getNamedItem("vitorias").getNodeValue()));			
		}
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		Document replyDoc = null;
		try {
			builder = dbf.newDocumentBuilder();
			replyDoc = builder.newDocument();
			
			Element protocolo = replyDoc.createElement("protocolo");
			replyDoc.appendChild(protocolo);
			Element metodo = replyDoc.createElement("ranking");
			protocolo.appendChild(metodo);
			Element reply = replyDoc.createElement("resposta");
			metodo.appendChild(reply);
			
			for (int i = maiorVitoria; i >= 0; i--) {
				for (String key : jogadorVitorias.keySet()) {
				    if (jogadorVitorias.get(key) == i) {
				    	Element jogadorRanking = replyDoc.createElement("jogadorRanking");
				    	jogadorRanking.setAttribute("nome", key);
				    	jogadorRanking.setAttribute("vitorias", jogadorVitorias.get(key).toString());
				    	reply.appendChild(jogadorRanking);
				    }
				}
			}
			
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
        
        String replyString = "";
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(replyDoc);
			transformer.transform(source, result);
			replyString = result.getWriter().toString();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		
		return replyString;	
	}
	
	private String processarPedido(String pedido) {
		try {
			if (!server.validarPedido(pedido)) {
				return "Erro na estruturação da mensagem! Por favor repetir pedido...";
			};
		} catch (SAXException e) {
		}
		
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
	    String reply = "";
	    
	   switch (metodoNome) {
			case "jogar":
				reply = jogar();
				break;
			case "mostrarFoto":
				reply = mostrarFoto();
				break;
			case "randomizar":
				reply = randomizar();
				break;
			case "ranking":
				reply = mostrarRanking();
				break;
			case "logout":
				reply = logout();
	   }
	   return reply;
	}

}
