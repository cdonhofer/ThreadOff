---
layout: page
title: About ThreadOff
order: 0
---
## What's this all about?
ThreadOff is the result of a little fun project of mine. It's a UI you can use to compare the performance of virtual threads with that of platform threads on different types of tasks.

![thread of UI gif](/images/ThreadOff-title.gif)


It allows you to run the same tasks with both types of threads and presents you with statistics about their execution. You can easily experiment with certain settings to get the best possible results:
- number of executions (times the task is run)
- thread type
- size of the thread pool (for platform threads)

There's a list of pre-defined tasks, but you can also easily extend it to [try out your own task](try-out-your-own-tasks.md)'s performance with the above settings. The pre-defined tasks include:
- blocking tasks
- synchronized tasks
- non-blocking calculations, e.g. calculation of a Koch Snowflake

For these purposes, you can either download the ready-to-run jar, or check out the maven project: [get ThreadOff](get-it.md)


## Goals
 I started ThreadOff because I wanted to learn about the new *virtual threads* which were introduced by Project Loom, and at the same time try out some of the latest other features of Java, such as
- record patterns
- sealed types
- algebraic data types (learn more about those [here](https://www.infoq.com/articles/data-oriented-programming-java/))

Combine that with learning more about concurrency in an unfamiliar scenario, i.e. something other than web server applications, and you get project ThreadOff.

**Thus, the main goals were to learn**
- how to use virtual threads
- how existing code could be adapted to switch from platform threads to virtual threads
- when virtual threads are the better option, and when they're not

**Design goals:**
- implement a responsive interruption policy
- avoid UI freezes, find a way to efficiently handle drawing large numbers of shapes from thousands of calculation threads in the single UI thread
- ... and make sure rendering is still smooth
- implement a solid handling of tasks and their results
- support both visual and non-visual tasks

Check out the high level design [here](design.md)


## Conclusions / Learnings
Virtual threads are the one-size-fits-all tool in Java when it comes to concurrency: 
- they are really easy to use:
	- no setup of thread pools
	- no thinking about what size those pools should be
	- ... or which pool type to use
- they are pretty fast for all types of task, and very fast for some:
	- they perfectly serve the main intention behind them: spawning many blocking tasks
	- they also do really well in non-blocking tasks of all sizes

Platform Threads, apart from the "many-blocking-tasks" scenario, can still be tuned to be faster than virtual threads (with their newVirtualThreadPerTaskExecutor). But that's exactly the thing:
- you have to carefully think about what size your tasks should be
- which size the thread pool should be
- what type of pool to use
- etc.

So, although there's still a lot to explore, my impression is that both types of threads are here to stay. While virtual threads perfectly fulfill their main intentions ([see JEP 425](https://openjdk.org/jeps/425)), they also serve as a way of simplifying concurrency in most situations.

When your tasks need to be highly efficient though, you might still find yourself preferring the traditional platform threads.