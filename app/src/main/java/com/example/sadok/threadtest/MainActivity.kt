package com.example.sadok.threadtest

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var threadINames: List<String>
    var total: Int = 0
    var run: Boolean = true
    private lateinit var condition: Condition
    private var nbrThread: Int = 0
    private var executionStack = mutableListOf<Int>()
    private lateinit var reentrantLock: MyLock
    private var nbIterations = 0
   
    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(inputMessage: Message) {
            Log.d(TAG, "Thread  ${Thread.currentThread().name} TOTAL: ${inputMessage.what}")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        parseFile("input_short.txt")
        reentrantLock = MyLock(stack = executionStack)
        Log.d(TAG, "My execution stack is = $executionStack")
        condition = reentrantLock.newCondition()

        initThreadPool(threadINames)


    }

    inner class MyLock(private val stack: List<Int>, var flag: Int = stack[0]) :
        ReentrantLock() {
        private var index = 0
        fun nextFlag() {
            index++
            try {
                flag = stack[index]
            } catch (e: java.lang.Exception) {
                flag = -1
                run = false
            }
        }
    }

    inner class MyThread(private val index: Int, name: String, private val lock: MyLock) :
        Thread(name) {

        override fun run() {
            try {
                lock.withLock {
                    while (run) {
                        while (lock.flag != index) {
                            condition.await()
                        }
                        val random = Random.nextInt(0, 100)
                        Log.d(TAG, "Thread $name - generated number: $random ")
                        total += random
                        lock.nextFlag()
                        condition.signalAll()
                        nbIterations++
                        if(nbIterations >= executionStack.size)
                            handler.obtainMessage(total)?.apply {
                                sendToTarget()
                            }

                    }

                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception $name :" + e.message)
            }
        }
    }

    /**
     * This is the initialisation of Thread list
     * @param threadNames this is the list of thread names
     */
    private fun initThreadPool(threadNames: List<String>) {
        threadNames.forEachIndexed { index, name ->
            MyThread(index, name, reentrantLock).start()
        }
    }


    @SuppressLint("NewApi")
    fun parseFile(fileName: String) {
        try {
            assets.open(fileName).bufferedReader().use {
                nbrThread = it.readLine().toInt()
                threadINames = getThreadNames(it.readLine())
                it.lines().forEach { line ->
                    executionStack.add(getThreadIndexFromLine(line))
                }
            }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
        }

    }

    private fun getThreadIndexFromLine(line: String) = (line.indexOf("X") - 1) / 2

    private fun getThreadNames(line: String) = line.split('|').filter { it != "" }

    companion object {
        const val TAG = "ThreadTest"
    }

}