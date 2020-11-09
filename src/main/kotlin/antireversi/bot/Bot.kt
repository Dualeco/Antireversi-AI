package bot

import board.Board
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class Bot(
    private val board: Board,
    private val recDepth: Byte
) : IBot {

    private var minValue: Int = Int.MAX_VALUE
    private var minValuePosition: Byte = 0

    override fun makeTurn(state: Board.BoardState): Board.Point? {
        minValue = Int.MAX_VALUE
        minValuePosition = 0

        val availableTurns = board.getAvailableTurns(state) ?: return null

        // Get available turns for bot.Bot
        val decisionPoint = when {
            availableTurns.isEmpty() -> {
                null
            }
            availableTurns.size == 1 -> {
                availableTurns[0]
            }
            else -> {
                // Evaluate turns using minimax
                val evaluations = ByteArray(availableTurns.size) {
                    val turn = availableTurns[it]
                    evaluateTurn(
                        turn.x,
                        turn.y,
                        (recDepth - 1).toByte(),
                        state = state.copyState(),
                        min = Byte.MAX_VALUE,
                        max = Byte.MIN_VALUE,
                        turnsPosition = it.toByte()
                    )
                }

                availableTurns[evaluations.indexOfMin()]
            }
        }
        val value = decisionPoint ?: return null
        board.makeTurn(state, value.x, value.y)
        return decisionPoint
    }

    /**
     * Evaluate the player's turn with regards to the given board matrices
     */
    fun evaluateTurn(
        x: Byte,
        y: Byte,
        depth: Byte,
        state: Board.BoardState, // state.copyState()
        min: Byte,
        max: Byte,
        turnsPosition: Byte
    ): Byte {
        board.makeTurn(state, x, y)

        state.inverseState()

        return minimax(
            depth,
            state,
            min,
            max,
            turnsPosition
        )
    }

    /**
     * Use recursive minimax algorithm
     */
    fun minimax(
        depth: Byte,
        state: Board.BoardState,
        min: Byte,
        max: Byte,
        turnsPosition: Byte
    ): Byte {
        val availableTurns: List<Board.Point>? = board.getAvailableTurns(state)
        when {
            availableTurns == null || depth == 0.toByte() -> {
                val score = state.getScore()
                return if ((recDepth - depth) % 2 == 0) score.x.toByte() else score.y.toByte()
            }
            (recDepth - depth) % 2 == 0 -> {
                val score = state.getScore()
                if (score.y - score.x < minValue) {
                    minValue = score.y - score.x
                    minValuePosition = turnsPosition
                }

                var maxEval = Byte.MIN_VALUE
                for (i in availableTurns.indices) {
                    val turn = availableTurns[i]
                    val evaluation = evaluateTurn(
                        turn.x.toByte(),
                        turn.y.toByte(),
                        (depth - 1).toByte(),
                        state = state.copyState(),
                        min = min,
                        max = max,
                        turnsPosition = turnsPosition
                    )
                    maxEval = maxOf(maxEval, evaluation)
                    val newMax = maxOf(max, maxEval)
                    if (min <= newMax) {
                        break
                    }
                }
                return maxEval
            }
            (recDepth - depth) % 2 == 1 -> {
                val score = state.getScore()
                if (score.x - score.y < minValue) {
                    minValue = score.x - score.y
                    minValuePosition = turnsPosition
                }

                var minEval = Byte.MAX_VALUE
                for (i in availableTurns.indices) {
                    val turn = availableTurns[i]
                    val evaluation = evaluateTurn(
                        turn.x.toByte(),
                        turn.y.toByte(),
                        (depth - 1).toByte(),
                        state = state.copyState(),
                        min = min,
                        max = max,
                        turnsPosition = turnsPosition
                    )
                    minEval = minOf(minEval, evaluation)
                    val newMin = minOf(min, minEval)
                    if (max >= newMin) {
                        break
                    }
                }
                return minEval
            }
            else -> throw MinimaxNotMetException()
        }
    }

    /**
     * Find the index of the minimal element of the array
     */
    private fun ByteArray.indexOfMin(): Int {
        var minI = if (size == 0) {
            -1
        } else {
            0
        }
        var min = Byte.MAX_VALUE

        forEachIndexed { i, elem ->
            if (elem < min) {
                min = elem
                minI = i
            }
        }

        return minI
    }

    class MinimaxNotMetException(
        message: String = "Minimax didn't meet any of the requirements to continue the computation"
    ) : RuntimeException(message)

    class GameOverException(
        message: String = "Game over"
    ) : RuntimeException(message)

    private fun runLongJob(
        job: () -> Result<Board.Point>,
        onTimeLimit: () -> Result<Board.Point>
    ): Result<Board.Point> {
        val executor = Executors.newSingleThreadExecutor()
        val future: Future<Result<Board.Point>> = executor.submit<Result<Board.Point>>(job)

        return try {
            future.get(1990, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            onTimeLimit()
        }.also {
            executor.shutdownNow()
        }
    }

    data class Result<T>(val value: T? = null)
}

// 0B 2 3
// 1W 3 2
// 2B 2 3
// 3W 3 2