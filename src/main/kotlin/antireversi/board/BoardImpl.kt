package board

import board.Board.BoardState.Companion.EMPTY_POINT
import board.Board.BoardState.Companion.ENEMY_POINT
import board.Board.BoardState.Companion.MY_POINT
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.max

class BoardImpl(private val blackHoleX: Byte, private val blackHoleY: Byte) : Board {

    companion object {

        val dirV: List<Board.Point> = listOf(
            -1 to -1,
            -1 to 0,
            -1 to 1,
            0 to 1,
            1 to 1,
            1 to 0,
            1 to -1,
            0 to -1
        ).map {
            Board.Point(it.first.toByte(), it.second.toByte())
        }
    }

    override fun getAvailableTurns(state: Board.BoardState): List<Board.Point>? {
        val turns = LinkedList<Board.Point>()
        var emptyPoints = 0
        for (i in 0 until 8)
            for (j in 0 until 8) {
                val i = i.toByte()
                val j = j.toByte()
                if (checkPoint(i, j) && state.get(i, j) == Board.BoardState.EMPTY_POINT) {
                    emptyPoints++
                    for (k in 0 until 8)
                        runSearch(state, i, j, k, turns)
                }
            }
        turns.sort()
        var lastElem: Board.Point? = null
        for (i in turns.indices.reversed()) {
            val elem = turns[i]
            if (lastElem != elem) {
                lastElem = elem
            } else {
                turns.removeAt(i)
            }
        }
        return turns.takeIf { emptyPoints > 0 }
    }

    override fun makeTurn(state: Board.BoardState, x: Byte, y: Byte) {
        if (state.get(x, y) != Board.BoardState.EMPTY_POINT) return
        state.takePoint(x, y)
        for (k in 0 until 8)
            runOccupationSearch(state, x.toByte(), y.toByte(), k)
    }

    private fun runOccupationSearch(state: Board.BoardState, x: Byte, y: Byte, dir: Int) {
        var posX = (x + dirV[dir].x).toByte()
        var posY = (y + dirV[dir].y).toByte()
        if (!checkPoint(posX, posY) || state.get(posX.toByte(), posY.toByte()) != Board.BoardState.ENEMY_POINT) return
        do {
            posX = (posX + dirV[dir].x).toByte()
            posY = (posY + dirV[dir].y).toByte()
        } while (checkPoint(posX, posY) &&
            state.get(posX, posY) == Board.BoardState.ENEMY_POINT
        )
        if (checkPoint(posX, posY) && state.get(posX, posY) == Board.BoardState.MY_POINT) {
            occupate(state, x, y, dir)
        }
    }

    private fun occupate(state: Board.BoardState, x: Byte, y: Byte, dir: Int) {
        var posX = (x + dirV[dir].x).toByte()
        var posY = (y + dirV[dir].y).toByte()
        do {
            state.takePoint(posX, posY)
            posX = (posX + dirV[dir].x).toByte()
            posY = (posY + dirV[dir].y).toByte()
        } while (checkPoint(posX, posY) &&
            state.get(posX, posY) == Board.BoardState.ENEMY_POINT
        )
    }

    private fun runSearch(state: Board.BoardState, x: Byte, y: Byte, dir: Int, turns: MutableList<Board.Point>) {
        var posX = (x + dirV[dir].x).toByte()
        var posY = (y + dirV[dir].y).toByte()
        if (!checkPoint(posX, posY) || state.get(posX, posY) != Board.BoardState.ENEMY_POINT) return
        do {
            posX = (posX + dirV[dir].x).toByte()
            posY = (posY + dirV[dir].y).toByte()
        } while (checkPoint(posX, posY) &&
            state.get(posX, posY) == Board.BoardState.ENEMY_POINT
        )
        if (checkPoint(posX, posY) && state.get(posX, posY) == Board.BoardState.MY_POINT) {
            turns.add(Board.Point(x, y))
        }
    }

    private fun checkPoint(x: Byte, y: Byte) =
        x >= 0 && x < 8 && y >= 0 && y < 8 && !(x == blackHoleX && y == blackHoleY)

