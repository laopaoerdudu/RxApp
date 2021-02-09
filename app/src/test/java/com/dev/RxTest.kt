package com.dev

import com.dev.OperatorUtils.Companion.logThread
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.functions.Action
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import io.reactivex.observables.ConnectableObservable
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.HashMap
import java.util.concurrent.TimeUnit
import kotlin.Any
import kotlin.ArithmeticException
import kotlin.Char
import kotlin.ClassCastException
import kotlin.Comparator
import kotlin.Exception
import kotlin.Int
import kotlin.Long
import kotlin.NullPointerException
import kotlin.RuntimeException
import kotlin.String
import kotlin.Throwable
import kotlin.arrayOf
import kotlin.toString

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
    fun test_retryWhen_flatMap_timer() {
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
                        throwable ->
                    println(throwable.message)
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

    @Test
    fun test_reduce() {
        Observable.just(1, 2, 3, 4, 5)
            .reduce { num1, num2 -> num1 + num2 }
            .subscribe {
                mList.add(it)
            }
        Assert.assertEquals(mList, listOf(15))
    }

    @Test
    fun test_count() {
        Observable.just(1, 2, 3, 4)
            .count()
            .subscribe(Consumer {
                println(it)
                mList.add(it)
            })
    }

    @Test
    fun test_concat() {
        val observable1 =
            Observable.interval(10, TimeUnit.SECONDS, mTestScheduler)
                .map { _ -> 1 }
                .doOnNext {
                    //println("Observable1 -> map -> doOnNext -> $it")
                }
                .take(3)

        val observable2 =
            Observable.interval(1, TimeUnit.SECONDS, mTestScheduler)
                .map { _ -> 2 }
                .doOnNext {
                    //println("Observable -> map -> doOnNext -> $it")
                }
                .take(2)

        Observable.concat(observable1, observable2)
            .doOnNext(System.out::println)
            .subscribe {
                mList.add(it)
            }
        mTestScheduler.advanceTimeBy(100, TimeUnit.SECONDS)
        println(mList)
    }

    @Test
    fun test_window() {
        val list = mutableListOf<Observable<Int>>()
        Observable.just(1, 2, 3, 4, 5, 6)
            .window(3)
            .forEach {
                list.add(it)
            }
        for (i in 0..1) {
            val observable = list[i]
            println("$i -> $observable")
            observable.subscribe(System.out::println)
        }
    }

    @Test
    fun test_scan() {
        Observable.just(1, 2, 3, 4, 5)
            .scan { num1, num2 -> num1 + num2 }
            .doOnNext(System.out::println)
            .subscribe {
                mList.add(it)
            }
        Assert.assertEquals(listOf(1, 3, 6, 10, 15), mList)
    }

    @Test
    fun test_map() {
        Observable.just(1, 2, 3)
            .map { integer -> integer * 10 }
            .subscribe {
                mList.add(it)
            }
        Assert.assertEquals(mList, listOf(10, 20, 30))
    }

    @Test
    fun test_groupBy() {
        Observable.just(1, 2, 130, 3, 150, 999)
            .groupBy { num ->
                if (num > 100) {
                    return@groupBy "big"
                }
                "small"
            }
            .subscribe { groupedObservable ->
                groupedObservable.subscribe { value ->
                    val key = groupedObservable.key
                    val result = "$key -> $value"
                    // println(result)
                    mList.add(result)
                }
            }
        println(mList)
    }

    @Test
    fun test_concatMap() {
        Observable.just(1, 2, 3)
            .concatMap { num ->
                Observable.interval((num - 1).toLong(), TimeUnit.SECONDS, mTestScheduler)
                    .take(3)
                    .map {
                        "$it ◇"
                    }.doOnNext(System.out::println)
            }.subscribe(Consumer {
                mList.add(it)
            })
        mTestScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        println(mList)
    }

    @Test
    fun test_flatMap() {
        Observable.just(1, 2, 3)
            .flatMap { num ->
                Observable.interval((num - 1).toLong(), TimeUnit.SECONDS, mTestScheduler)
                    .take(3)
                    .map {
                        "$it ◇"
                    }//.doOnNext(System.out::println)

            }.subscribe(Consumer {
                mList.add(it)
            })
        mTestScheduler.advanceTimeBy(100, TimeUnit.SECONDS)
        println(mList)
    }

    @Test
    fun test_bufferWithTimeUnit() {
        Observable.interval(0, 1, TimeUnit.SECONDS, mTestScheduler)
            .take(6)
            .buffer(2, TimeUnit.SECONDS, mTestScheduler)
            .subscribe(Consumer {
                mList.add(it)
            })
        mTestScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        println(mList)
        val expectedList =
            listOf(
                listOf(0L, 1L),
                listOf(2L, 3L),
                listOf(4L, 5L)
            )
        Assert.assertEquals(mList, expectedList)
    }

    @Test
    fun test_buffer() {
        Observable.just(1, 2, 3, 4, 5, 6)
            .buffer(3)
            .subscribe(Consumer {
                mList.add(it)
            })

        println(mList)
        val exceptList =
            listOf(
                listOf(
                    1,
                    2,
                    3
                ), listOf(4, 5, 6)
            )
        Assert.assertEquals(exceptList, mList)
    }

    @Test
    fun test_timestamp() {
        println("start time -> ${System.currentTimeMillis()}")
        Observable.create(ObservableOnSubscribe<Int> { emit ->
            emit.onNext(1)
            OperatorUtils.sleep(1000)
            emit.onNext(2)
            OperatorUtils.sleep(3000)
            emit.onNext(3)
            emit.onComplete()
        })
            .timestamp()
            .subscribe(System.out::println)
        println("end -> ${System.currentTimeMillis()}")
    }

    @Test
    fun test_timeout() {
        Observable.create(ObservableOnSubscribe<Int> { emit ->
            emit.onNext(1)
            emit.onNext(2)
            emit.onNext(3)
            emit.onNext(4)
            OperatorUtils.sleep(3000)
            emit.onNext(5)
            emit.onComplete()
        })
            .subscribeOn(mTestScheduler)
            .timeout(2, TimeUnit.SECONDS) //java.util.concurrent.TimeoutException:
            .doOnError(System.out::println) //打印异常信息
            .subscribeBy(
                onNext = { num ->
                    mList.add(num)
                },

                onError = {
                    mList.add("throwable")
                }
            )
        mTestScheduler.advanceTimeBy(0, TimeUnit.SECONDS)
        Assert.assertEquals(listOf(1, 2, 3, 4, "throwable"), mList)
    }

    @Test
    fun test_timeInterval() {
        Observable.create(ObservableOnSubscribe<Int> { emit ->
            OperatorUtils.sleep(500)
            emit.onNext(1)
            OperatorUtils.sleep(1000)
            emit.onNext(2)
            OperatorUtils.sleep(2000)
            emit.onNext(3)
            OperatorUtils.sleep(3000)
            emit.onComplete()
        })
            .take(5)
            .timeInterval()
            .subscribe(System.out::println)
    }

    @Test
    fun test_subscribeOn_and_observeOn() {
        Observable.create(ObservableOnSubscribe<Int> { emit ->
            logThread("Emit -> ")
            emit.onNext(1)
            emit.onComplete()
        })
            .observeOn(Schedulers.io()) //决定了map()的线程
            .map {
                logThread("Map operator -> ")
            }
            .subscribeOn(Schedulers.newThread()) //决定了消息源的线程
            .observeOn(Schedulers.computation()) //决定了订阅者的线程
            .subscribe {
                logThread("subscriber -> ")
            }

        //保证所有线程正常执行完毕
        OperatorUtils.sleep(1000)
    }

    @Test
    fun subscribeOn() {
        Observable.create(ObservableOnSubscribe<Int> { emit ->
            logThread("emit -> ")
            emit.onNext(1)
            emit.onComplete()
        }).subscribeOn(Schedulers.newThread())
            .observeOn(Schedulers.computation())
            .subscribe(Consumer {
                logThread("subscribe -> ")
            })
    }

    @Test
    fun test_subscribe() {
        Observable.just(1, 2, 3)
            .subscribeBy(
                onNext = {
                    mList.add(it)
                },
                onError = {
                    mList.add("Error")
                },
                onComplete = {
                    mList.add("Complete")
                }
            )
        Assert.assertEquals(listOf(1, 2, 3, "Complete"), mList)
    }

    @Test
    fun test_observeOn() {
        Observable.create(ObservableOnSubscribe<Int> { emit ->
            emit.onNext(1)
            emit.onComplete()
        }).observeOn(Schedulers.newThread())
            .subscribe {
                OperatorUtils.logThread("subscribe -> ")
            }
    }

    @Test
    fun test_materialize() {
        Observable.just(1, 2)
            .materialize()
            .doOnNext(System.out::println)
            .subscribe { notification ->
                mList.add(
                    notification.value.toString()
                )
            }
        Assert.assertEquals(
            listOf("1", "2", "null"),
            mList
        )
    }

    @Test
    fun doAfterTerminate() {
        Observable.just(1, 2, 3, 4, 5, 6)
            .doAfterTerminate { mList.add("doAfterTerminate") }
            .subscribe(Consumer { i ->
                mList.add(i)
            })
        Assert.assertEquals(
            mList,
            listOf(1, 2, 3, 4, 5, 6, "doAfterTerminate")
        )
    }

    @Test
    fun test_doOnTerminate() {
        Observable.just(1)
            .doOnTerminate { mList.add("doOnTerminate by Completed") }
            .subscribe()

        Observable.create(ObservableOnSubscribe<String> { emit ->
            emit.onError(Exception("null"))
        }).doOnTerminate(Action {
            mList.add("doOnTerminate by Error")
        }).subscribeBy(
            onError = {},
            onNext = {}
        )

        Assert.assertEquals(
            mList,
            listOf(
                "doOnTerminate by Completed",
                "doOnTerminate by Error"
            )
        )
    }

    @Test
    fun doOnEach_doOnError() {
        Observable.create(ObservableOnSubscribe<Int> { emit ->
            emit.onNext(1)
            emit.onNext(5 / 0)
        })
            .doOnEach(Consumer { notification ->
                val actionName: String = notification.value.toString()
                println("doOnEach -> $actionName")
            })
            .doOnError(Consumer { throwable ->
                println("doOnError -> ${throwable.message}")
            })
            .subscribeBy(
                onNext = { num ->
                    println("subscribe onNext -> $num")
                },
                onError = { throwable ->
                    println("subscribe onError -> ${throwable.message}")
                },
                onComplete = {
                    println("subscribeBy Done")
                }
            )
    }

    @Test
    fun test_delaySubscription() {
        Observable.just(888)
            .delaySubscription(5, TimeUnit.SECONDS, mTestScheduler)
            .doOnSubscribe { println("o1 -> doOnSubscribe") }
            .doOnNext(System.out::println)
            .subscribe(Consumer { i ->
                mList.add(i)
            })

        //延时2s订阅，此数据流会先被订阅
        Observable.just(666)
            .delaySubscription(2, TimeUnit.SECONDS, mTestScheduler)
            .doOnSubscribe { println("o2 -> doOnSubscribe") }
            .doOnNext(System.out::println)
            .subscribe(Consumer { i ->
                mList.add(i)
            })
        mTestScheduler.advanceTimeBy(6, TimeUnit.SECONDS)
        Assert.assertEquals(mList, listOf(666, 888))
    }

    @Test
    fun test_delayWithSelector() {
        Observable.just(1, 2, 3)
            .delay { integer ->
                Observable.timer(
                    (integer * 20).toLong(),
                    TimeUnit.SECONDS,
                    mTestScheduler//Schedulers.newThread()
                )
            }
            .subscribe(Consumer { i ->
                println(i)
                mList.add(i)
            })
        mTestScheduler.advanceTimeTo(20, TimeUnit.SECONDS)
        Assert.assertEquals(mList, listOf(1))
        mTestScheduler.advanceTimeTo(40, TimeUnit.SECONDS)
        Assert.assertEquals(mList, listOf(1, 2))
    }

    @Test
    fun test_delay() {
        Observable.just(1, 2, 1)
            .delay(3000, TimeUnit.SECONDS, mTestScheduler)
            .subscribe(Consumer { i ->
                mList.add(i)
            })
        mTestScheduler.advanceTimeBy(2000, TimeUnit.SECONDS)
        println("after 2000ms -> $mList")
        Assert.assertTrue(mList.isEmpty())
        mTestScheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
        println("after 3000ms -> $mList")
        Assert.assertEquals(mList, listOf(1, 2, 1))
    }

    @Test
    fun test_thread_with_TestScheduler() {
        val testScheduler: TestScheduler = TestScheduler()
        println("测试线程：" + Thread.currentThread().name)

        //指定调度器
        Observable.timer(3, TimeUnit.SECONDS, testScheduler)
            .subscribe { num ->
                println("subscribe thread -> " + Thread.currentThread().name)
                println("获取订阅数据：$num")
            }

        //将时间提前了3s
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
    }

    @Test
    fun combiningOperator_thread_way2() {
        val testScheduler = TestScheduler()

        val observable1: Observable<Int> = Observable.create(ObservableOnSubscribe<Int> { emit ->
            println("observable1 -> " + Thread.currentThread().name)

            emit.onNext(1)
            OperatorUtils.sleep(500)

            emit.onNext(2)
            OperatorUtils.sleep(1500)

            emit.onNext(3)
            OperatorUtils.sleep(250)

            emit.onNext(4)
            OperatorUtils.sleep(500)

            emit.onNext(5)
            emit.onComplete()
        }).subscribeOn(testScheduler)

        val observable2: Observable<Int> = Observable.create(ObservableOnSubscribe<Int> { emit ->
            OperatorUtils.sleep(200)
            println("observable2-->" + Thread.currentThread().name)
            emit.onNext(1111);
            emit.onNext(2222);
            emit.onNext(3333);
            emit.onComplete()
        }).subscribeOn(Schedulers.newThread())

        Observable.merge(observable1, observable2).subscribe(System.out::println)
        //并将时钟提前
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
    }

    @Test
    fun combiningOperator_thread_way1() {
        val observable1: Observable<Int> = Observable.create(ObservableOnSubscribe<Int> { emit ->
            println("observable1 -> " + Thread.currentThread().name)

            emit.onNext(1)
            OperatorUtils.sleep(500)

            emit.onNext(2)
            OperatorUtils.sleep(1500)

            emit.onNext(3)
            OperatorUtils.sleep(250)

            emit.onNext(4)
            OperatorUtils.sleep(500)

            emit.onNext(5)
            emit.onComplete()
        }).subscribeOn(Schedulers.newThread())

        val observable2: Observable<Int> = Observable.create(ObservableOnSubscribe<Int> { emit ->
            OperatorUtils.sleep(200)
            println("observable2-->" + Thread.currentThread().name)
            emit.onNext(1111);
            emit.onNext(2222);
            emit.onNext(3333);
            emit.onComplete()
        }).subscribeOn(Schedulers.newThread())

        Observable.merge(observable1, observable2).subscribe(System.out::println)

        //测试线程休眠一定时间，保证两个消息源所在线程能正常执行完毕
        OperatorUtils.sleep(5000)
    }

    @Test
    fun test_buffer_1() {
        val testSubscriber = TestSubscriber<List<String>>()

        //缓冲2个发射一次
        Flowable.just("A", "B", "C", "D")
            .buffer(2)
            .subscribe(testSubscriber)

        testSubscriber.assertResult(listOf("A", "B"), listOf("C", "D"))
        testSubscriber.assertValueCount(2)
        testSubscriber.assertComplete()
    }

    @Test
    fun test_timer_2() {
        val testSubscriber = TestSubscriber<Long>()
        //延时5秒发射
        Flowable.timer(5, TimeUnit.SECONDS, mTestScheduler)
            .subscribe(testSubscriber)

        //时间到5秒
        mTestScheduler.advanceTimeTo(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(0L)
        testSubscriber.assertValueCount(1)
        testSubscriber.assertComplete()
    }

    //测试这种有关时间的操作符，使用异步转同步的思路(RxJavaRule),但是这样我们的测试方法也要等待10s
    //所以还是沿用以下的测试方法，TestScheduler完成对时间的操控

    @Test
    fun test_interval_2() {
        val testSubscriber = TestSubscriber<Long>()

        //隔1秒发射一次，一共10次
        Flowable.interval(1, TimeUnit.SECONDS, mTestScheduler)
            .take(10)
            .subscribe(testSubscriber)

        //时间经过3秒
        mTestScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriber.assertValues(0L, 1L, 2L)
        testSubscriber.assertValueCount(3)
        testSubscriber.assertNotComplete()

        //时间再经过2秒
        mTestScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValues(0L, 1L, 2L, 3L, 4L)
        testSubscriber.assertValueCount(5)
        testSubscriber.assertNotComplete()

        //时间到10秒
        mTestScheduler.advanceTimeTo(10, TimeUnit.SECONDS)
        testSubscriber.assertValueCount(10)
        testSubscriber.assertComplete()
    }

    @Test
    fun test_buffer_2() {
        val testSubscriber = TestSubscriber<List<String>>()

        //缓冲2个发射一次
        Flowable.just("A", "B", "C", "D")
            .buffer(2)
            .subscribe(testSubscriber)

        testSubscriber.assertResult(listOf("A", "B"), listOf("C", "D"))
        testSubscriber.assertValueCount(2)
        testSubscriber.assertComplete()
    }

    @Test
    fun test_repeat_2() {
        val testSubscriber = TestSubscriber<Int>()
        Flowable.fromIterable(listOf(1, 2))
            .repeat(2) //重复发送2次
            .subscribe(testSubscriber)

        testSubscriber.assertValues(1, 2, 1, 2)
        testSubscriber.assertValueCount(4)
        testSubscriber.assertComplete()
    }

    @Test
    fun test_range_2() {
        val testSubscriber = TestSubscriber<Int>()

        //从3开始发射3个连续的int
        Flowable.range(3, 3).subscribe(testSubscriber)

        testSubscriber.assertValues(3, 4, 5)
        testSubscriber.assertValueCount(3)
        testSubscriber.assertComplete()
    }

    @Test
    fun test_from_2() {
        val testSubscriber = TestSubscriber<Int>()

        //依次发射list中的数
        Flowable.fromIterable(listOf(1, 2)).subscribe(testSubscriber)

        testSubscriber.assertValues(1, 2)
        testSubscriber.assertValueCount(2)
        testSubscriber.assertComplete()
    }

    @Test
    fun test_error_2() {
        val testSubscriber = TestSubscriber<Any>()
        val exception = RuntimeException("error")
        Flowable.error<Any>(exception).subscribe(testSubscriber)
        testSubscriber.assertError(exception)
        testSubscriber.assertError {
            it.message == "error"
        }
    }

    @Test
    fun test_just_2() {
        val testSubscriber = TestSubscriber<String>()

        //依次发射A，B，C
        Flowable.just("A", "B", "C").subscribe(testSubscriber)

        //断言值是否不存在
        testSubscriber.assertNever("D")

        //断言值是否相等
        testSubscriber.assertValues("A", "B", "C")

        //断言值的数量是否相等
        testSubscriber.assertValueCount(3)
        testSubscriber.assertComplete()
        testSubscriber.assertTerminated()
    }

    @Test
    fun test_observer_2() {
        val testObserver = TestObserver.create<Int>()
        testObserver.onNext(1)
        testObserver.onNext(2)
        testObserver.onComplete()

        //断言值是否相等
        testObserver.assertValues(1, 2)

        //断言是否完成
        testObserver.assertComplete()
    }
}