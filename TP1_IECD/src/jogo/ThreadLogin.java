package jogo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Socket;

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


class ThreadLogin extends Thread {

	private Socket playerConnection;
	private Document doc;
	private BufferedReader is;
	private PrintWriter os;
	private boolean conectou;
	private Servidor server;
	private String nickname;

	public ThreadLogin(Socket playerConnection, Document doc, Servidor server) {
		this.playerConnection = playerConnection;
		this.doc    = doc;
		this.server = server;
		try {
			is = new BufferedReader(new InputStreamReader(playerConnection.getInputStream()));
			os = new PrintWriter(playerConnection.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		while(!conectou) {	
			try {
				String pedido = is.readLine();
				if (pedido != null)
					os.println(processarPedido(pedido));
			} catch (IOException e1) {
				System.out.println("Desconectado...");
				return;
			}
		}
		new EsperaThread(playerConnection, doc, server, this.nickname).start();
	} 
	
	private String formularResposta(String metodoNome, String conteudo) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		Document replyDoc = null;
		try {
			builder = dbf.newDocumentBuilder();
			replyDoc = builder.newDocument();
			
			Element protocolo = replyDoc.createElement("protocolo");
			replyDoc.appendChild(protocolo);
			Element metodo = replyDoc.createElement(metodoNome);
			protocolo.appendChild(metodo);
			Element reply = replyDoc.createElement("resposta");
			metodo.appendChild(reply);
			reply.setTextContent(conteudo);
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
	
	public String processarPedido(String pedido) {	
				
		try {
			if (!server.validarPedido(pedido)) {
				return "Erro na estruturação da mensagem! Por favor repetir pedido...";
			};
		} catch (SAXException e) {
		}
		
		Document docAux = null;
	    try {
	    	DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    	InputSource is = new InputSource();
	    	is.setCharacterStream(new StringReader(pedido));
	    	docAux = db.parse(is);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			System.out.println("Falha do utilizador na conexão!");
		} 
	    
	    Node protocolo     = docAux.getElementsByTagName("protocolo").item(0);
	    Node metodo        = protocolo.getFirstChild();
	    String metodoNome  = metodo.getNodeName();
	    String nickname    = metodo.getFirstChild().getFirstChild().getTextContent();
	    String password    = metodo.getFirstChild().getLastChild().getTextContent();
	    String reply = "";
	    
	    switch(metodoNome) {
	    	case("validar"):
	    		reply = validar(nickname, password);
	    		break;
	    	case("registar"):
	    		reply = registar(nickname, password);
	    		break;
	    }
	   return reply;
	}

	private String validar(String nickname, String password) {
		String estadoLogin = "Login Inválido";
		
		Element servidor = doc.getDocumentElement();
		Element utilizadores = (Element) servidor.getElementsByTagName("jogadores").item(0);
		NodeList jogadores = utilizadores.getElementsByTagName("jogadorInscrito");
		for (int i = 0; i < jogadores.getLength(); i++) {
			String nomeJogador = jogadores.item(i).getAttributes().getNamedItem("nome").getNodeValue();
			String passwordJogador = jogadores.item(i).getAttributes().getNamedItem("password").getNodeValue();
			if (nickname.equals(nomeJogador) && password.equals(passwordJogador) && !server.isConnected(nickname)) {
				estadoLogin = "Login validado!";
				conectou    = true;
				this.nickname    = nomeJogador;
				server.adicionarLigacao(nomeJogador, playerConnection);
			}
		}
		
        String replyString = formularResposta("validar", estadoLogin);
		return replyString;	
	}
	
	private String registar(String nickname, String password) {
				
		String estadoRegisto = "Conta criada!";
		
		Element servidor = doc.getDocumentElement();
		Element utilizadores = (Element) servidor.getElementsByTagName("jogadores").item(0);
		NodeList jogadores = utilizadores.getElementsByTagName("jogadorInscrito");
		for (int i = 0; i < jogadores.getLength(); i++) {
			String nomeJogador = jogadores.item(i).getAttributes().getNamedItem("nome").getNodeValue();
			if (nickname.equals(nomeJogador)) {
				estadoRegisto = "Já existe uma conta com esse username!";
			}
		}
		
		if (estadoRegisto.equals("Conta criada!")) {
			conectou    = true;
			this.nickname = nickname;
			server.adicionarRegisto(nickname, password);
			server.adicionarLigacao(nickname, playerConnection);
		}
        
        String replyString = formularResposta("registar", estadoRegisto);
		return replyString;	
	}
} 