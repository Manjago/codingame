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

        var myPacId: Int = 0
        var myX: Int = 0
        var myY: Int = 0

        for (i in 0 until visiblePacCount) {
            val pacId = input.nextInt() // pac number (unique within a team)
            val mine = input.nextInt() != 0 // true if this pac is yours
            val x = input.nextInt() // position in the grid
            val y = input.nextInt() // position in the grid
            val typeId = input.next() // unused in wood leagues
            val speedTurnsLeft = input.nextInt() // unused in wood leagues
            val abilityCooldown = input.nextInt() // unused in wood leagues
            if (mine) {
                myPacId = pacId
                myX = x
                myY = y
            }
        }
        val visiblePelletCount = input.nextInt() // all pellets in sight
        val myPellets = mutableListOf<Pellet>()
        for (i in 0 until visiblePelletCount) {
            val x = input.nextInt()
            val y = input.nextInt()
            val value = input.nextInt() // amount of points this pellet is worth
            myPellets.add(Pellet(x, y, value))
        }

        // Write an action using println()
        // To debug: System.err.println("Debug messages...");

        val move = solver.nextMove(myPacId, myX, myX, myPellets)
        println(move)
        //println("MOVE 0 15 10") // MOVE <pacId> <x> <y>
    }

}

data class Pellet(
    val x: Int,
    val y: Int,
    val value: Int
) {
    fun dist(i: Int, j: Int) = (i - x) * (i - x) + (j - y) * (j - y)
}

class Solver {

    var current: Pellet? = null

    fun nextMove(pacId: Int, x: Int, y: Int, pellets: List<Pellet>): String {

        if (current == null) {
            return newTarget(pellets, x, y, pacId)
        } else {
            val nexttarget = pellets.asSequence().firstOrNull { it == current }
            if (nexttarget != null) {
                System.err.println("to saved $nexttarget")
                return doMove(pacId, nexttarget)
            } else {
                return newTarget(pellets, x, y, pacId)
            }
        }
    }

    private fun newTarget(
        pellets: List<Pellet>,
        x: Int,
        y: Int,
        pacId: Int
    ): String {
        val next = pellets.asSequence().sortedBy { it.dist(x, y) }.firstOrNull()

        if (next == null) {
            System.err.println("0 0")
            return "MOVE $pacId 0 0"
        } else {
            current = next
            System.err.println("to new $next")
            return doMove(pacId, next)
        }

    }

    private fun doMove(pacId: Int, next: Pellet) = "MOVE $pacId ${next.x} ${next.y}"
}