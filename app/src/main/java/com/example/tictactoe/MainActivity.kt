package com.example.tictactoe

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.window.layout.WindowMetricsCalculator
import com.example.tictactoe.databinding.ActivityMainBinding
import kotlin.math.*
import kotlin.Double.Companion.NEGATIVE_INFINITY as ninf
import kotlin.Double.Companion.POSITIVE_INFINITY as inf


//Added to support different screens
enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED }

class InvalidPlayerSymbolException: Exception("Player symbol must be either X or O")
class InvalidComputerSymbolException: Exception("Computer symbol must be either X or O")

const val NAVIGATOR_EDGE_LENGTH:Int = 3

fun List<String>.coo1d(coo:Coo):Int{
    val N = sqrt(this.size.toFloat()).toInt()
    return coo.y*N+coo.x
}
fun List<String>.nextCoo(coo:Coo):Coo{
    val N = sqrt(this.size.toFloat()).toInt()
    var tmpCoo:Coo = coo
    tmpCoo.x = if (tmpCoo.x < 0) tmpCoo.x+N else if (tmpCoo.x > N-1) tmpCoo.x-N else tmpCoo.x
    tmpCoo.y = if (tmpCoo.y < 0) tmpCoo.y+N else if (tmpCoo.y > N-1) tmpCoo.y-N else tmpCoo.y
    return tmpCoo
}
fun List<String>.coo2d(c1d:Int):Coo{
    val N = sqrt(this.size.toFloat()).toInt()
    return Coo(c1d%N,c1d/N)
}

data class Coo(var x:Int, var y:Int){
    operator fun plus(other:Coo):Coo{
        return Coo(x+other.x, y+other.y)
    }
}

class MainActivity : AppCompatActivity() {
    private val winSizes:Pair<Int,Int> by lazy { computeWindowSizes() }
    private val fieldEdgeLength = 4
    private val playerSymbol:String = "X"
    private val computerSymbol:String by lazy {
        when (playerSymbol){
            "X"->"O"
            "O"->"X"
            else->{
                throw InvalidPlayerSymbolException()
            }
        }
    }
    private val fieldSize = fieldEdgeLength*fieldEdgeLength
    private var binding: ActivityMainBinding? = null
    private var fieldLabels:MutableList<String> = MutableList<String>(fieldSize){"-"}
    private val navigatorLabels: List<String> = listOf(
        "NW", "^", "NE",
        "<", playerSymbol, ">",
        "SW", "V", "SE",
    )
    private val playableTiles: MutableList<Coo> = MutableList<Coo>(fieldSize){
        it -> fieldLabels.coo2d(it)
    }
    private var currCoo:Coo = Coo(0,0)

    private fun Int.toPixel():Int{
        return (this*resources.displayMetrics.density).roundToInt()
    }
    private fun Int.toDP():Int{
        return (this/resources.displayMetrics.density).roundToInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playableTiles.shuffle()
        if (binding == null) {
            binding = ActivityMainBinding.inflate(this.layoutInflater)
        }
        setContentView(binding!!.root)

        setMargins(binding!!.fieldView,50)
        setMargins(binding!!.navigatorView,120)

        setupGrid(fieldLabels, fieldEdgeLength, binding!!.fieldView)
        setupGrid(navigatorLabels, NAVIGATOR_EDGE_LENGTH, binding!!.navigatorView, true)

        colorNextTile(currCoo)

        if (playerSymbol == "O"){
            computerPlays()
        }

        binding!!.navigatorView.onItemClickListener = AdapterView.OnItemClickListener {
            adapterView, view, i, l ->
            val moveCoo: Coo = normalizeNavigatorCoo(navigatorLabels.coo2d(i))
            val newCoo: Coo = fieldLabels.nextCoo(currCoo + moveCoo)
            if (moveCoo == Coo(0, 0)) {
                if (currCoo in playableTiles){
                    writeTile(currCoo, playerSymbol)
                    computerPlays()
                }
                else{
                    Toast.makeText(this@MainActivity,"You shall not pass!", Toast.LENGTH_SHORT).show()
                }

            } else {
                colorNextTile(newCoo)
            }
        }
    }
    private fun setMargins(
        view: View,
        smallerMarginDp:Int=10
    ){
        val weight = (((view.parent as View).layoutParams) as LinearLayout.LayoutParams).weight
        view.updateLayoutParams<ConstraintLayout.LayoutParams> {
            val w:Float = winSizes.first.toFloat()
            val h:Float = (weight*winSizes.second/5)
            val largerMarginDp:Int = (abs(w-h)/2).roundToInt()+smallerMarginDp
            Log.d("ActivityMain","$w, $h, $largerMarginDp, $smallerMarginDp")

            var marginH:Int = largerMarginDp.toPixel()
            var marginV:Int = smallerMarginDp.toPixel()

            if (w < h){
                marginH = smallerMarginDp.toPixel()
                marginV = largerMarginDp.toPixel()
            }
            this.setMargins(marginH, marginV, marginH, marginV)
        }
    }
    private fun computeWindowSizes():Pair<Int,Int> {
        val metrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)

