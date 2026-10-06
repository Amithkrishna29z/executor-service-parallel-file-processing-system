# Parallel File Processing System — Bug Fixes & Code Walkthrough

A Java 21 / Maven learning lab for `ExecutorService`. This document records the bugs that were
fixed in `App.java`, then explains every class in the project along with its **actual** output
(each snippet below was captured by compiling and running the code, not written from memory).

- **JDK:** 21.0.10 LTS
- **Maven:** 3.9.10
- **Module:** `executor-learning` (`com.amith.executor:executor-learning:1.0-SNAPSHOT`)

---

## Table of Contents

1. [Bugs Fixed in App.java](#1-bugs-fixed-in-appjava)
2. [How to Build and Run](#2-how-to-build-and-run)
3. [The Core Pipeline](#3-the-core-pipeline)
   - [App.java](#31-appjava)
   - [FileTask.java](#32-filetaskjava)
   - [FileResult.java](#33-fileresultjava)
   - [FileAnalyzer.java](#34-fileanalyzerjava)
4. [Basic Examples](#4-basic-examples)
5. [Thread Pool Types](#5-thread-pool-types)
6. [Advanced Patterns](#6-advanced-patterns)
7. [Concepts Cheat Sheet](#7-concepts-cheat-sheet)
8. [Remaining Observations (not changed)](#8-remaining-observations-not-changed)

---

## 1. Bugs Fixed in App.java

The project did not compile. `javac` reported:

```
src\main\java\com\amith\executor\App.java:9: error: cannot find symbol
        FileAnalyzer analyzer = new FileAnalyzer(4);
        ^
  symbol:   class FileAnalyzer
  location: class App
src\main\java\com\amith\executor\App.java:9: error: cannot find symbol
        FileAnalyzer analyzer = new FileAnalyzer(4);
                                    ^
  symbol:   class FileAnalyzer
  location: class App
2 errors
```

### Bug #1 — Missing import (`cannot find symbol: class FileAnalyzer`)

`App` lives in package `com.amith.executor`, but `FileAnalyzer` lives in the **sub-package**
`com.amith.executor.tasks`.

Java does **not** import sub-packages implicitly. A class gets unqualified access only to types in
its *own* package (plus `java.lang`). `com.amith.executor.tasks` is a completely separate package
as far as the compiler is concerned — the dotted name implies no parent/child relationship at the
language level. So the simple name `FileAnalyzer` was unresolvable.

**Fix** — add the explicit import:

```java
import com.amith.executor.tasks.FileAnalyzer;
```

### Bug #2 — Unhandled checked exception

`FileAnalyzer.run()` is declared as:

```java
public void run() throws Exception { ... }
```

It must be, because it calls two methods that throw checked exceptions:

| Call | Checked exception thrown |
| --- | --- |
| `future.get()` | `InterruptedException`, `ExecutionException` |
| `executor.awaitTermination(...)` | `InterruptedException` |

`App.main` called `analyzer.run()` inside a `main` declared as plain
`public static void main(String[] args)` — no `throws`, no `try/catch`. Under Java's
*catch-or-specify* rule this is a compile error:

```
unreported exception java.lang.Exception; must be caught or declared to be thrown
```

This error was **masked** by Bug #1: once the type `FileAnalyzer` fails to resolve, the compiler
cannot determine the signature of `.run()`, so it never gets to the exception check. Fixing only
the import would have surfaced this as a second round of errors.

**Fix** — propagate it from `main`:

```java
public static void main(String[] args) throws Exception {
```

Letting it escape `main` is the right call for a learning lab: an exception becomes a visible stack
trace on stderr and a non-zero exit code, rather than being silently swallowed by a `catch` block.

### The diff

```diff
  package com.amith.executor;

+ import com.amith.executor.tasks.FileAnalyzer;
+
  public class App {
-     public static void main(String[] args) {
+     public static void main(String[] args) throws Exception {
          System.out.println("=========================");
```

### Verification

```
$ javac -d out $(find src/main/java -name '*.java')
COMPILE OK
```

Both errors are gone and the application runs end-to-end (output in [§3.1](#31-appjava)).

---

## 2. How to Build and Run

```bash
cd executor-learning

# Maven
mvn clean package
mvn exec:java -Dexec.mainClass=com.amith.executor.App

# Or plain javac/java
javac -d out $(find src/main/java -name '*.java')
java -cp out com.amith.executor.App
```

---

## 3. The Core Pipeline

This is the part `App` actually exercises. Flow:

```
App.main
   │
   ├─ new FileAnalyzer(4)        → creates a fixed pool of 4 threads
   │
   └─ analyzer.run()
         │
         ├─ for each of 10 file names:
         │     executor.submit(new FileTask(name))  → returns Future<FileResult>
         │     (stored in a List, preserving submission order)
         │
         ├─ for each Future (in order):
         │     future.get()      → BLOCKS until that task is done
         │     print FileResult
         │
         └─ shutdown() + awaitTermination(10s)
```

### 3.1 `App.java`

The entry point. Prints a banner, builds a `FileAnalyzer` with a 4-thread pool, runs it, prints a
closing line.

```java
package com.amith.executor;

import com.amith.executor.tasks.FileAnalyzer;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("=========================");
        System.out.println("ExecutorService Learning Lab");
        System.out.println("=========================");

        FileAnalyzer analyzer = new FileAnalyzer(4);

        analyzer.run();

        System.out.println("\n Application finished");
    }
}
```

**Line-by-line:**

| Line | What it does |
| --- | --- |
| `import ...tasks.FileAnalyzer;` | **Bug #1 fix.** Brings the sub-package type into scope. |
| `throws Exception` | **Bug #2 fix.** Satisfies catch-or-specify for `run()`. |
| `new FileAnalyzer(4)` | 4 worker threads for 10 tasks → tasks run in waves of 4. |
| `analyzer.run()` | Blocking call; returns only after all 10 results are collected. |
| `"\n Application finished"` | Proof `run()` completed *and* the pool shut down cleanly. |

**Actual output:**

```
=========================
ExecutorService Learning Lab
=========================

All tasks submitted.

START Processing file-01.txt on pool-1-thread-1
START Processing file-03.txt on pool-1-thread-3
START Processing file-04.txt on pool-1-thread-4
START Processing file-02.txt on pool-1-thread-2
End processing file-01.txt on pool-1-thread-1
End processing file-02.txt on pool-1-thread-2
End processing file-04.txt on pool-1-thread-4
End processing file-03.txt on pool-1-thread-3
START Processing file-05.txt on pool-1-thread-3
START Processing file-07.txt on pool-1-thread-2
START Processing file-08.txt on pool-1-thread-1
START Processing file-06.txt on pool-1-thread-4
file-01.txt     lines=423   Words=4230  Thread=pool-1-thread-1
file-02.txt     lines=292   Words=2920  Thread=pool-1-thread-2
file-03.txt     lines=343   Words=3430  Thread=pool-1-thread-3
file-04.txt     lines=464   Words=4640  Thread=pool-1-thread-4
End processing file-06.txt on pool-1-thread-4
End processing file-05.txt on pool-1-thread-3
End processing file-08.txt on pool-1-thread-1
End processing file-07.txt on pool-1-thread-2
START Processing file-09.txt on pool-1-thread-4
START Processing file-10.txt on pool-1-thread-3
file-05.txt     lines=439   Words=4390  Thread=pool-1-thread-3
file-06.txt     lines=395   Words=3950  Thread=pool-1-thread-4
file-07.txt     lines=160   Words=1600  Thread=pool-1-thread-2
file-08.txt     lines=484   Words=4840  Thread=pool-1-thread-1
End processing file-09.txt on pool-1-thread-4
End processing file-10.txt on pool-1-thread-3
file-09.txt     lines=236   Words=2360  Thread=pool-1-thread-4
file-10.txt     lines=461   Words=4610  Thread=pool-1-thread-3

 Executor terminated.

 Application finished
```

**Four things worth noticing in that output:**

1. **`All tasks submitted.` appears before any `START` line.** `submit()` is non-blocking — it
   queues the task and returns a `Future` immediately. The main thread raced ahead and printed its
   message before the workers got scheduled.
2. **`START` lines are interleaved and out of order** (01, 03, 04, 02). Four threads start at once
   and the OS scheduler decides who prints first. This is expected non-determinism, not a bug.
3. **Results print in strict submission order** (01→10) even though tasks *finished* out of order.
   That is the whole point of iterating the `List<Future>` in order: `future.get()` on `file-01`
   blocks until file-01 is ready, so ordering is restored on the consuming side.
4. **Work happens in waves of 4.** With 10 tasks on 4 threads and a 1-second sleep each, total
   wall-clock is ~3 seconds (4 + 4 + 2 tasks), not 10. Thread names get reused across waves
   (`pool-1-thread-3` runs file-03, then file-05, then file-10).

`lines`/`words` values change on every run — they come from `Math.random()`.

### 3.2 `FileTask.java`

The unit of work. Implements `Callable<FileResult>`, which is the `Runnable` equivalent that **can
return a value and can throw a checked exception**.

```java
public class FileTask implements Callable<FileResult> {
    private final String fileName;

    public FileTask(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public FileResult call() throws Exception {
        String threadName = Thread.currentThread().getName();

        System.out.println("START Processing " + fileName + " on " + threadName);

        Thread.sleep(1000);

        int lines = (int) (Math.random() * 500);
        int words = lines * 10;

        System.out.println("End processing " + fileName + " on " + threadName);

        return new FileResult(fileName, lines, words, threadName);
    }
}
```

**Key points:**

- **`Callable<FileResult>` vs `Runnable`:** `Runnable.run()` returns `void` and cannot throw checked
  exceptions. `Callable.call()` returns `V` and is declared `throws Exception`. Since we need the
  line/word counts back, `Callable` is the correct choice.
- **`Thread.currentThread().getName()`** is captured inside `call()`, so it records the *worker*
  thread (`pool-1-thread-N`), not `main`. This is what makes the parallelism visible in the output.
- **`Thread.sleep(1000)`** simulates I/O latency — a stand-in for actually reading a file. It is
  what makes the wave pattern observable.
- **No real file I/O.** Nothing is opened; `lines` is `Math.random()*500` and `words` is simply
  `lines * 10`. The file names are labels. This is deliberate for a concurrency lab — it isolates
  the executor mechanics from filesystem concerns, but it does mean `words` is never independent
  data.
- **`call()` is the only place state is created**, and `fileName` is `final`. The task holds no
  mutable shared state, so it is safe to run on any thread without synchronization.

### 3.3 `FileResult.java`

An immutable value object carrying one task's outcome back to the caller.

```java
public class FileResult {
    private final String fileName;
    private final int lines;
    private final int words;
    private final String threadName;

    // constructor + getters ...

    @Override
    public String toString() {
        return String.format("%-15s lines=%-5d Words=%-5d Thread=%s",
                             fileName, lines, words, threadName);
    }
}
```

**Key points:**

- **All four fields are `final`** and there are no setters. The object is immutable, so it can be
  handed from a worker thread to the main thread with no locking. Immutability is the simplest
  correct way to publish data across threads.
- **`toString()` does the formatting.** `%-15s` left-pads the filename to 15 chars and `%-5d` pads
  the numbers to 5 — that is what produces the aligned columns in the output. `System.out.println(result)`
  calls `toString()` implicitly.
- **Could be a `record`.** On Java 21 this entire class collapses to
  `record FileResult(String fileName, int lines, int words, String threadName)` plus the custom
  `toString()`. Left as-is — the explicit form shows what a record generates for you.

### 3.4 `FileAnalyzer.java`

The orchestrator: owns the pool, submits all tasks, collects all results, shuts down.

```java
public class FileAnalyzer {
    private final ExecutorService executor;

    public FileAnalyzer(int numberOfTheads) {
        this.executor = Executors.newFixedThreadPool(numberOfTheads);
    }

    public void run() throws Exception {
        List<String> files = List.of("file-01.txt", ..., "file-10.txt");

        List<Future<FileResult>> futures = new ArrayList<>();

        // submit tasks
        for (String file : files) {
            FileTask task = new FileTask(file);
            Future<FileResult> future = executor.submit(task);
            futures.add(future);
        }
        System.out.println("\nAll tasks submitted. \n");

        // Collect Results
        for (Future<FileResult> future : futures) {
            FileResult result = future.get();
            System.out.println(result);
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\n Executor terminated.");
    }
}
```

**The two-loop pattern is the heart of this class.** It is deliberately *submit everything first,
then collect* — not submit-and-get in one loop:

```java
// ✅ What the code does — all 10 tasks run concurrently
for (String f : files) futures.add(executor.submit(new FileTask(f)));
for (Future<FileResult> fu : futures) System.out.println(fu.get());

// ❌ The anti-pattern — get() inside the submit loop serializes everything
for (String f : files) System.out.println(executor.submit(new FileTask(f)).get());
```

The second version blocks on each result before submitting the next task, so only one thread is
ever busy and the pool is pointless — 10 seconds instead of 3.

**Other key points:**

- **`Executors.newFixedThreadPool(n)`** creates exactly `n` threads backed by an *unbounded*
  `LinkedBlockingQueue`. The extra 6 tasks wait in that queue; a thread picks the next one up the
  moment it finishes.
- **`submit()` returns immediately.** It returns a `Future<FileResult>` — a handle to a result that
  does not exist yet.
- **`future.get()` blocks** until that specific task completes. If the task threw, `get()` rethrows
  it wrapped in an `ExecutionException` (the original is available via `getCause()`).
- **`List.of(...)`** creates an immutable list (Java 9+).
- **`shutdown()` is not optional.** Pool threads are *non-daemon*, so if you never shut the pool
  down the JVM will not exit even after `main` returns. This is the single most common
  `ExecutorService` mistake.
- **`shutdown()` vs `shutdownNow()`:** `shutdown()` is a graceful request — stop accepting new
  tasks, finish what is queued. `shutdownNow()` attempts to interrupt running tasks and returns the
  queued ones that never ran.
- **`awaitTermination(10, SECONDS)`** blocks until the pool is actually done (or the timeout
  elapses). Here every `get()` has already returned, so all work is finished and this returns
  essentially instantly.

---

## 4. Basic Examples

These classes are standalone demos — **`App` does not call them.** The outputs below were captured
by invoking each `run()` / factory method directly.

### 4.1 `RunnableExample.java`

Submits 10 `Runnable` lambdas (no return value) to a 3-thread pool.

```java
ExecutorService executor = Executors.newFixedThreadPool(3);

for (int i = 1; i <= 10; i++) {
    int taskId = i;                              // ← effectively-final copy
    executor.submit(() -> {
        System.out.println("Task " + taskId + " running on "
                           + Thread.currentThread().getName());
    });
}
executor.shutdown();
```

**Key point — `int taskId = i;` is required, not decoration.** A lambda may only capture
*effectively final* local variables. The loop counter `i` is reassigned on every iteration, so
`i` itself cannot be captured — `error: local variables referenced from a lambda expression must
be final or effectively final`. Copying it into a fresh per-iteration variable is the standard fix.

**Actual output:**

```
Task 3 running on pool-1-thread-3
Task 2 running on pool-1-thread-2
Task 1 running on pool-1-thread-1
Task 5 running on pool-1-thread-3
Task 4 running on pool-1-thread-1
Task 6 running on pool-1-thread-2
Task 7 running on pool-1-thread-3
Task 8 running on pool-1-thread-1
Task 9 running on pool-1-thread-2
Task 10 running on pool-1-thread-3
```

Tasks 1–3 print out of order (3, 2, 1) — the 3 threads start simultaneously. Later tasks are
roughly ordered because the tasks are so short that threads free up one at a time. Only 3 distinct
thread names appear across all 10 tasks: **threads are reused**, which is the entire economic
argument for a pool over `new Thread()` per task.

### 4.2 `CallableExample.java`

A factory that returns a `Callable<Integer>` rather than running anything itself.

```java
public static Callable<Integer> createTask(int number) {
    return () -> {
        System.out.println("Calculating " + number + " on "
                           + Thread.currentThread().getName());
        return number * number;
    };
}
```

**Key point:** nothing executes when `createTask(7)` is called — it just builds a lambda. The body
runs only when some executor invokes `call()`. The `number` parameter is captured by the closure
(method parameters are effectively final if never reassigned, so no copy is needed here).

Driven with `executor.submit(CallableExample.createTask(7))`, **actual output:**

```
Calculating 7 on pool-1-thread-1
Result = 49
```

Note the ordering: `Calculating` is printed by the *worker* thread; `Result = 49` is printed by
`main` after `future.get()` returns.

### 4.3 `FutureExamle.java`

> Note the filename typo — `FutureExamle`, missing the `p`. Left unchanged (see [§8](#8-remaining-observations-not-changed)).

Demonstrates that `submit()` does not block but `get()` does.

```java
ExecutorService executor = Executors.newFixedThreadPool(3);

Callable<Integer> task = () -> {
    Thread.sleep(2000);
    return 100;
};

Future<Integer> future = executor.submit(task);

System.out.println("Task submitted");
System.out.println("Doing other work...");

Integer result = future.get();      // ← blocks here for ~2s
System.out.println("Result = " + result);
executor.shutdown();
```

**Actual output:**

```
Task submitted
Doing other work...
Result = 100
```

The first two lines appear **instantly**; `Result = 100` appears ~2 seconds later. That gap is the
lesson: between `submit()` and `get()` the main thread is free to do real work. Call `get()` only
when you actually need the value.

Useful `Future` methods not shown here: `isDone()` (non-blocking poll), `cancel(boolean)`,
`get(timeout, unit)` (see [§6.3](#63-timeoutexamplejava)).

---

## 5. Thread Pool Types

Four `Executors` factory methods, side by side.

### 5.1 `FixedThreadPoolExample.java`

10 tasks, 3 threads, 2-second sleep each — the clearest demonstration of queueing.

```java
ExecutorService executor = Executors.newFixedThreadPool(3);

for (int i = 1; i <= 10; i++) {
    int taskId = i;
    executor.submit(() -> {
        try {
            System.out.println("START Task " + taskId + " -> " + Thread.currentThread().getName());
            Thread.sleep(2000);
            System.out.println("End Task " + taskId + " -> " + Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // ← restore the interrupt flag
        }
    });
}
executor.shutdown();
```

**Key point — `Thread.currentThread().interrupt()` in the catch block.** When
`InterruptedException` is thrown, the JVM *clears* the thread's interrupt flag. Swallowing the
exception without restoring the flag loses the cancellation signal, and code further up the stack
(the pool itself, during `shutdownNow()`) can no longer tell the thread was asked to stop.
Re-interrupting is the correct idiom whenever you catch `InterruptedException` and cannot propagate
it — and a lambda implementing `Runnable` *cannot* propagate it, which is why the `try/catch` is
there at all.

**Actual output:**

```
START Task 2 -> pool-1-thread-2
START Task 1 -> pool-1-thread-1
START Task 3 -> pool-1-thread-3
End Task 2 -> pool-1-thread-2
End Task 1 -> pool-1-thread-1
End Task 3 -> pool-1-thread-3
START Task 4 -> pool-1-thread-3
START Task 5 -> pool-1-thread-2
START Task 6 -> pool-1-thread-1
End Task 5 -> pool-1-thread-2
End Task 4 -> pool-1-thread-3
End Task 6 -> pool-1-thread-1
START Task 7 -> pool-1-thread-2
START Task 8 -> pool-1-thread-3
START Task 9 -> pool-1-thread-1
End Task 9 -> pool-1-thread-1
End Task 8 -> pool-1-thread-3
End Task 7 -> pool-1-thread-2
START Task 10 -> pool-1-thread-1
End Task 10 -> pool-1-thread-1
```

Perfectly visible batching: **3 START → 3 End → 3 START → 3 End → 3 START → 3 End → 1 START →
1 End**. Four waves at 2 seconds each ≈ 8 seconds total. Tasks 4–10 sat in the queue until a
thread freed up. `shutdown()` does **not** cancel queued tasks — all 10 still ran.

### 5.2 `CachedThreadPoolExample.java`

20 trivial tasks on a pool that grows on demand.

```java
ExecutorService executor = Executors.newCachedThreadPool();

for (int i = 1; i <= 20; i++) {
    int taskId = i;
    executor.submit(() -> {
        System.out.println("Task " + taskId + " -> " + Thread.currentThread().getName());
    });
}
executor.shutdown();
```

**Actual output:**

```
Task 4 -> pool-1-thread-4
Task 12 -> pool-1-thread-12
Task 6 -> pool-1-thread-6
Task 14 -> pool-1-thread-14
Task 9 -> pool-1-thread-9
Task 7 -> pool-1-thread-7
Task 10 -> pool-1-thread-10
Task 1 -> pool-1-thread-1
Task 11 -> pool-1-thread-11
Task 20 -> pool-1-thread-20
Task 8 -> pool-1-thread-8
Task 19 -> pool-1-thread-19
Task 3 -> pool-1-thread-3
Task 2 -> pool-1-thread-2
Task 5 -> pool-1-thread-5
Task 16 -> pool-1-thread-16
Task 15 -> pool-1-thread-15
Task 17 -> pool-1-thread-17
Task 18 -> pool-1-thread-18
Task 13 -> pool-1-thread-13
```

**Contrast this with the fixed pool: 20 tasks created ~20 distinct threads** (`thread-1` through
`thread-20`), and the output order is thoroughly scrambled.

Why? A cached pool is backed by a `SynchronousQueue`, which has **zero capacity**. A task can only
be handed off to a thread that is already waiting for one; if none is idle at that instant, the pool
spawns a new thread rather than queueing. Here the submit loop runs faster than threads can be
recycled, so it spawns a new one almost every iteration.

**The practical warning:** a cached pool is *unbounded*. Under a flood of slow tasks it will keep
creating threads until the JVM throws `OutOfMemoryError: unable to create new native thread`. Use
it only for large numbers of short-lived tasks; prefer a fixed or bounded pool otherwise. Idle
threads are reaped after 60 seconds.

### 5.3 `SingleThreadExecutorExample.java`

One thread — guarantees sequential execution.

```java
ExecutorService executor = Executors.newSingleThreadExecutor();

for (int i = 1; i <= 5; i++) {
    int taskId = i;
    executor.submit(() -> {
        System.out.println("Task " + taskId + " -> " + Thread.currentThread().getName());
    });
}
executor.shutdown();
```

**Actual output:**

```
Task 1 -> pool-1-thread-1
Task 2 -> pool-1-thread-1
Task 3 -> pool-1-thread-1
Task 4 -> pool-1-thread-1
Task 5 -> pool-1-thread-1
```

**The only deterministic output in this entire project.** Tasks run strictly in submission order,
all on `pool-1-thread-1`. Because exactly one thread touches the work, tasks are implicitly
mutually exclusive — no synchronization needed for state they share. That makes this the right
choice for an event loop, an append-only log writer, or any ordered queue.

Compare with `newFixedThreadPool(1)`: functionally near-identical, but a single-thread executor is
wrapped so it cannot be reconfigured to add threads later, and it replaces its worker if one dies
from an uncaught exception.

### 5.4 `ScheduledExecutorExample.java`

Delayed and recurring execution.

```java
ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

executor.schedule(() -> System.out.println("Executed after 3 seconds"), 3, TimeUnit.SECONDS);

executor.scheduleAtFixedRate(
        () -> System.out.println("Periodic task: " + System.currentTimeMillis()),
        1, 2, TimeUnit.SECONDS);          // initialDelay=1s, period=2s

executor.schedule(executor::shutdown, 10, TimeUnit.SECONDS);
```

**Actual output:**

```
Periodic task: 1791276876101
Periodic task: 1791276878088
Executed after 3 seconds
Periodic task: 1791276880091
Periodic task: 1791276882104
Periodic task: 1791276884095
```

Read the timeline against the timestamps:

| t | Event | Timestamp |
| --- | --- | --- |
| 1s | periodic #1 | `...876101` |
| 3s | periodic #2 | `...878088` (+1987 ms) |
| 3s | one-shot `schedule` fires | — |
| 5s | periodic #3 | `...880091` (+2003 ms) |
| 7s | periodic #4 | `...882104` |
| 9s | periodic #5 | `...884095` |
| 10s | `executor::shutdown` runs → periodic task stops | — |

The deltas are consistently ~2000 ms, confirming the fixed *rate*. Note periodic #2 (t=3s) printed
*before* the one-shot also due at t=3s — a tie broken arbitrarily by the 2 scheduler threads.

**Key points:**

- **A periodic task runs forever** until cancelled or the pool shuts down. The third
  `schedule(executor::shutdown, 10, SECONDS)` is what makes this program terminate — self-shutdown
  via a method reference. Without it the JVM would never exit.
- **`scheduleAtFixedRate` vs `scheduleWithFixedDelay`:** *fixed rate* targets a constant start-time
  cadence (start every 2 s regardless of how long a run takes; runs can bunch up if one overruns).
  *Fixed delay* waits a constant gap *after* each run finishes. Use fixed delay when overlap or
  pile-up would be harmful.
- **An uncaught exception silently kills a periodic task** — no further executions, and no error
  printed unless you hold the `ScheduledFuture` and call `get()`. Always wrap periodic task bodies
  in `try/catch`.

---

## 6. Advanced Patterns

### 6.1 `InvokeAllExample.java`

Submit a batch and wait for **all** of it in one call.

```java
ExecutorService executor = Executors.newFixedThreadPool(3);
List<Callable<Integer>> tasks = new ArrayList<>();

for (int i = 1; i <= 5; i++) {
    int number = i;
    tasks.add(() -> {
        Thread.sleep(1000);
        return number * number;
    });
}

List<Future<Integer>> futures = executor.invokeAll(tasks);   // ← blocks until ALL finish

for (Future<Integer> future : futures) {
    System.out.println("Result = " + future.get());
}
executor.shutdown();
```

**Actual output:**

```
Result = 1
Result = 4
Result = 9
Result = 16
Result = 25
```

**Key points:**

- **`invokeAll` blocks** until every task completes (normally or by throwing). When it returns,
  every `Future` in the list is already done — so the subsequent `get()` calls never block.
- **The returned list preserves the order of the input collection**, not completion order. Hence
  the clean 1, 4, 9, 16, 25 — squares of 1..5 in order, despite 5 tasks racing on 3 threads.
- **vs. the manual loop in `FileAnalyzer`:** `invokeAll` is the concise form when you have all the
  tasks up front and want all the results. `FileAnalyzer`'s hand-rolled submit-then-collect loop is
  equivalent but lets you process each result the moment it is ready.
- **Exceptions do not escape `invokeAll`** — a failed task's exception surfaces from *its*
  `future.get()` as an `ExecutionException`. So one bad task will not hide the other results.

### 6.2 `InvokeAnyExample.java`

Race several alternatives; keep the first winner.

```java
ExecutorService executor = Executors.newFixedThreadPool(3);
List<Callable<String>> tasks = List.of(
    () -> { Thread.sleep(3000); return "Server A"; },
    () -> { Thread.sleep(1000); return "Server B"; },
    () -> { Thread.sleep(2000); return "Server C"; }
);

String result = executor.invokeAny(tasks);
System.out.println("First result = " + result);
executor.shutdown();
```

**Actual output:**

```
First result = Server B
```

**Server B wins deterministically** — it sleeps 1 s versus A's 3 s and C's 2 s. The result is the
*value* itself (`String`), not a `Future`: there is only one winner, so there is nothing to unwrap.

**Key points:**

- **The losers are cancelled.** As soon as one task completes, `invokeAny` interrupts the rest.
  Here the program finishes in ~1 second, not 3.
- **The real use case:** query redundant mirrors/replicas and take whichever answers first.
- **If every task fails**, `invokeAny` throws `ExecutionException`.
- **Only the *first successful* completion counts** — a task that throws does not win the race;
  `invokeAny` keeps waiting for a genuine result.

### 6.3 `TimeoutExample.java`

Bound how long you will wait, and cancel if exceeded.

```java
ExecutorService executor = Executors.newFixedThreadPool(2);

Future<String> future = executor.submit(() -> {
    Thread.sleep(5000);
    return "Completed";
});

try {
    String result = future.get(2, TimeUnit.SECONDS);   // ← wait at most 2s
    System.out.println(result);
} catch (TimeoutException e) {
    System.out.println("Task took too long");
    future.cancel(true);                               // ← true = interrupt if running
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
} catch (ExecutionException e) {
    System.out.println("Task failed: " + e.getCause());
}
executor.shutdown();
```

**Actual output:**

```
Task took too long
```

The task needs 5 seconds; we wait 2. `"Completed"` is never printed. The program exits after ~2
seconds rather than 5, because `cancel(true)` interrupts the sleeping worker — `Thread.sleep`
responds to interruption immediately by throwing `InterruptedException`.

**The three catch blocks map to the three things `get(timeout, unit)` can throw:**

| Exception | Meaning | Handling here |
| --- | --- | --- |
| `TimeoutException` | The wait elapsed; the task is still running | Report + `cancel(true)` |
| `InterruptedException` | *Our* waiting thread was interrupted | Restore the interrupt flag |
| `ExecutionException` | The task itself threw | Report `e.getCause()` — the real exception |

**Key points:**

- **`e.getCause()` is essential for `ExecutionException`.** The `ExecutionException` is only a
  wrapper; its message is near-useless. The actual failure is the cause.
- **`cancel(true)` vs `cancel(false)`:** `true` interrupts an already-running task; `false` only
  prevents it from starting if it is still queued. Interruption is cooperative — it works here
  because `Thread.sleep` is a blocking call that checks the flag. A task busy-looping on pure CPU
  without ever checking `Thread.interrupted()` **cannot be stopped this way**.
- **`get()` vs `get(timeout, unit)`:** the no-arg form waits forever. In production code, prefer
  the timeout form — an unbounded `get()` on a hung task hangs your application.

---

## 7. Concepts Cheat Sheet

### Pool types

| Factory | Threads | Queue | Use when |
| --- | --- | --- | --- |
| `newFixedThreadPool(n)` | exactly `n` | unbounded `LinkedBlockingQueue` | Default choice; bounded CPU/IO concurrency |
| `newCachedThreadPool()` | 0 → unbounded | `SynchronousQueue` (capacity 0) | Many short-lived tasks; ⚠️ can exhaust threads |
| `newSingleThreadExecutor()` | 1 | unbounded | Ordering matters; implicit mutual exclusion |
| `newScheduledThreadPool(n)` | `n` | delay queue | Delayed or recurring work |

### Submission APIs

| Call | Blocks? | Returns |
| --- | --- | --- |
| `submit(Runnable)` | no | `Future<?>` (result is `null`) |
| `submit(Callable<V>)` | no | `Future<V>` |
| `invokeAll(tasks)` | yes — until **all** finish | `List<Future<V>>`, in input order |
| `invokeAny(tasks)` | yes — until **one** succeeds | `V`, the winning value |
| `future.get()` | yes — indefinitely | `V` |
| `future.get(t, unit)` | yes — up to `t` | `V`, or throws `TimeoutException` |

### Shutdown

| Call | Effect |
| --- | --- |
| `shutdown()` | Graceful: reject new tasks, finish queued ones |
| `shutdownNow()` | Interrupt running tasks; return the never-run queued ones |
| `awaitTermination(t, unit)` | Block until done or timeout; returns `boolean` |

The robust shutdown idiom:

```java
executor.shutdown();
if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
    executor.shutdownNow();
}
```

### Recurring gotchas, all of which appear in this project

1. **Forgetting `shutdown()`** → pool threads are non-daemon, so the JVM never exits.
2. **Capturing a loop counter in a lambda** → must copy to an effectively-final local
   (`int taskId = i;`).
3. **Calling `get()` inside the submit loop** → serializes everything; the pool does nothing.
4. **Swallowing `InterruptedException`** → always restore the flag with
   `Thread.currentThread().interrupt()`.
5. **Reading `ExecutionException.getMessage()`** instead of `getCause()` → you lose the real error.
6. **Expecting completion order to equal submission order** → it does not; iterate the `Future`
   list in order if you need ordering restored.
7. **An uncaught exception in a periodic task** silently cancels all future executions.

---

## 8. Remaining Observations (not changed)

The task was to fix the bugs in `App.java`, so the following were deliberately **left alone**.
They are cosmetic or live in other files — listed here for visibility, not applied:

| File | Observation |
| --- | --- |
| `FileAnalyzer.java` | Constructor parameter is spelled `numberOfTheads` (missing `r`). |
| `FileAnalyzer.java` | If `run()` throws, `shutdown()` is never reached — the pool leaks and the JVM hangs. A `try/finally` around the body would fix it. |
| `FileAnalyzer.java` | The `boolean` from `awaitTermination` is discarded; a timeout passes unnoticed. The idiom in §7 handles it. |
| `FutureExamle.java` | Class/file name typo — should be `FutureExample`. Renaming requires renaming the file too. |
| `FixedThreadPoolExample.java` | Unused import `java.util.concurrent.Executor`. |
| `FileTask.java` | `words` is just `lines * 10`, so it carries no independent information. |
| `pom.xml` | JUnit 3.8.1 (released 2006) on a Java 21 project; JUnit 5 would be the modern choice. |
| `AppTest.java` | Generated placeholder — `assertTrue(true)` tests nothing. |

None of these block compilation or affect the outputs documented above.
