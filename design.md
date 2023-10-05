---
layout: page
title: Application Design
order: 3
---
## High level design

![thread of UI gif](/images/threadoff-high-level.svg)

As the main goal of this application was to compare the performance of virtual and platform threads, the main design goals turned out to be the following:
- automatically provide the correct service for the requested thread type
- decouple task processing and UI updates

The latter one was the most important and also the one the required the most thought. There is one main attribute of JavaFX and most other UI frameworks: there's only a single thread that's allowed to update the UI. With possibly thousands of tasks finishing in short succession, that clearly results in the main bottleneck of the application.


## Decoupling processing and UI updates
With this in mind, a solution had to be found to achieve both competing goals, correctly measuring the performance of a task execution and continuously updating the UI, without falling prey to this bottleneck. The the obvious solution to achieve this was to use a completion service.

The resulting execution flow was:
- the main thread, triggered by the start button, initializes the executor and completion service
- it also creates, using the task specific handler class, the (initial) tasks, which are then submitted to the executor
- at the same time, two tasks were started
	- a pickup task, which retrieves the finished tasks from the completion service and accumulates the results, i.e. the number of completed tasks and the shapes possibly produced by these tasks
	- the "drawing task", which reads these accumulated results and issues the UI update commands via the UI thread

![thread of UI gif](/images/threadoff-result-handling.svg)

Using this separation of result accumulation and UI updates was key to achieving the above mentioned goals. Of course, it's no absolute guarantee that the measurement of performance won't be tainted by logistic tasks: the work queue of the executor service could still become too large, or the update of the state variables used for result accumulation might be slower than tasks complete. But it's as close as one can get to such a guarantee with simple means.