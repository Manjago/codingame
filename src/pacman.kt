import java.util.*
import kotlin.math.abs

/**
 * Grab the pellets as fast as you can!
 **/
fun main(args: Array<String>) {
    val solver = Solver()
    val input = Scanner(System.`in`)
    val width = input.nextInt() // size of the grid
    val height = input.nextInt() // top left corner is (x=0, y=0)
    if (input.hasNextLine()) {
        input.nextLine()
    }
    for (i in 0 until height) {
        val row = input.nextLine() // one line of the grid: space " " is floor, pound "#" is wall
    }

    var turnNum = 0
    // game loop
    while (true) {
        val myScore = input.nextInt()
        val opponentScore = input.nextInt()
        val visiblePacCount = input.nextInt() // all your pacs and enemy pacs in sight

        val myPacmans = mutableListOf<Pacman>()
        val hisPacmans = mutableListOf<Pacman>()

        for (i in 0 until visiblePacCount) {
            val pacId = input.nextInt() // pac number (unique within a team)
            val mine = input.nextInt() != 0 // true if this pac is yours
            val x = input.nextInt() // position in the grid
            val y = input.nextInt() // position in the grid
            val typeId = input.next() // unused in wood leagues
            val speedTurnsLeft = input.nextInt() // unused in wood leagues
            val abilityCooldown = input.nextInt() // unused in wood leagues
            if (mine) {
                myPacmans.add(Pacman(pacId, x, y))
            } else {
                hisPacmans.add(Pacman(pacId, x, y))
            }
        }
        val visiblePelletCount = input.nextInt() // all pellets in sight
        val myPellets = mutableSetOf<Pellet>()
        for (i in 0 until visiblePelletCount) {
            val x = input.nextInt()
            val y = input.nextInt()
            val value = input.nextInt() // amount of points this pellet is worth
            myPellets.add(Pellet(x, y, value))
        }

        // Write an action using println()
        // To debug: System.err.println("Debug messages...");

        val move = solver.nextMove(myPacmans, myPellets, hisPacmans, turnNum++)
        println(move)
        //println("MOVE 0 15 10") // MOVE <pacId> <x> <y>
    }

}

abstract class Item(open val x: Int, open val y: Int) {
    fun dist(other: Item) = abs(other.x - x) + abs(other.y - y)
}

data class Pellet(override val x: Int, override val y: Int, val value: Int) : Item(x, y) {
    override fun toString(): String {
        return "(x=$x, y=$y, v=$value)"
    }
}

data class Pacman(val id: Int, override val x: Int, override val y: Int) : Item(x, y)

data class Move(val pacman: Pacman, val item: Item) {
    override fun toString(): String {
        return "MOVE ${pacman.id} ${item.x} ${item.y}"
    }
}

interface Strategy {
    fun nextMove(pacman: Pacman): Move?
    fun isDummy(): Boolean = false
    fun name(): String
    fun commit(move: Move): Move = move
}


class DummyStrategy : Strategy {
    override fun name() = "dummy"
    override fun isDummy() = true
    override fun nextMove(pacman: Pacman): Move? {
        return Move(pacman, pacman)
    }
}

class Solver {

    private val currentTargets: MutableMap<Int, Item> = mutableMapOf()
    private val currentStrategies: MutableMap<Int, Strategy> = mutableMapOf()
    private lateinit var pellets: Set<Pellet>
    private lateinit var prevMyPacmans: List<Pacman>
    private lateinit var myPacmans: List<Pacman>
    private lateinit var hisPacmans: List<Pacman>
    private lateinit var moves: MutableList<Move>
    private lateinit var prevMoves: MutableList<Move>


