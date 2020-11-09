package board

interface Board {

    fun getAvailableTurns(state: BoardState): List<Point>?

    fun makeTurn(state: BoardState, x: Byte, y: Byte)

    data class Point(val x: Byte, val y: Byte) : Comparable<Point> {

        override fun compareTo(other: Point): Int =
            (x * 10 + y).compareTo(other.x * 10 + other.y)
    }

    interface BoardState {

        companion object {

            const val MY_POINT = 1.toByte()
            const val ENEMY_POINT = 2.toByte()
            const val EMPTY_POINT = 3.toByte()
        }

        fun takePoint(x: Byte, y: Byte)

        fun get(x: Byte, y: Byte): Byte

        fun getScore(): Point

        fun inverseState()

        fun display()

        fun copyState(): BoardState

        fun isEqual(state: BoardState): Boolean
    }
}
/*
*
*01234567
0........
1........
2........
3...WWW..
4...BW...
5........
6........
7........
* */