    data class BoardStateImpl(
        private val blackHoleX: Byte,
        private val blackHoleY: Byte,
        private var matrix: Long = 34628173824,
        private var enemyMatrix: Long = 68853694464
    ) : Board.BoardState {

        var color = 'B'
        var enemyColor = 'W'

        companion object {

            private const val C = 6 //6
            private const val B = 3 //4
            private const val G = 1 //1
            private const val R = 2 //2

            private const val BACK_PENALTIES = 0.3 //0.3

            private val INSIDE_WIN_PER_THRESHOLD = arrayOf(0, 0, 0, 0, 0, 0, 0, 2, 3)

            private val PENALTIES = arrayOf(
                //            0  1  2  3  4  5  6  7
                /*0*/ arrayOf(C, B, B, B, B, B, B, C),
                /*1*/ arrayOf(B, B, R, R, R, R, B, B),
                /*2*/ arrayOf(B, R, B, G, G, B, R, B),
                /*3*/ arrayOf(B, R, G, B, B, G, R, B),
                /*4*/ arrayOf(B, R, G, B, B, G, R, B),
                /*5*/ arrayOf(B, R, B, G, G, B, R, B),
                /*6*/ arrayOf(B, B, R, R, R, R, B, B),
                /*7*/ arrayOf(C, B, B, B, B, B, B, C)
            )
        }

        val isMatrixEmpty: Boolean
            get() = matrix == 0L

        val isEnemyMatrixEmpty: Boolean
            get() = matrix == 0L

        override fun takePoint(x: Byte, y: Byte) {
            val pos = (x.toInt() shl 3).toByte() + y
            matrix = matrix or (1L shl pos)
            enemyMatrix = enemyMatrix and (1L shl pos).inv()
        }

        override fun get(x: Byte, y: Byte): Byte = when {
            get(matrix, x, y) > 0 -> MY_POINT
            get(enemyMatrix, x, y) > 0 -> ENEMY_POINT
            else -> EMPTY_POINT
        }

        override fun getScore(): Board.Point {
            var b = 0.toByte()
            var w = 0.toByte()
            for (i in 0 until 8)
                for (j in 0 until 8) {
                    var score = PENALTIES[i][j]

                    val i = i.toByte()
                    val j = j.toByte()
//                    if (get(matrix, i, j) > 0) {
//                        var bInsideW = 0
//                        for (k in 0 until 8) {
//                            val toX = i + dirV[k].x
//                            val toY = j + dirV[k].y
//                            if (get(enemyMatrix, toX, toY) > 0) bInsideW++
//                        }
//                        score -= (BACK_PENALTIES * INSIDE_WIN_PER_THRESHOLD[bInsideW]).toInt()
//                    }
//
//                    if (get(enemyMatrix, i, j) > 0) {
//                        var wInsideB = 0
//                        for (k in 0 until 8) {
//                            val toX = i + dirV[k].x
//                            val toY = j + dirV[k].y
//                            if (get(matrix, toX, toY) > 0) wInsideB++
//                        }
//                        score += INSIDE_WIN_PER_THRESHOLD[wInsideB]
//                    }

                    if (get(matrix, i, j) > 0) {
                        b = (b + score).toByte()
                        w = (w - (BACK_PENALTIES * score)).toInt().toByte()
                    }
                    if (get(enemyMatrix, i, j) > 0) {
                        w = (w + score).toByte()
                        b = (b - (BACK_PENALTIES * score)).toInt().toByte()
                    }
                }
            return Board.Point(maxOf(b, 0), maxOf(w, 0))
        }

        override fun inverseState() {
            val tm = matrix
            matrix = enemyMatrix
            enemyMatrix = tm

            val tc = color
            color = enemyColor
            enemyColor = tc
        }

        override fun display() {
            println("B: $matrix, W: $enemyMatrix")
            for (i in 0 until 8)
                for (j in 0 until 8) {
                    val i = i.toByte()
                    val j = j.toByte()
                    val ch = when {
                        i == blackHoleX && j == blackHoleY -> 'O'
                        get(matrix, i, j) > 0 -> color
                        get(enemyMatrix, i, j) > 0 -> enemyColor
                        else -> '.'
                    }
                    if (j == 7.toByte()) println(ch) else print(ch)
                }
            println()
        }

        override fun copyState(): Board.BoardState = BoardStateImpl(blackHoleX, blackHoleY, matrix, enemyMatrix)

        override fun isEqual(state: Board.BoardState): Boolean {
            if (state is BoardStateImpl) {
                return (state.matrix == matrix && state.enemyMatrix == enemyMatrix) || (state.matrix == enemyMatrix && state.enemyMatrix == matrix)
            } else return false
        }

        private fun get(matrix: Long, x: Byte, y: Byte): Long =
            ((matrix shr ((x.toInt() shl 3) + y)) and 0b01)
    }
}
///01234567
//0XXXXXXXX
//1XXXXXXXX
//2XXXXXXXX
//3XX.XX.XX
//4XXXXXXXX
//5XXXXXXXX
//6XXXXXXXX
//7XXXXXXXX