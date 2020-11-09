package bot

import board.Board

interface IBot {
    fun makeTurn(state: Board.BoardState): Board.Point?
}