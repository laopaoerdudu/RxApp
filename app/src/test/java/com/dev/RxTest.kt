package com.dev

import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import io.reactivex.observables.ConnectableObservable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.reactivestreams.Subscriber
import java.util.HashMap
import java.util.concurrent.TimeUnit

class RxTest {

    private lateinit var mTestScheduler: TestScheduler
    private lateinit var mList: MutableList<Any>

    @Before
    fun setUp() {
        mTestScheduler = TestScheduler()
        mList = mutableListOf()
    }

    @Test
    fun test_zip() {
        val observable1 =
            Observable.interval(5, TimeUnit.SECONDS, mTestScheduler)
                .skip(1)
                .take(4)
                .doOnNext {
                    //println("observable1 doOnNext -> $it")
                }

        val observable2 =
            Observable.interval(8, TimeUnit.SECONDS, mTestScheduler)
                .take(4)
                .doOnNext {
                    // println("observable2 -> $it")
                }
                .map { aLong -> ('A' + aLong.toInt()) }
                .doOnNext {
                    //println("map -> doOnNext -> $it")
                }

        Observable.zip(observable1, observable2, BiFunction<Long, Char, String> { t1, t2 ->
            //println("t1 -> $t1")
            //println("t2 -> $t2")
            t1.toString().plus(t2.toString())
        }).subscribe { str ->
            mList.add(str)
        }
        mTestScheduler.advanceTimeBy(50, TimeUnit.SECONDS)
        Assert.assertEquals(listOf("1A", "2B", "3C", "4D"), mList)
    }

    @Test
    fun test_merge() {
        val observable1 =
            Observable.interval(5, TimeUnit.SECONDS, mTestScheduler)
                .take(5)
                .map { aLong -> (aLong + 1) * 20 }
                .doOnNext(System.out::println)

        val observable2 =
            Observable.interval(18, TimeUnit.SECONDS, mTestScheduler)
                .take(2)
                .map { 1L }
                .doOnNext(System.out::println)

        Observable.merge(observable1, observable2).subscribe { mList.add(it) }
        mTestScheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
        Assert.assertEquals(
            listOf(20L, 40L, 60L, 1L, 80L, 100L, 1L),
            mList
        )
    }

    @Test
    fun test_publish() {
        val publish: ConnectableObservable<Int> = Observable.just(1, 2, 3)
            .publish()

        //此时并不会马上订阅数据
        publish.subscribe {
            mList.add(it)
        }
        Assert.assertTrue(mList.isEmpty())

        //开始订阅数据
        publish.connect()
        Assert.assertEquals(listOf(1, 2, 3), mList)
    }

    @Test
    fun test_toSortedList() {
        Observable.just(2, 5, 1, 6, 3, 4)
            .toSortedList()
            .subscribeBy {
                mList.addAll(it)
            }
        Assert.assertEquals(listOf(1, 2, 3, 4, 5, 6), mList)

        //以下代码进行倒序
        mList.clear()
        Observable.just(2, 5, 1, 6, 3, 4)
            .toSortedList(Comparator() { t1, t2 ->
                t2 - t1
            })
            .subscribeBy {
                mList.addAll(it)
            }
        Assert.assertEquals(listOf(6, 5, 4, 3, 2, 1), mList)
    }

    @Test
    fun test_toMap() {
        val map: MutableMap<String?, String?> = HashMap()
        Observable.just("one", "two", "three")
            .toMap { s -> "key$s" }
            .subscribe { m ->
                map.putAll(m)
            }
        println(map)
    }

    @Test
    fun test_timer() {
        //仅发射0的数值，延迟100s
        Observable.timer(100, TimeUnit.SECONDS, mTestScheduler)
            .subscribe { long ->
                mList.add(long)
            }
        Assert.assertTrue(mList.isEmpty())
        mTestScheduler.advanceTimeBy(100, TimeUnit.SECONDS)
        Assert.assertEquals(listOf(0L), mList)
    }

    @Test
    fun test_repeatWhen() {
        Observable.just(1, 2)
            .repeatWhen { completed ->
                completed.delay(
                    5,
                    TimeUnit.SECONDS,
                    mTestScheduler
                )
            }
            .subscribe {
                mList.add(it)
            }
        mTestScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        Assert.assertEquals(listOf(1, 2), mList)

        mTestScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        Assert.assertEquals(listOf(1, 2, 1, 2), mList)
    }

