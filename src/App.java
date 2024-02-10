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

public class App {
	static final HybridMinimaxMCTS HYBRID_MINIMAX_MCTS = new HybridMinimaxMCTS();
	static final ModifiedUCT MODIFIED_UCT = new ModifiedUCT(0.5f, 0.3f);
	static final MCTS LUDII_MCTS = MCTS.createUCT();
	static final AlphaBetaSearch LUDII_ALPHA_BETA = AlphaBetaSearch.createAlphaBeta();
	static final HybridMinimaxMCTS HYBRID_MINIMAX_MCTS2 = new HybridMinimaxMCTS();
	static final List<String> GAME_NAMES = List.of("Tic-tac-toe.lud", "Tapatan.lud", "Alquerque.lud", "Reversi.lud");
	static final List<String> GAME_NAMES2 = List.of("Alquerque.lud");
	static final int MAX_STEPS = 100;
	static final int NUM_GAMES = 20;
	static final double THINKING_TIME = 5.0;

	private App() {}

	public static void main(final String[] args) {
		//HYBRID_MINIMAX_MCTS.setMean(0.5f);
		//HYBRID_MINIMAX_MCTS.setStandardDeviation(0.3f);
		for(String gameName : GAME_NAMES2) {
			final Game game = GameLoader.loadGameFromName(gameName);

			final Trial trial = new Trial(game);
			final Context context = new Context(game, trial);
			
			for (int gameCounter = 0; gameCounter < NUM_GAMES; ++gameCounter)
			{
				game.start(context);
				System.out.println("Game : " + game.name());

				final List<AI> ais = getAIs(gameCounter);
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
				System.out.println("First reached depth = " + HYBRID_MINIMAX_MCTS.getFirstReachedDepth());
				System.out.println("Mean reached depth = " + HYBRID_MINIMAX_MCTS.getMeanReachedDepth());
				System.out.println("Mean spent time = " + HYBRID_MINIMAX_MCTS.getMeanSpentTimeSeconds() + "s");
				System.out.println("===============================");
			}
		}
	}

	private static List<AI> getAIs(int gameCounter) {
		List<AI> ais = new ArrayList<>();
		ais.add(null);
		ais.add(null);
		ais.add(null);
		ais.set(gameCounter%2 +1, HYBRID_MINIMAX_MCTS);
		ais.set((1 - gameCounter%2) + 1, LUDII_MCTS);
		return ais;
	}
}