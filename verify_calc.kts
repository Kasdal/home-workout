
data class Exercise(val id: Int, val name: String, val weight: Float, val sets: Int = 4, val reps: Int = 13)

fun main() {
    val exercise = Exercise(1, "Bench Press", 20f)
    val completedSets = setOf(0, 1, 2, 3) // User "ticked off first exercise" (all 4 sets)
    
    val totalWeight = completedSets.size * exercise.reps * exercise.weight
    
    println("Sets: ${completedSets.size}")
    println("Reps: ${exercise.reps}")
    println("Weight: ${exercise.weight}")
    println("Total: $totalWeight")
    
    val oneSet = setOf(0)
    val totalOneSet = oneSet.size * exercise.reps * exercise.weight
    println("Total for 1 set: $totalOneSet")
}
