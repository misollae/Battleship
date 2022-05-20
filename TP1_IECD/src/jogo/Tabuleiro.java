package jogo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Tabuleiro {
	private ArrayList<String> tirosSofridos      = new ArrayList<String>();
	private Map<String, ArrayList<String>> ships = new HashMap<String, ArrayList<String>>();
	private String nicknameJogador;

	private static final Map<String, Integer> shipLength = new HashMap<String, Integer>();
	private static final Map<String, String>    shipName = new HashMap<String, String>();
	
	/* Construtor	
	 * 	Construtor simples usado na inicialização de um novo tabuleiro.
	 */ 
	public Tabuleiro(String nicknameJogador) {
		this.nicknameJogador = nicknameJogador;
		
		ships.put("P",   new ArrayList<String>());
		ships.put("T1",  new ArrayList<String>());
		ships.put("T2",  new ArrayList<String>());
		ships.put("C1",  new ArrayList<String>());
		ships.put("C2",  new ArrayList<String>());
		ships.put("C3",  new ArrayList<String>());
		ships.put("S1",  new ArrayList<String>());
		ships.put("S2",  new ArrayList<String>());
		ships.put("S3",  new ArrayList<String>());
		ships.put("S4",  new ArrayList<String>());

		shipLength.put("P", 5);
		shipLength.put("T", 4);
		shipLength.put("C", 3);
		shipLength.put("S", 2);
		
		shipName.put("P", "Porta-Aviões");
		shipName.put("T", "Navio Tanque");
		shipName.put("C", "Contratorpedeiro");
		shipName.put("S", "Submarino");
		
		randomize();
	}
	
	/* Construtor	
	 * 	Construtor usado na recuperação do tabuleiro.
	 */ 
	public Tabuleiro(String nicknameJogador, Map<String, ArrayList<String>> ships, ArrayList<String> tirosSofridos) {
		this.nicknameJogador = nicknameJogador;
		this.ships = ships;
		this.tirosSofridos = tirosSofridos; 
		
		shipLength.put("P", 5);
		shipLength.put("T", 4);
		shipLength.put("C", 3);
		shipLength.put("S", 2);
		
		shipName.put("P", "Porta-Aviões");
		shipName.put("T", "Navio Tanque");
		shipName.put("C", "Contratorpedeiro");
		shipName.put("S", "Submarino");
	}
	
	/* randomize()	
	 * 	Método para randomizar o tabuleiro, chama colocarBarco() para cada barco do jogo, enviando uma linha, coluna e 
	 *  direção randomizadas até que a posição seja validada e o barco adicionado ao XML.
	 */ 	
	private void randomize() {
		Random rand = new Random();
		
		// 1 porta-aviões (cinco quadrados)
		while (!colocarNavio("P", rand.nextInt(10) + 1, rand.nextInt(10) + 1, rand.nextInt(4) + 1));
		
		// 2 navios-tanque (quatro quadrados)
		while (!colocarNavio("T1", rand.nextInt(10) + 1, rand.nextInt(10) + 1, rand.nextInt(4) + 1));
		while (!colocarNavio("T2", rand.nextInt(10) + 1, rand.nextInt(10) + 1, rand.nextInt(4) + 1));
		
		// 3 contratorpedeiros (três quadrados)
		while (!colocarNavio("C1", rand.nextInt(10) + 1, rand.nextInt(10) + 1, rand.nextInt(4) + 1));
		while (!colocarNavio("C2", rand.nextInt(10) + 1, rand.nextInt(10) + 1, rand.nextInt(4) + 1));
		while (!colocarNavio("C3", rand.nextInt(10) + 1, rand.nextInt(10) + 1, rand.nextInt(4) + 1));
		
		// 4 submarinos (dois quadrados)
		while (!colocarNavio("S1", rand.nextInt(10) + 1, rand.nextInt(10) + 1, rand.nextInt(4) + 1));
		while (!colocarNavio("S2", rand.nextInt(10) + 1, rand.nextInt(10) + 1, rand.nextInt(4) + 1));
		while (!colocarNavio("S3", rand.nextInt(10) + 1, rand.nextInt(10) + 1, rand.nextInt(4) + 1));
		while (!colocarNavio("S4", rand.nextInt(10) + 1, rand.nextInt(10) + 1, rand.nextInt(4) + 1));
	}

	
	/* colocarNavio()
	 * 	Método que, enviando um tipo de barco, averigua se dada linha, coluna e direção representam uma posição
	 * 	válida para o barco, adicionando-o à estrutura de jogo. 
	 * 
	 * 	@params tipoBarco - Barco que se pretente colocar
	 *  @params linha,
	 *          coluna, 
	 *          direcao   - inteiros representativos da posição/casas a ocupar
	 *  @return booleano que indica se foi ou não adicionado à estrutura de jogo
	 */
	private boolean colocarNavio(String tipoBarco, int linha, int coluna, int direcao) {
        // Direção:
        // 1 -> Norte      2 -> Este        3 -> Sul         4 -> Oeste
        int dir = (direcao == 1 || direcao == 4) ?  -1 : 1;
        int dimBarco = shipLength.get(Character.toString(tipoBarco.charAt(0)));
        
        for (int pos = 0; pos < dimBarco; pos++) {
            if (direcao == 1 || direcao == 3) {
                for (int l = linha + (dir*pos) - 1; l <= linha + (dir*pos) + 1; l++) {
                    for (int c = coluna - 1; c <= coluna + 1; c++) {
                    	String posFormat = (char) (l + 64) + String.valueOf(c);
                		if ((temBarco(posFormat) && getBarco(posFormat) != tipoBarco)  || !dentroDoTabuleiro(linha + (dir*pos), coluna)) {
                            ships.get(tipoBarco).clear();
                            return false;
                        }
                    }
                }
                ships.get(tipoBarco).add((char)(linha  + (dir*pos) + 64) + String.valueOf(coluna));
            }
            
            if (direcao == 2 || direcao == 4) {
                for (int l = linha - 1; l <= linha + 1; l++) {
                    for (int c = coluna + (dir*pos) - 1; c <= coluna + (dir*pos) + 1; c++) {
                    	String posFormat = (char) (l + 64) + String.valueOf(c);
                    	if ((temBarco(posFormat) && getBarco(posFormat) != tipoBarco) || !dentroDoTabuleiro(linha, coluna + (dir*pos))) {
                            ships.get(tipoBarco).clear();
                            return false;
                        }
                    }
                }
                ships.get(tipoBarco).add((char)(linha + 64) + String.valueOf(coluna  + (dir*pos)));
            }
        }
        return true;
    }
	
	/* dentroDoTabuleiro()
	 * 	Método que verifica se a posição enviada se encontra dentro do tabuleiro.
	 * 
	 *  @params linha,
	 *          coluna  -  inteiros representativos da posição a verificar
	 *  @return booleano que indica se a posição enviada está dentro do tabuleiro
	 */
	private boolean dentroDoTabuleiro(int linha, int coluna) {
		return !(linha < 1 || linha > 10 || coluna < 1 || coluna > 10);
	}
	
	/* tiroSofrido()
	 * 	Método que retorna se dada posição enviada tem um tiro dado pelo outro jogador.
	 * 
	 *  @params linha,
	 *          coluna  -  inteiros representativos da posição a verificar
	 *  @return booleano que indica se a posição já foi atingida
	 */
	public boolean tiroSofrido(String pos) {
        for (String tiro : tirosSofridos) {
            if (tiro.equals(pos)) {
                return true;
            }
        }
        return false;
    }
	
	/* getTiroSofrido()
	 * 	Método que retorna a representação de um tiro sofrido em dada posição.
	 * 
	 *  @params linha,
	 *          coluna  -  inteiros representativos da posição a verificar
	 *  @return representação do estado de tiro sofrido da posição enviada:
	 *         					 X   > Tiro numa posição com barco
	 *             				 O   > Tiro numa posição com água
	 *           				null > Sem tiro
	 */
	public String getTiroSofrido(String pos) {
		if (tiroSofrido(pos) && temBarco(pos)) {
			return "X";
		} else if (tiroSofrido(pos)) {
			return "O";
		}
		return null;
	}

	
	/* temBarco()
	 * 	Método que retorna se dada posição enviada tem um barco do jogador.
	 * 
	 *  @params linha,
	 *          coluna  -  inteiros representativos da posição a verificar
	 *  @return booleano que indica se a posição tem um barco
	 */
	public boolean temBarco(String pos) {
        for (String navio : ships.keySet()) {
            for (String posBarco : ships.get(navio)) {
                if (posBarco.equals(pos)) {
                    return true;
                }
            }
        }
        return false;
	}
	
	/* getBarco()
	 * 	Método que retorna a representação de um barco em dada posição 
	 * 
	 *  @params linha,
	 *          coluna  -  inteiros representativos da posição a verificar
	 *  @return representação do do barco presente na posição enviado (sigla identificativa
	 *  		do barco ou null, se não tiver barcos)
	 */
	public String getBarco(String pos) {
		for (String navio : ships.keySet()) {
            for (String posBarco : ships.get(navio)) {
                if (posBarco.equals(pos)) {
                    return navio;
                }
            }
        }
        return null;
	}	
	
	/* fimDeJogo()
	 * 	Método que verifica se o jogo já acabou (tendo o jogador dono do tabuleiro que a classe representa perdido).
	 * 
	 * 	@return booleano que infica fim do jogo e perda do jogador.
	 */
	public boolean fimDeJogo() {
		for (String tipoBarco : ships.keySet()) {
			if (!afundou(tipoBarco))
				return false;
		}
		return true;
	}

	/* afundou()
	 * 	Método que verifica se dado navio foi afundado.
	 * 
	 * 	@params barco a verificar.
	 * 	@return booleano se o navio enviado já foi afundado.
	 */
	private boolean afundou(String tipoBarco) {
	    for (String pos : ships.get(tipoBarco)) {
	    	if(!tiroSofrido(pos))
	    		return false;
	    }
	    return true;
	}

	/* afundou()
	 * 	Método que formula a resposta a um tiro dado pelo oponente.
	 * 
	 * 	@params posição onde o oponente atirou.
	 * 	@return resposta ao tiro dado.
	 * 				"Tiro no (barco atingido)!"  > Se o barco foi atingido mas não afundado.
	 * 				"(barco atingido) ao fundo!" > Se o barco foi atingido e afundado.
	 *              "Tiro na água!"              > Se o tiro não atingiu nenhum navio
	 */
	public String sofrerTiro(String pos) {
		if (!tirosSofridos.contains(pos)) {
			tirosSofridos.add(pos);
		}
		
		if (temBarco(pos)) {
			if(fimDeJogo()) return "O jogador venceu!!";
			String barcoAtingido = getBarco(pos);
			if (afundou(barcoAtingido)) {
				return shipName.get(Character.toString(barcoAtingido.charAt(0))) + " ao fundo!";
			} else {
				return "Tiro no " + shipName.get(Character.toString(barcoAtingido.charAt(0))) + "!";
			}
		}
		return "Tiro na água!";
	}
	
	/*	getNickname()
	 * 		Retorna o nome to jogador a quem pertence o tabuleiro.
	 * 
	 * 		@return nickname
	 */
	public String getNickname() {
		return this.nicknameJogador;
	}
	
	/*	getTirosSofridos()
	 * 		Retorna o array de tiros que o jogador já sofreu, facilitando a sua representação.
	 * 
	 * 		@return tiros sofridos
	 */
	public ArrayList<String> getTirosSofridos() {
		return this.tirosSofridos;
	}
	
	/*	getBarcos()
	 * 		Retorna o mapa do barcos do jogador e das suas posições, facilitando a sua representação.
	 * 
	 * 		@return barcos e posições
	 */
	public Map<String, ArrayList<String>> getBarcos() {
		return this.ships;
	}
}