        val widthDp:Int = metrics.bounds.width().toDP()
        val heightDp:Int = metrics.bounds.height().toDP()

        return Pair<Int,Int>(widthDp,heightDp)
    }
    private fun computerPlays(){
        (binding!!.navigatorView.adapter as TictactoeGridAdapter).makeAllUnclickable()
        if (wins(playerSymbol)){
            Toast.makeText(this@MainActivity, "Player wins",Toast.LENGTH_SHORT).show()
        }
        else if (playableTiles.isEmpty()){
            Toast.makeText(this@MainActivity, "Draw",Toast.LENGTH_SHORT).show()
        }
        else {
            //COM ALGO
            Log.d("ActivityMain: ", "minimax start")
            //val coo = playableTiles.random()
            val coo:Coo = minimax().first!!
            Log.d("ActivityMain: ", "minimax end")
            writeTile(coo,computerSymbol)
            //////////
            if (wins(computerSymbol)){
                Toast.makeText(this@MainActivity, "Computer wins",Toast.LENGTH_SHORT).show()
            }
            else if (playableTiles.isEmpty()){
                Toast.makeText(this@MainActivity, "Draw",Toast.LENGTH_SHORT).show()
            }
            else {
                (binding!!.navigatorView.adapter as TictactoeGridAdapter).makeAllClickable()
            }
        }
    }
    private fun wins(symbol:String):Boolean{
        return isGameOverH(symbol) || isGameOverV(symbol) || isGameOverD(symbol)
    }
    private fun isGameOverH(symbol:String):Boolean{
        //Check horizontally
        var gameOver:Boolean = false
        for (i:Int in 0..fieldEdgeLength-1){
            var gameOverH:Boolean = true
            for (j:Int in 0..fieldEdgeLength-1){
                val index:Int = fieldLabels.coo1d(Coo(j,i))
                if (!(fieldLabels[index] == symbol)){
                    gameOverH = false
                    break
                }
            }
            if (gameOverH) {
                gameOver = true
                break
            }
        }
        return gameOver
    }
    private fun isGameOverV(symbol:String):Boolean{
        //Check horizontally
        var gameOver:Boolean = false
        for (i:Int in 0..fieldEdgeLength-1){
            var gameOverV:Boolean = true
            for (j:Int in 0..fieldEdgeLength-1){
                val index:Int = fieldLabels.coo1d(Coo(i,j))
                if (!(fieldLabels[index] == symbol)){
                    gameOverV = false
                    break
                }
            }
            if (gameOverV) {
                gameOver = true
                break
            }
        }
        return gameOver
    }
    private fun isGameOverD(symbol:String):Boolean{
        //Check diagonally
        var gameOver:Boolean = false
        for (i:Int in listOf(0, fieldEdgeLength)){
            var gameOverTmp:Boolean = true
            for (j:Int in 0..fieldEdgeLength-1){
                val invj:Float = (1-(1+2*j.toFloat())/(fieldEdgeLength.toFloat()))*i.toFloat()+j.toFloat()
                val index:Int = fieldLabels.coo1d(Coo(invj.roundToInt(),j))
                gameOverTmp = gameOverTmp && (fieldLabels[index] == symbol)
            }
            gameOver = gameOver || gameOverTmp
            if (gameOver) {
                break
            }
        }
        return gameOver
    }
    private fun normalizeNavigatorCoo(coo:Coo) :Coo{
        return coo + Coo(-1, -1)
    }
    private fun tileAt(coo: Coo):TextView{
        return binding!!.fieldView.adapter.getView(fieldLabels.coo1d(coo),null,null).findViewById<TextView>(R.id.textView)
    }
    private fun writeTile(coo: Coo, symbol:String){
        if (coo in playableTiles){
            tileAt(coo).text = symbol
            fieldLabels[fieldLabels.coo1d(coo)] = symbol
            playableTiles.remove(coo)
        }
        else {
            throw Exception("The tile $coo is not playable")
        }
    }
    private fun colorNextTile(loc:Coo){
        tileAt(currCoo).setBackgroundResource(R.drawable.border)
        currCoo = loc
        tileAt(currCoo).setBackgroundColor(Color.argb(100,0,0,255))
    }
    private fun setupGrid(labels: List<String>, size: Int, gridView: GridView, clickable: Boolean=false){
        val adapter: TictactoeGridAdapter = TictactoeGridAdapter(this@MainActivity, labels,clickable)
        gridView.numColumns = size
        gridView.adapter = adapter
    }
    //MINIMAX WITH ALPHA BETA PRUNING, COMPUTER IS TRYING TO MAXIMIZE
    private fun eval(depth:Int):Int{
        //Note that max depth is fieldSize
        if (wins(computerSymbol)){
            return fieldSize-depth+1
        }
        if (wins(playerSymbol)){
            return -100
        }
        return 0
    }
    //Minimax alone lasted more than 24 minutes, couldn't wait
    private fun minimax(
        symbol:String = computerSymbol,
        alpha:Int=ninf.toInt(), beta:Int=inf.toInt(),
        depth:Int=0,
        ):Pair<Coo?, Int>{
        if (playableTiles.isEmpty()){
            val pt = eval(depth)
            return Pair<Coo?,Int>(null, pt)
        }
        var bestMove:Coo? = null
        var pt4bestMove = if (symbol == computerSymbol) ninf.toInt() else if(symbol == playerSymbol) inf.toInt() else throw InvalidComputerSymbolException()
        val otherSymbol = if (symbol == "X") "O" else if(symbol == "O") "X" else throw InvalidComputerSymbolException()
        val playableTiles_:List<Coo> = playableTiles.toList()
        var alpha_:Int = alpha
        var beta_:Int = beta
        for (coo:Coo in playableTiles_){
            val index:Int = fieldLabels.coo1d(coo)
            playableTiles.remove(coo)
            fieldLabels[index] = symbol

            val pair:Pair<Coo?, Int> = minimax(otherSymbol,alpha_,beta_,depth+1)
            if (symbol == computerSymbol){
                if (pair.second > pt4bestMove){
                    bestMove = coo
                    pt4bestMove = pair.second
                }
                alpha_ = max(alpha_, pair.second)
                if (beta_ <= alpha_){
                    fieldLabels[index] = "-"
                    playableTiles.add(coo)
                    break
                }
            }
            else if (symbol == playerSymbol){
                if (pair.second < pt4bestMove){
                    bestMove = coo
                    pt4bestMove = pair.second
                }
                beta_ = min(beta_, pair.second)
                if (beta_ <= alpha_){
                    fieldLabels[index] = "-"
                    playableTiles.add(coo)
                    break
                }
            }
            else {
                throw InvalidComputerSymbolException()
            }

            fieldLabels[index] = "-"
            playableTiles.add(coo)
        }
        return Pair<Coo,Int>(bestMove!!,pt4bestMove)
    }
}