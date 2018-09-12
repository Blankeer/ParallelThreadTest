# 多线程并发的实现
## 需求
多线程并发执行逻辑，等待所有线程执行完毕，取回所有结果，再将结果做后续处理。

举例：
- 同时发送多个 http 请求，等待所有请求完成，获得所有响应，做后续处理。

## 实现
1. CountDownLatch

利用并发工具类`CountDownLatch`,因为每个线程执行结束时间是随机的，所以 result 是乱序的，可以通过判断下标的方式变为有序。
```kotlin
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
```

2. RxJava 的 flatMap

利用 toList(),结果也是乱序的
```kotlin
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
```

3. 利用 RxJava 的 zip (推荐)

利用zip操作符，可以很好的将多个流结合，结果是有序的，按照流的顺序
```kotlin
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
```

4. 利用 RxJava 的 merge

merge 合并多个流为一个流，所以结果是无序的
```kotlin
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
``` 