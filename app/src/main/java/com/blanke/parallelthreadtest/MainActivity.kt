package com.blanke.parallelthreadtest

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private lateinit var consoleView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        consoleView = tv_console
        bu_countdownlatch.setOnClickListener { doCountDownLatch() }
        bu_rxjava.setOnClickListener { doRxJava() }
        bu_rxjava2.setOnClickListener { doRxJava2() }
        bu_rxjava3.setOnClickListener { doRxJava3() }
    }

    private fun getSourceData(): List<Int> {
        return (0..30).map { it }
    }

    private fun doCountDownLatch() {
        clearConsole()
        val array = getSourceData()
        val mCountDownLatch = CountDownLatch(array.size)
        val result = Collections.synchronizedList(mutableListOf<String>())
        // 避免 android 主线程阻塞
        thread {
            array.forEach {
                thread {
                    Thread.sleep(Random().nextInt(5000).toLong())
                    val res = it.toString()
                    result.add(res)
                    log(res)
                    mCountDownLatch.countDown()
                }
            }
            mCountDownLatch.await()
            log("\ndoCountDownLatch=${result.joinToString(",")}")
        }
    }

    private fun doRxJava() {
        clearConsole()
        val array = getSourceData()
        Flowable.fromIterable(array)
                .flatMap {
                    Flowable.just(it)
                            .subscribeOn(Schedulers.newThread())
                            .map {
                                Thread.sleep(Random().nextInt(5000).toLong())
                                val res = it.toString()
                                log(res)
                                res
                            }
                }
                .toList()
                .subscribe(Consumer<List<String>> {
                    log("\ndoRxJava=${it.joinToString(",")}")
                })
    }

    private fun doRxJava2() {
        clearConsole()
        val array = getSourceData()
        val observables = array.map {
            Observable.just(it).subscribeOn(Schedulers.newThread())
                    .map {
                        Thread.sleep(Random().nextInt(5000).toLong())
                        val res = it.toString()
                        log(res)
                        res
                    }
        }
        Observable.zip(observables, { it })
                .subscribe {
                    log("\ndoRxJava2=${it.joinToString(",")}")
                }
    }

    private fun doRxJava3() {
        clearConsole()
        val array = getSourceData()
        val observables = array.map {
            Observable.just(it).subscribeOn(Schedulers.newThread())
                    .map {
                        Thread.sleep(Random().nextInt(5000).toLong())
                        val res = it.toString()
                        log(res)
                        res
                    }
        }
        Observable.merge(observables)
                .toList()
                .subscribe(Consumer<List<String>> {
                    log("\ndoRxJava3=${it.joinToString(",")}")
                })
    }

    private fun clearConsole() {
        consoleView?.post {
            consoleView.text = ""
        }
    }

    private fun log(s: String) {
        Log.d("parallel", s)
        consoleView?.post {
            consoleView.append("$s ")
        }
    }
}
