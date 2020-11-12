package bot

import board.Board
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.experimental.or
import kotlin.math.min

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
                var minEvaluation = Short.MAX_VALUE
                var minIndex = -1
                // Evaluate turns using minimax
                val evaluations = ShortArray(availableTurns.size) {
                    val turn = availableTurns[it]
                    val evaluation = evaluateTurn(
                        turn.x,
                        turn.y,
                        (recDepth - 1).toByte(),
                        state = state.copyState(),
                        min = minEvaluation,
                        max = Short.MIN_VALUE,
                        minEvaluation
                    )
                    if (evaluation < minEvaluation) {
                        minEvaluation = evaluation
                        minIndex = it
                    }
                    evaluation
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
        min: Short,
        max: Short,
        minEvaluation: Short
    ): Short {
        board.makeTurn(state, x, y)

        state.inverseState()

        return minimax(
            depth,
            state,
            min,
            max,
            minEvaluation
        )
    }

    /**
     * Use recursive minimax algorithm
     */
    fun minimax(
        depth: Byte,
        state: Board.BoardState,
        min: Short,
        max: Short,
        minEvaluation: Short
    ): Short {
        val availableTurns: List<Board.Point>? = board.getAvailableTurns(state)
        when {
            availableTurns == null || depth == 0.toByte() -> {
                val score = state.getScore(weighed = true)
                return if ((recDepth - depth) % 2 == 0) score.x else score.y
            }
            availableTurns.isEmpty() -> {
                val score = state.getScore(weighed = true)
                return if ((recDepth - depth) % 2 == 1) score.x else score.y
            }
            (recDepth - depth) % 2 == 0 -> {
                var maxEval = Short.MIN_VALUE
                for (i in availableTurns.indices) {
                    val turn = availableTurns[i]
                    val evaluation = evaluateTurn(
                        turn.x,
                        turn.y,
                        (depth - 1).toByte(),
                        state = state.copyState(),
                        min = min,
                        max = max,
                        minEvaluation = minEvaluation
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
                var minEval = Short.MAX_VALUE
                for (i in availableTurns.indices) {
                    val turn = availableTurns[i]
                    val evaluation = evaluateTurn(
                        turn.x,
                        turn.y,
                        (depth - 1).toByte(),
                        state = state.copyState(),
                        min = min,
                        max = max,
                        minEvaluation = minEvaluation
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
    private fun ShortArray.indexOfMin(): Int {
        var minI = if (size == 0) {
            -1
        } else {
            0
        }
        var min = Short.MAX_VALUE

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