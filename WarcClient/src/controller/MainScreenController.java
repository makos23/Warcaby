package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import model.Data;
import model.Dialog;
import model.Piece;
import model.PieceType;
import model.Tile;

/**
 * Klasa controlera glownego ekranu programu
 */
public class MainScreenController implements Runnable {
	private ObservableList<Data> results = FXCollections.observableArrayList();
	public ObservableList<Data> getResults() {
		return results;
	}
	private Stage primaryStage;
	ControllerUser controllerUser;
	private BufferedReader in;
	private PrintWriter out;
	private IntegerProperty port = new SimpleIntegerProperty(22222);
	private Socket socket;
	private Thread thread;
	private boolean yourTurn = false,accepted = false;
	private String ip;
	private String waitingString = "Oczekiwanie na drugiego gracza";
	private String wonString = "Wygra�e�";
	private String enemyWonString = "Przeciwnik wygra�";
	private String tieString = "Gra zako�czona remisem";
	private char playerType;
	private Tile[][] tileMatrix = new Tile[8][8];
	private Group tileGroup = new Group(),pieceGroup = new Group(); 
	@FXML
	private BorderPane borderPane;
	@FXML
	private CheckMenuItem alwaysOnTop;
	@FXML
	private Pane gamePool;
	@FXML
	private Label myTurn,myNick,oppTurn,enemyNick,LabelGamemssgs,labelEnemyNick;	
	@FXML
	private Ellipse myElipse,oppElipse;
	public Label getMyNick() {
		return myNick;
	}
	@FXML
	private MenuItem myGames;

	public void setStage(ControllerUser controllerUser, Stage primaryStage) throws InterruptedException {
		this.controllerUser = controllerUser;
		this.primaryStage = primaryStage;
		this.primaryStage.setResizable(false);
		ip = controllerUser.getIp();
		connect();
		primaryStage.setOnCloseRequest(value -> {
			out.println("QUIT");
			Platform.exit();
			System.exit(0);
		});
		myNick.setText(controllerUser.getLocaluser());
		myNick.setVisible(true);
	};

	public enum Turn {
		MyTurn, OpponentTurn, None
	}

	/**
	 * Metoda ustawiajaca widocznosc element�w informujacych graczy czyja kolej
	 * 
	 * @param turn
	 *            - wartosc klasy wyliczeniowej Turn
	 */
	private void displayTurn(Turn turn) {
		switch (turn) {
		case MyTurn:
			myElipse.setVisible(true);
			myTurn.setVisible(true);
			oppElipse.setVisible(false);
			oppTurn.setVisible(false);
			myNick.setVisible(true);
			enemyNick.setVisible(true);
			labelEnemyNick.setVisible(true);
			yourTurn=true;
			break;
		case OpponentTurn:
			myElipse.setVisible(false);
			myTurn.setVisible(false);
			oppElipse.setVisible(true);
			oppTurn.setVisible(true);		
			myNick.setVisible(true);
			enemyNick.setVisible(true);
			labelEnemyNick.setVisible(true);
			yourTurn=false;
			break;
		case None:
			myElipse.setVisible(false);
			myTurn.setVisible(false);
			oppElipse.setVisible(false);
			oppTurn.setVisible(false);
			myGames.setDisable(false);
			myNick.setVisible(true);
			enemyNick.setVisible(false);
			labelEnemyNick.setVisible(false);
			yourTurn=false;
			break;
		}
	}

