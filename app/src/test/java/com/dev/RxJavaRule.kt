package com.dev

import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

public class RxJavaRule : TestRule {

    private final val mTestScheduler = TestScheduler()

    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                //将异步转化为同步
                RxJavaPlugins.setIoSchedulerHandler { mTestScheduler }
                RxJavaPlugins.setComputationSchedulerHandler { mTestScheduler }
                RxAndroidPlugins.setMainThreadSchedulerHandler { mTestScheduler }
                RxAndroidPlugins.setInitMainThreadSchedulerHandler { mTestScheduler }
                base?.evaluate()
                RxJavaPlugins.reset()
                RxAndroidPlugins.reset()
            }
        }
    }
}