    @Test
    fun test_repeat() {
        Observable.just(1, 2)
            .repeat(3)
            .subscribe {
                mList.add(it)
            }
        Assert.assertEquals(listOf(1, 2, 1, 2, 1, 2), mList)
    }

    @Test
    fun test_range() {
        Observable.range(2, 6)
            .subscribe {
                mList.add(it)
            }
        Assert.assertEquals(listOf(2, 3, 4, 5, 6, 7), mList)
    }

    @Test
    fun test_interval() {
        Observable.interval(100, TimeUnit.MILLISECONDS, mTestScheduler)
            .subscribe {
                mList.add(it)
            }

        //时间提早400ms前
        mTestScheduler.advanceTimeBy(400, TimeUnit.MILLISECONDS)
        Assert.assertEquals(
            mList,
            listOf(0L, 1L, 2L, 3L)
        )

        //时间提早(400 + 200)ms前
        mTestScheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS)
        Assert.assertEquals(
            mList,
            listOf(0L, 1L, 2L, 3L, 4L, 5L)
        )
    }

    @Test
    fun test_error() {
        Observable.error<java.lang.Exception>(NullPointerException())
            .subscribeBy(
                onNext = { value ->
                    mList.add(value)
                },
                onError = {
                    mList.add("error")
                },
                onComplete = {
                    mList.add("completed")
                }
            )
        println(mList)
    }

    @Test
    fun test_empty() {
        Observable.empty<String>()
            .doOnNext { value -> mList.add(value) }
            .doOnComplete { mList.add("completed") }
            .subscribe {
                mList.add(it)
            }
        Assert.assertEquals(mList, listOf("completed"))
    }

    @Test
    fun test_never() {
        Observable.never<String>()
            .doOnNext { value -> mList.add(value) }
            .doOnComplete { mList.add("completed") }
            .subscribe {
                mList.add(it)
            }
        Assert.assertTrue(mList.isEmpty())
    }

    @Test
    fun test_create() {
        Observable.create(ObservableOnSubscribe<Int> { emitter ->
            emitter.onNext(1)
            emitter.onNext(3)
            emitter.onNext(5)
            emitter.onError(ClassCastException())
            emitter.onNext(7)
            emitter.onComplete()
        }).subscribeBy(
            onComplete = {
                println("onCompleted")
            },
            onError = {
                println(it.javaClass.name)
            },
            onNext = {
                mList.add(it)
            }
        )
        println(mList)
    }

    @Test
    fun test_retryWhen_flatMap_timer()
    {
        Observable.create(ObservableOnSubscribe<Int> { emitter ->
            emitter.onNext(1)
            emitter.onNext(2)
            emitter.onError(RuntimeException("RuntimeException"))
        }).retryWhen(Function {
            Observable.timer(5, TimeUnit.SECONDS, mTestScheduler)
        }).subscribe {
            mList.add(it)
        }

        //时间提前10s，将发生1次订阅+1次重新订阅
        mTestScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        println(mList)
    }

    @Test
    fun test_retry() {
        val arrays = arrayOf(0)
        Observable.create(ObservableOnSubscribe<Int> { emitter ->
            emitter.onNext(1)
            emitter.onNext(2)
            emitter.onNext(3 / arrays[0]++)
            emitter.onComplete()
        }).retry()
            .subscribe {
                mList.add(it)
            }
        println(mList)
    }

    @Test
    fun test_onExceptionResumeNext() {
        Observable.create(ObservableOnSubscribe<Int> { emitter ->
            emitter.onNext(1)
            emitter.onNext(2)
            emitter.onError(Throwable("throwable"))
        }).onExceptionResumeNext(Observable.just(4))
            .subscribeBy(
                onNext = {
                    mList.add(it)
                },
                onError = {
                    //onExceptionResumeNext只处理Exception类型的error，其他类型(如Error和Throwable)的异常不进行处理
                        throwable -> println(throwable.message)
                }
            )
        println(mList)
    }

    @Test
    fun test_onErrorResumeNext2() {
        Observable.create(ObservableOnSubscribe<Int> { emitter ->
            emitter.onNext(1)
            emitter.onNext(2)
            emitter.onError(NullPointerException())
        }).onErrorResumeNext(Function { throwable ->
            if (throwable is java.lang.NullPointerException) {
                return@Function Observable.just(3, 4)
            }
            Observable.just(5, 6)
        }).subscribe {
            mList.add(it)
        }
        println(mList)
    }

    @Test
    fun test_onErrorResumeNext() {
        Observable.create(ObservableOnSubscribe<Int> { emitter ->
            emitter.onNext(1)
            emitter.onNext(2)
            emitter.onNext(3)
            emitter.onError(NullPointerException())
        }).onErrorResumeNext(Function {
            Observable.just(4, 5)
        }).subscribe {
            mList.add(it)
        }
        println(mList)
    }

    @Test
    fun test_onErrorReturn() {
        Observable.create(ObservableOnSubscribe<String> { emitter ->
            emitter.onNext("○")
            emitter.onNext("○")
            emitter.onNext("○")
            emitter.onError(ArithmeticException())
        }).onErrorReturn {
            "◇"
        }.subscribe {
            mList.add(it)
        }
        println(mList)
    }

    @Test
    fun test_throttleLast() {
        Observable.create(ObservableOnSubscribe<Int> { emitter ->
            emitter.onNext(1)
            OperatorUtils.sleep(500)
            emitter.onNext(2)
            emitter.onNext(3)
            OperatorUtils.sleep(500)
            emitter.onNext(4)
            emitter.onNext(5)
            OperatorUtils.sleep(500)
            emitter.onNext(6)
            emitter.onComplete()
        })
            .subscribeOn(mTestScheduler)
            .doOnComplete {
                println("Observable Completed")
            }
            .throttleLast(500, TimeUnit.MILLISECONDS)
            .subscribe {
                mList.add(it)
            }
        mTestScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        println(mList)
    }

    @Test
    fun test_throttleFirst() {
        Observable.create(ObservableOnSubscribe<Int> { emitter ->
            emitter.onNext(1)
            OperatorUtils.sleep(500)
            emitter.onNext(2)
            emitter.onNext(3)
            OperatorUtils.sleep(500)
            emitter.onNext(4)
            emitter.onNext(5)
            OperatorUtils.sleep(500)
            emitter.onNext(6)
            emitter.onComplete()
        })
            .subscribeOn(mTestScheduler)
            .doOnComplete {
                println("Observable1-Completed")
            }
            .throttleFirst(500, TimeUnit.MILLISECONDS)
            .subscribe {
                mList.add(it)
            }
        mTestScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        println(mList)
    }

    @Test
    fun test_ignoreElements() {
        Observable.just(1, 2, 3, 4, 5, 6)
            .ignoreElements()
            .doOnComplete { mList.add("Completed") }
            .subscribe()
        println(mList)
    }

    @Test
    fun find() {
        Observable.just(2, 30, 22, 5, 60, 1)
            .filter { integer -> integer > 10 }
            .first(10)
            .subscribe { it ->
                mList.add(it)
            }
        println(mList)
    }

    @Test
    fun test_filter() {
        Observable.just(2, 30, 22, 5, 60, 1)
            .filter { integer -> integer > 10 }
            .subscribe {
                mList.add(it)
            }
        println(mList)
    }

    @Test
    fun test_elementAt() {
        Observable.just(1, 2, 3, 4)
            .elementAt(2)
            .subscribe {
                mList.add(it)
            }
        println(mList)
    }

    @Test
    fun test_distinct() {
        Observable.just(1, 2, 2, 1, 3)
            .distinct()
            .subscribe {
                mList.add(it)
            }
        println(mList)
    }

    @Test
    fun test_debounce() {
        Observable.create(ObservableOnSubscribe<Int> { emit ->
            emit.onNext(1)
            OperatorUtils.sleep(500)
            emit.onNext(2)
            emit.onNext(3)
            emit.onNext(4)
            emit.onNext(5)
            OperatorUtils.sleep(500)
            emit.onNext(6)
            emit.onComplete()
        })
            .subscribeOn(mTestScheduler)
            .doOnNext(System.out::println)
            .debounce(500, TimeUnit.MILLISECONDS)
            .subscribe {
                mList.add(it)
            }

        // 测试线程将时间提早10ms，可以保证create操作符顺利执行完毕
        mTestScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        println(mList)
    }

}