	/**
	 * Metoda tworzaca plansze do gry oraz ustawiajaca na niej pionki
	 */
	private void createContent() {
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				Tile tile = new Tile((y + x) % 2 == 0, x, y);
				tileMatrix[x][y] = tile;
				tileGroup.getChildren().add(tile);
				Piece piece = null;
				if ((y + x) % 2 != 0 && y < 3)
					piece = movePiece(PieceType.RED, x, y, false);
				if ((y + x) % 2 != 0 && y > 4)
					piece = movePiece(PieceType.WHITE, x, y, false);
				if (piece != null) {
					pieceGroup.getChildren().add(piece);
					tile.setPiece(piece);
				}
			}
		}
	}

	/**
	 * Metoda przeliczajca wsp�rzedne okna na wspo�rzedne planszy
	 * 
	 * @param pixel
	 *            - wspolrzedna okna
	 * @return wspolrzedna bedaca po�ozeniem pionka na planszy
	 */
	private int toBoard(double pixel) {
		int a = (int) (pixel + Tile.getTileSize() / 2) / Tile.getTileSize();
		return a;
	}

	/**
	 * Metoda wysy�aj�ca wiadomosc do serwera po przeciagnieciu pionka badz tez
	 * anulujaca ruch pionka
	 * 
	 * @param type
	 *            - typ pionka
	 * @param x
	 *            - wspolrzedna X pionka
	 * @param y
	 *            - wspolrzedna Y pionka
	 * @param isQueen
	 *            - parametr mowiacy o tym czy pionek jest damka
	 * @return metoda zwraca opbiekt klasy Piece reprezentujacy pionek
	 */
	public Piece movePiece(PieceType type, int x, int y, boolean isQueen) {
		Piece piece = new Piece(type, x, y, isQueen);
		piece.setOnMouseReleased(e -> {
			int newX = toBoard(piece.getLayoutX());
			int newY = toBoard(piece.getLayoutY());
			if (accepted && yourTurn)
				out.println("MOVE " + x + y + newX + newY);
			else
				piece.abortMove();
		});
		return piece;
	}

	/**
	 * Metoda odswiezajaca plansze po ruchu
	 * 
	 * @param s
	 *            - string z stanem gry
	 */
	private void repaint(String s) {
		pieceGroup.getChildren().remove(0, pieceGroup.getChildren().size());
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				Tile tile = tileMatrix[x][y];
				Piece piece = null;
				if (s.charAt(x + y * 8) == '1') {
					piece = movePiece(PieceType.WHITE, x, y, false);
					tile.setPiece(piece);
				}
				if (s.charAt(x + y * 8) == '2') {
					piece = movePiece(PieceType.WHITE, x, y, true);
					tile.setPiece(piece);
				}
				if (s.charAt(x + y * 8) == '3') {
					piece = movePiece(PieceType.RED, x, y, false);
					tile.setPiece(piece);
				}
				if (s.charAt(x + y * 8) == '4') {
					piece = movePiece(PieceType.RED, x, y, true);
					tile.setPiece(piece);
				}
				if (s.charAt(x + y * 8) == '0')
					tile.setPiece(null);

				if (piece != null) {
					pieceGroup.getChildren().add(piece);
				}
			}
		}
	}

	/**
	 * Metoda ktora w zaleznosci od poczatku odebranej wiadomosci: -"WELCOME"-
	 * ustawia typ pionk�w dla gracza - MESSAGE Waiting ...- ustawia komentarz
	 * oczekiwania na przeciwnika - OPPONENTNICK-ustawia nick przeciwnika na i
	 * wyswietla go na planszy - MSG Your move- albo MSG Opponent move- wyswietla
	 * elementy informujaca o kolejnosci ruchow -MOREKILL - informuje gracza o
	 * dalszym przymusowym biciu -VALIDMOVE- informuje o poprawnosci ruchu i zmienia
	 * elementy t�a -INVALIDMOVE - informuje o b��dnym ruchu -OPPONENTMOVE- odswieza
	 * plansze po ruchu przeciwnika -VICTORY,DEFEAT,TIE-ustawia odpowiedni komunikat
	 * i wylacza elenenty informujace o kolejce gracza
	 * 
	 * @throws IOException
	 */
	public void play() throws IOException {
		String response;
		
		try {
			response = in.readLine();
			if (response.startsWith("WELCOME")) {
				playerType = response.charAt(8);
			} else if (response.startsWith("MESSAGE Waiting for opponent to connect")) {
				displayTurn(Turn.None);
				showMsg(waitingString);
			} else if (response.startsWith("OPPONNENTNICK")) {
				javafx.application.Platform.runLater(new Runnable() {
					@Override
					public void run() {
						enemyNick.setText(response.replace("OPPONNENTNICK", ""));
					}
				});
			} else if (response.startsWith("MESSAGE All players connected")) {
				javafx.application.Platform.runLater(new Runnable() {
					@Override
					public void run() {
						out.println("NICK" + playerType + myNick.getText());
					}
				});
				accepted = true;
				showMsg("Gramy!");
			} else if (response.startsWith("MSG Your move")) {
				setElipseColor();
				displayTurn(Turn.MyTurn);
			} else if (response.startsWith("MSG Opponen move")) {
				setElipseColor();
				displayTurn(Turn.OpponentTurn);
			} else if (response.startsWith("MOREKILL")) {
				showMove(response.substring(8));
			} else if (response.startsWith("VALID_MOVE")) {
				showMove(response.substring(10));
				displayTurn(Turn.OpponentTurn);
			} else if (response.startsWith("OPPONENT_MOVED")) {
				showMove(response.substring(15));
				displayTurn(Turn.MyTurn);
			} else if (response.startsWith("VICTORY")) {
				showMsg(wonString);
				displayTurn(Turn.None);
			} else if (response.startsWith("DEFEAT")) {
				showMsg(enemyWonString);
				displayTurn(Turn.None);
			} else if (response.startsWith("TIE")) {
				showMsg(tieString);
				displayTurn(Turn.None);
			} else if (response.startsWith("INVALID_MOVE")) {
				showMove(response.substring(12));
			} else if (response.startsWith("DATA")) {
				results.add(addToResults(response.substring(4)));
			} else if (response.startsWith("PLAYAGAIN")) {
				javafx.application.Platform.runLater(new Runnable() {
					@Override
					public void run() {
						playAgain();
					}
				});
			} else if (response.startsWith("YES")) {
				showMsg("Gramy jeszcze raz");
				javafx.application.Platform.runLater(new Runnable() {
					@Override
					public void run() {
						nextPlay();
					}
				});
			} else if (response.startsWith("NO")) {
				showMsg("Dziekuje za gre");
				displayTurn(Turn.None);
				clearBoard();
				
			} else if (response.startsWith("END")) {
				showMsg("Przeciwnik zrezygnowa� z gry");
				displayTurn(Turn.None);
				clearBoard();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void clearBoard() {
		javafx.application.Platform.runLater(new Runnable() {
			@Override
			public void run() {
				pieceGroup.getChildren().clear();
			}
		});
	}

	/**
	 * Metoda wzpeniajca liste z wynikami gier
	 * @param s- rekord z danymi z serwera o jednej grze
	 * @return metoda zwraca obiekt typu Data
	 */
	public Data addToResults(String s) {
		int i = 0, j = 0;
		String whitePlayer = null, redPlayer = null, gameStatus = null, winner = null;
		for (int k = 1; k <= 3; k++) {
			i = s.indexOf(",", i + 1);
			if (k == 1)
				whitePlayer = s.substring(j, i);
			if (k == 2)
				redPlayer = s.substring(j + 1, i);
			if (k == 3)
				gameStatus = s.substring(j + 1, i);
			j = i;
		}
		winner = s.substring(j + 1);
		return new Data(whitePlayer, redPlayer, gameStatus, winner);
	}

	/**
	 * Metoda ustawiajaca okreslony text w LabelGamemssgs
	 * 
	 * @param msg
	 *            - teks do wyswietlenia
	 */
	private void showMsg(String msg) {
		javafx.application.Platform.runLater(new Runnable() {
			@Override
			public void run() {
				LabelGamemssgs.setText("Komunikat:" + msg);
			}
		});
	}

	/**
	 * Metoda wywo�ujaca metode repaint z odpowiednim stringiem
	 * 
	 * @param msg
	 *            - string z aktualnum stanem gry
	 */
	private void showMove(String msg) {
		javafx.application.Platform.runLater(new Runnable() {
			@Override
			public void run() {
				repaint(msg);
			}
		});
	}

	/**
	 * Metoda tworzaca po��czenie z serwerem ustawiajaca strumienie wymiany danych
	 * tworzaca i uruchamiajaca watek thread
	 * 
	 * @throws InterruptedException
	 */
	private void connect() throws InterruptedException {
		try {
			socket = new Socket(ip, port.intValue());
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			thread = new Thread(this, "MainScreenController");
			thread.setDaemon(true);
			thread.start();
			myGames.setDisable(false);
		} catch (IOException e) {
		}
	}
  
	/**
	 * Metoda wywo�ywana przy tworzeniu tego kontrolera
	 * @throws Exception
	 */
	@FXML
	private void initialize() throws Exception {
		createContent();
		gamePool.getChildren().addAll(tileGroup, pieceGroup);
		displayTurn(Turn.None);
	}

	/**
	 * Metoda zamyka program
	 */
	@FXML
	void closeApplication() {
		out.println("QUIT");
		Platform.exit();
		System.exit(0);
	}

	/**
	 * Ustawia arkusz styl�w na classic.css
	 */
	@FXML
	void setClassic() {
		Stage stage = (Stage) borderPane.getScene().getWindow();
		stage.getScene().getStylesheets().clear();
		stage.getScene().getStylesheets().add(getClass().getResource("classic.css").toExternalForm());
	}

	/**
	 * Ustawia arkusz styl�w na modern.css
	 */
	@FXML
	void setModern() {
		Stage stage = (Stage) borderPane.getScene().getWindow();
		stage.getScene().getStylesheets().clear();
		stage.getScene().getStylesheets().add(getClass().getResource("modern.css").toExternalForm());
	}

	/**
	 * Motoda ustawia okno zawsze na wierzchu lub wy��cza t� opcje
	 * 
	 * @param event
	 *            - event checkBoxa
	 */
	@FXML
	void setOnTop(ActionEvent event) {
		Stage stage = (Stage) borderPane.getScene().getWindow();
		boolean value = ((CheckMenuItem) event.getSource()).selectedProperty().get();
		stage.setAlwaysOnTop(value);
	}

	/**
	 * Metoda wyswietka okno w trybie komunikatu z zasadani gry
	 */
	@FXML
	void about() {
		Dialog.rulesDialog();
	}

	/**
	 * Metoda otwieraj�ca okno z mozliwoscia obejrzenia rankingu oraz gier
	 * uzytkownika
	 */
	@FXML
	void openMyGames() {
		if (accepted) {
			results.clear();
			out.println("DATA");
		}
		try {
			FXMLLoader loader = new FXMLLoader(Main.class.getResource("/view/MyGames.fxml"));
			Pane gamePane = loader.load();
			Stage gameWindowStage = new Stage();
			gameWindowStage.setTitle("Moje Gry");
			gameWindowStage.initModality(Modality.WINDOW_MODAL);
			gameWindowStage.initOwner(primaryStage);
			gameWindowStage.setMinWidth(500.0);
			gameWindowStage.setMinHeight(500.0);
			Scene scene = new Scene(gamePane);
			gameWindowStage.setScene(scene);
			GameScreenController gsc = loader.getController();
			gsc.setMyGameStage(gameWindowStage);
			gsc.setMainScreenController(this);
			gameWindowStage.showAndWait();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				play();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
    
	/**
	 * Setter pola BufferedReader
	 * @param in
	 */
	public void setIn(BufferedReader in) {
		this.in = in;
	}

	/**
	 * Getter pola PlayerType
	 * @return
	 */
	public char getPlayerType() {
		return playerType;
	}

	/**
	 * Metoda otwieraj�ca okno z zapytaniem czy chcemy grac dalej
	 */
	public void playAgain() {
		ButtonType result = Dialog.confirmDialog();
		if (result == ButtonType.YES)
			out.println("PLAYAGAINYES");
		else if (result == ButtonType.NO)
			out.println("PLAYAGAINNO");
	}

	/**
	 * Metoda ustawiajaca plansze i pola gracz do kolejnej gry
	 */
	public void nextPlay() {
		tileGroup.getChildren().clear();
		pieceGroup.getChildren().clear();
		createContent();
		if (playerType == 'W') {
			playerType = 'R';
			setElipseColor();
			displayTurn(Turn.OpponentTurn);
			return;
		} else {
			playerType = 'W';
			setElipseColor();
			displayTurn(Turn.MyTurn);
			return;
		}
	}

	/**
	 * Metoda do ustawiania koloru elipsy taki sam jak kolor pionka danego gracza
	 */
	public void setElipseColor() {
		if (playerType == 'W') {
			myElipse.setFill(Color.valueOf("#fff9f4"));
			oppElipse.setFill(Color.valueOf("#c40003"));
		} else {
			myElipse.setFill(Color.valueOf("#c40003"));
			oppElipse.setFill(Color.valueOf("#fff9f4"));
		}
	}

}