    inner class HarvesterStrategy() : Strategy {
        override fun name() = "harv"

        override fun nextMove(pacman: Pacman): Move? {

            val currentTarget = currentTargets[pacman.id]
            return if (currentTarget == null || !pellets.contains(currentTarget)) {

                currentTargets.remove(pacman.id)

                val target: Pellet? = newTarget(pacman)
                if (target != null) {
                    currentTargets[pacman.id] = target
                    Move(pacman, target)
                } else {
                    null
                }
            } else {
                Move(pacman, currentTarget)
            }
        }

        override fun commit(move: Move): Move {
            currentTargets.remove(move.pacman.id)
            currentTargets[move.pacman.id] = move.pacman
            return move
        }

        private fun newTarget(pacman: Pacman): Pellet? {

            val acquiredTargets = currentTargets.values.toSet()

            val next10 = pellets.asSequence()
                .filter { it.value > 2 }
                .filter { !acquiredTargets.contains(it) }
                .sortedBy { it.dist(pacman) }
                .firstOrNull()

            return next10 ?: pellets.asSequence()
                .filter { !acquiredTargets.contains(it) }
                .sortedBy { it.dist(pacman) }
                .firstOrNull()
        }
    }

    inner class IdleStrategy() : Strategy {

        override fun name() = "idle"

        override fun isDummy() = true

        override fun nextMove(pacman: Pacman): Move? {
            val enemy = hisPacmans.asSequence()
                .sortedBy { it.dist(pacman) }
                .firstOrNull()

            if (enemy != null) {
                return Move(pacman, enemy)
            } else {
                return null
            }
        }

    }

    fun nextMove(
        pacmans: List<Pacman>, pellets: Set<Pellet>, hisPacmans: List<Pacman>,
        turnNum: Int
    ): String {

        this.pellets = pellets
        this.prevMyPacmans = if (turnNum != 0) this.myPacmans else listOf()
        this.myPacmans = pacmans
        this.hisPacmans = hisPacmans
        this.prevMoves = if (turnNum != 0) this.moves else mutableListOf()

        removeDeads()

        moves = mutableListOf()

        pacmans.forEach { pacman ->

            val prestrategy = currentStrategies[pacman.id]
            if (prestrategy != null && prestrategy.isDummy()) {
                currentStrategies.remove(pacman.id)
            }
            val strategy = currentStrategies[pacman.id]

            val move = strategy?.nextMove(pacman)
            if (move != null) {
                log(strategy, move)
                strategy.commit(move)
                moves.add(move)
            } else {
                val (actualStrategy, newmove) = newStrategyMove(pacman)
                if (!actualStrategy.isDummy()) {
                    currentStrategies[pacman.id] = actualStrategy
                }
                actualStrategy.commit(newmove)
                log(actualStrategy, newmove)
                moves.add(newmove)
            }
        }

        return moves.joinToString(" | ") { it.toString() }
    }

    private fun Pacman.isBlocked(): Boolean {
        val prev = prevMyPacmans.asSequence().firstOrNull { it.id == this.id }
        if (prev == null) {
            return false
        }

        return prev.x == this.x && prev.y == this.y
    }

    private fun log(strategy: Strategy, move: Move) {
        System.err.println("$move ${strategy.name()}")
    }

    private fun newStrategyMove(pacman: Pacman): Pair<Strategy, Move> {
        val pretender1 = HarvesterStrategy()
        val move1 = pretender1.nextMove(pacman)
        if (move1 != null) {
            return pretender1 to move1
        }

        val pretender2 = IdleStrategy()
        val move2 = pretender2.nextMove(pacman)
        if (move2 != null) {
            return pretender2 to move2
        }

        val pretender3 = DummyStrategy()
        return pretender3 to pretender3.nextMove(pacman)!!
    }

    private fun removeDeads() {
        val alivePacmans = myPacmans.asSequence().map { it.id }.toSet()
        currentTargets.entries.removeAll { (k, v) ->
            !alivePacmans.contains(k)
        }
        currentStrategies.entries.removeAll { (k, v) ->
            !alivePacmans.contains(k)
        }
    }

}