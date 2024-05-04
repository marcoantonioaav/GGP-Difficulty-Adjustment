import java.util.ArrayList;
import java.util.List;

import game.Game;
import other.AI;
import other.GameLoader;
import other.context.Context;
import other.model.Model;
import other.trial.Trial;
import search.mcts.MCTS;
import search.minimax.AlphaBetaSearch;

public class DifficultyAdjustmentTournament {

	private DifficultyAdjustmentTournament() {}

	public static void main(final String[] args) {
		float alpha = 3;
		float beta_start = 2;
		float beta_step = 1;
		float beta_end = 5;

		//List<String> algs = List.of("UCT");
		
		List<String> GAME_NAMES = List.of("Tic-tac-toe.lud", "Tapatan.lud"/*, "Alquerque.lud", "Reversi.lud"*/);
		int MAX_STEPS = 100;
		int NUM_GAMES = 20;
		double THINKING_TIME = 3.0;
		
		for(String gameName : GAME_NAMES) {
			System.out.println("New game.");
			for (float beta = beta_start; beta <= beta_end; beta += beta_step) {
				for (float multiplier = 1; multiplier <= 2; multiplier += 0.5f) {

					float win_rate_sum = 0;
					float lose_rate_sum = 0;
					float game_count = 0;

					System.out.println("beta: " + beta);
					System.out.println("multiplier: " + multiplier);
					
					final BetaStochasticUCT agent1 = new BetaStochasticUCT(alpha*multiplier, beta*multiplier);
					final BetaStochasticUCT agent2 = new BetaStochasticUCT(alpha, beta_start);
					
					final Game game = GameLoader.loadGameFromName(gameName);
		
					final Trial trial = new Trial(game);
					final Context context = new Context(game, trial);
					
					for (int gameCounter = 0; gameCounter < NUM_GAMES; ++gameCounter)
					{
						game.start(context);
						System.out.println("Game : " + game.name());
		
						final List<AI> ais = listAI(agent1, agent2, gameCounter);
						for (int player = 1; player < ais.size(); ++player)
						{
							System.out.println("Player " + player + " : " + ais.get(player).friendlyName());
							ais.get(player).initAI(game, player);
						}
						
						int steps = 0;
		
						final Model model = context.model();
		
						while (!context.trial().over() && steps <= MAX_STEPS)
						{
							model.startNewStep(context, ais, THINKING_TIME);
							steps++;
						}
						
						System.out.println("Outcome = " + context.trial().status());
						System.out.println("===============================");
						System.out.println("Game ended.");
						System.out.println("===============================");
						if(context.trial().status() != null){
							game_count += 1;
							if((""+context.trial().status()).contains("1") && game_count % 2 == 1) {
								win_rate_sum += 1;
							}
							else if((""+context.trial().status()).contains("2") && game_count % 2 == 0) {
								win_rate_sum += 1;
							}
							if((""+context.trial().status()).contains("1") && game_count % 2 == 0) {
								lose_rate_sum += 1;
							}
							else if((""+context.trial().status()).contains("2") && game_count % 2 == 1) {
								lose_rate_sum += 1;
							}
						}
					}
					
					float win_rate = win_rate_sum/game_count;
					float lose_rate = lose_rate_sum/game_count;
					System.out.println("Win rate:" + win_rate);
					System.out.println("Lose rate:" + lose_rate);
					System.out.println("===============================");
					System.out.println("Game batch ended.");
					System.out.println("===============================");
				}
			}
		}
		
		System.out.println("===============================");
		System.out.println("End.");
		System.out.println("===============================");
	}

	private static List<AI> listAI(AI ai1, AI ai2, int gameCounter) {
		List<AI> ais = new ArrayList<>();
		ais.add(null);
		ais.add(null);
		ais.add(null);
		ais.set(gameCounter%2 +1, ai1);
		ais.set((1 - gameCounter%2) + 1, ai2);
		return ais;
	}
}