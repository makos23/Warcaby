package prk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Scanner;

public class IOFile {
	private static final String dataFile = "C:\\Users\\GRAV40123\\Desktop\\data.txt";
	/**
	 * Metoda zapisuj¹ca dane o grze do pliku txt na serwerze
	 * @throws IOException -wyj¹tek rzucany podczas wyst¹pienia b³êdy w czasie zapisu danych do pliku 
	 */
	public static synchronized void saveData(boolean won,boolean tie,String whitePlayer,String redPlayer, char mark) throws IOException {
		PrintWriter out = null;
		try {
			File file = new File(dataFile);
			if (file != null) {
				out = new PrintWriter(new FileWriter(file.getPath(),true));
				out.printf("%-10s %-10s %-10s %-10s %n", whitePlayer, redPlayer,
						(won == true || tie == true) ? "Ended" : "notEnded",
						won == true ? mark == 'W'?whitePlayer:redPlayer: "---");
			}
		} catch (FileNotFoundException e) {
		} finally {
			if (out != null)
				out.close();
		}
	}
	public static void loadData(PrintWriter output) {
		
		File file = new File(dataFile);
		if (file != null) {
			
			Scanner in = null;
			String whitePlayer, redPlayer, gameStatus, winner;
			try {
				in = new Scanner(Paths.get(file.getPath()));
				while (in.hasNext()) {
					whitePlayer = in.next();
					redPlayer = in.next();
					gameStatus = in.next();
					winner = in.next();
					output.println("DATA" + whitePlayer + "," + redPlayer + "," + gameStatus + "," + winner);
				}
			} catch (IOException e) {
			} finally {
				if (in != null)
					in.close();
			}
		}
	}

}
