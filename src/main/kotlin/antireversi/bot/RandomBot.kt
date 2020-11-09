package bot

import board.Board

class RandomBot(private val board: Board) : IBot {

    override fun makeTurn(state: Board.BoardState): Board.Point? {
        val availableTurns = board.getAvailableTurns(state) ?: throw Bot.GameOverException()

        when {
            availableTurns.isEmpty() -> {
                return null
            }
            availableTurns.size == 1 -> {
                val turn = availableTurns[0]
                board.makeTurn(state, turn.x, turn.y)
                return turn
            }
        }

        val turn = availableTurns.random()
        board.makeTurn(state, turn.x, turn.y)
        return turn
    }
}