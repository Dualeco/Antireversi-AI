package antireversi

import board.Board
import board.BoardImpl
import bot.Bot
import bot.IBot
import bot.RandomBot
import java.lang.Exception

fun main(args: Array<String>) {
    val cmdWorker = CommandLineWorker()
    cmdWorker.runGame()
    //runTesting()
}

fun runTesting() {
    val list = arrayListOf<Char>()
    val blackHoleX = 2.toByte()
    val blackHoleY = 4.toByte()

    repeat(100) {
        val board = BoardImpl(blackHoleX, blackHoleY)
        val state = BoardImpl.BoardStateImpl(blackHoleX, blackHoleY)

        println("Attempt $it")
        try {

            var turn = 0
            fun isBlackTurn() = turn % 2 == 0

            val botBlack: IBot = Bot(board, 4)
            val botWhite: IBot = RandomBot(board)

            var prevState = state.copyState()
            var prevStateCounter = 3
            runGame {
                (if (isBlackTurn()) botBlack else botWhite).makeTurn(state)

                if (prevState.isEqual(state)) {
                    prevStateCounter--
                    if (prevStateCounter <= 0) throw Bot.GameOverException()
                } else {
                    prevStateCounter = 3
                }
                prevState = state.copyState()

                turn++
                state.inverseState()
            }
            state.display()

            val score = state.getScore(weighed = false)
            val scoreB = if (isBlackTurn()) score.x else score.y
            val scoreW = if (isBlackTurn()) score.y else score.x

            val str = when {
                scoreB < scoreW -> {
                    "B won | Score B: $scoreB | Score W: $scoreW ".also { list += 'B' }
                }
                else -> {
                    "W won | Score B: $scoreB | Score W: $scoreW ".also { list += 'W' }
                }
            }
            println(
                "${
                    (list.count { it == 'B' }.toFloat() / list.size) * 100
                }%, ${list.count { it == 'B' }} / ${list.count { it == 'W' }}, $str"
            )
            println()
            println()
        } catch (e: Exception) {
            println("Error: ${e.message}")
        } finally {
            //state.display()
        }
    }
}

fun runGame(action: () -> Unit) {
    var isGameOver = false
    do {
        try {
            action()
        } catch (e: Bot.GameOverException) {
            isGameOver = true
        }
    } while (!isGameOver)
}