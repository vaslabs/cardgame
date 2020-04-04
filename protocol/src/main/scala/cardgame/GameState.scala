package cardgame
import GameOps._
import cats.effect.IO

case class GameState(gameProgress: LazyList[Action], game: Game, randomizer: IO[Int]) {

  def handleAction(action: Action): Option[(Event, GameState)] =
    game match {
      case _: FinishedGame =>
        None
      case playableGame =>
        val (game, event) = playableGame.action(action)
        Some(event -> GameState(gameProgress.drop(1), game, randomizer))
    }


  def start =
    LazyList.unfold[Event, GameState](this) {
    gameState =>
      gameState.gameProgress.headOption.flatMap { nextCommand =>
        gameState.handleAction(nextCommand)
      }
  }
}
