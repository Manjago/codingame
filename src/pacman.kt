import java.util.*

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

    // game loop
    while (true) {
        val myScore = input.nextInt()
        val opponentScore = input.nextInt()
        val visiblePacCount = input.nextInt() // all your pacs and enemy pacs in sight

        val myPacmans = mutableListOf<Pacman>()

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

        val move = solver.nextMove(myPacmans, myPellets)
        println(move)
        //println("MOVE 0 15 10") // MOVE <pacId> <x> <y>
    }

}

data class Pellet(val x: Int, val y: Int, val value: Int) {
    fun dist(i: Int, j: Int) = (i - x) * (i - x) + (j - y) * (j - y)
    override fun toString(): String {
        return "(x=$x, y=$y, v=$value)"
    }
}

data class Pacman(val id: Int, val x: Int, val y: Int)

data class Move(val pacId: Int, val x: Int, val y: Int) {
    override fun toString(): String {
        return "MOVE $pacId $x $y"
    }

    companion object {
        fun defaultMove(pacId: Int) = Move(pacId, 0, 0)
    }
}

class Solver {

    private val currentTargets: MutableMap<Int, Pellet> = mutableMapOf()

    fun nextMove(pacmans: List<Pacman>, pellets: Set<Pellet>): String {

        val alivePacmans = pacmans.asSequence().map { it.id }.toSet()
        currentTargets.entries.removeAll { (k, v) ->
            !alivePacmans.contains(k)
        }

        val moves = mutableListOf<Move>()

        pacmans.forEach { pacman ->

            val currentTarget = currentTargets[pacman.id]
            if (currentTarget == null || !pellets.contains(currentTarget)) {

                currentTargets.remove(pacman.id)

                val target: Pellet? = newTarget(pellets, pacman)
                if (target != null) {
                    currentTargets[pacman.id] = target
                    moves.add(Move(pacman.id, target.x, target.y))
                } else {
                    moves.add(Move.defaultMove(pacman.id))
                }
            } else {
                moves.add(Move(pacman.id, currentTarget.x, currentTarget.y))
            }
        }

        return moves.joinToString("|") { it.toString() }
    }

    private fun newTarget(pellets: Set<Pellet>, pacman: Pacman): Pellet? {

        val acquiredTargets = currentTargets.values.toSet()

        val next10 = pellets.asSequence()
            .filter { it.value > 2 }
            .filter { !acquiredTargets.contains(it) }
            .sortedBy { it.dist(pacman.x, pacman.y) }
            .firstOrNull()

        return next10 ?: pellets.asSequence()
            .filter { !acquiredTargets.contains(it) }
            .sortedBy { it.dist(pacman.x, pacman.y) }
            .firstOrNull()

    }

    private fun doMove(pacId: Int, next: Pellet) = "MOVE $pacId ${next.x} ${next.y}"
}