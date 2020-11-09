package antireversi

import board.Board
import board.BoardImpl
import bot.Bot
import bot.IBot

class CommandLineWorker {

    private var turn = 0
    private val letterToNumber = arrayOf('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H')

    fun runGame() {
        val input = readLine()!!
        val blackHoleX = convertToPointX(input[1])
        val blackHoleY = convertToPointY(input[0])
        val color = readLine()?.let {
            when (it) {
                "Black", "black" -> 0
                "White", "white" -> 1
                else -> null
            }
        } ?: throw Exception("Invalid input")

        val board = BoardImpl(blackHoleX, blackHoleY)
        val state = BoardImpl.BoardStateImpl(blackHoleX, blackHoleY)
        val bot: IBot = Bot(board, 4)

        if (color == 1) {
            turn++
            //println(turn)
            val input = readLine() ?: throw Exception("Invalid input")
            board.makeTurn(state, convertToPointX(input[1]), convertToPointY(input[0]))
            state.inverseState()
            //state.display()
        }
        var shouldGo = true
        var playerPassed = false
        var botPassed = false
        do {
            try {
                if (state.isEnemyMatrixEmpty || state.isMatrixEmpty) {
                    break
                }
                if (shouldGo) {
                    bot.makeTurn(state)?.let {
                        turn++
                        //println(turn)
                        println(it.toLetter())
                        botPassed = false
                    } ?: run {
                        println("pass")
                        botPassed = true
                    }
                    state.inverseState()
                    //state.display()
                }

                if (state.isEnemyMatrixEmpty || state.isMatrixEmpty) {
                    break
                }
                if (shouldGo) {
                    val input = readLine() ?: throw Exception("Invalid input")
                    if (!input.contains("pass")) {
                        turn++
                        //println(turn)
                        board.makeTurn(state, convertToPointX(input[1]), convertToPointY(input[0]))
                        playerPassed = false
                    } else {
                        playerPassed = true
                    }
                    state.inverseState()
                    //state.display()
                }
            } catch (e: Exception) {
                shouldGo = false
            }
        } while (shouldGo && !(playerPassed && botPassed) && !(turn == 59 && playerPassed))
    }

    fun convertToPointY(char: Char): Byte {
        if (!char.isUpperCase()) throw Exception("Invalid input")
        return (char.toInt() - 65).toByte()
    }

    fun convertToPointX(char: Char): Byte {
        if (!char.isDigit()) throw Exception("Invalid input")
        return (char.toInt() - 48 - 1).toByte()
    }

    fun Board.Point.toLetter() =
        "${letterToNumber[y.toInt()]}${x + 1}"

}

/*
*/