## Structured Concurrency: Managing the Hierarchical Cancelation and Error Handling

#### Ryan Knight
*Developer @ <a href="https://www.grandcloud.com//" class="twitter-follow-button" data-size="large">Grand Cloud</a>*
*Cloud Architect @ <a href="https://audioenhancement.com/" class="twitter-follow-button" data-size="large">Audio Enhancement</a>*
*Park City, UT*
*Mountain Biking, Surfing, Backcountry Skiing, Kettlebells, Longevity*
*<a href="https://twitter.com/knight_cloud" class="twitter-follow-button" data-size="large">@knight_cloud</a><script async src="https://platform.twitter.com/widgets.js" charset="utf-8"></script>*

---

<!-- ### Park City in the Fall

<div style="display: flex;">
<img src="fall leaves.jpg" alt="Park City" width="500" height="600">
<img src="sunrise.jpg" alt="Park City" width="500" height="600">
</div>

--- -->

<!-- ## Challenges with Traditional Concurrency

* Shared Mutable State
* Mutexes and Locks - Synchronization primitives
* Non-determinism - Race Conditions 
* Scalability Concerns - Threads competing for shared resources
--- -->

<!--
Why concurrency is / has been hard (shared mutable state, mutexes, etc)
 * Shared Mutable State
 * Mutexes and Locks - Synchronization primitives to protect shared resources
 * Non-determinism - Race conditions may only occur under specific timing conditions
 * Scalability Concerns - Contention: Threads competing for shared resources
                        - Lock granularity trade-offs: Fine-grained vs. coarse-grained locking    
 *  Complexity in Design and Implementation
-->

## Common Concurrency Paradigms

* Asynchronous/Event-Driven
* Actors
* Futures / Promises
* Coroutines (e.g., Goroutines with Channels)
* Challenges in Concurrent Programming
  * Complex and hard to read code
  * Callback Hell
  * Difficult resource management

---

## Challenges of Goroutines

* Go Considered Harmful - [Go statement considered harmful](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/)
* Often resemble "goto" statements in terms of control flow, making it difficult to maintain a strict structure in concurrent operations.
* Non-local exits leaving resources uncleaned or operations unfinished
* Implicit dependencies - channels create implicit dependencies between goroutines
* Manual Cancellation and Cleanup
* Error Propagation Challenges
* Leads to code that is hard to read and reason about.
* Prone to unexpected behavior and errors

---

## Goals of Structure Concurrency:

* Concurrent operations are scoped and hierarchical in a tree-like structure
* Parent operations wait for child operations to complete
* Cancellation e.g. Races (loser cancellation)
* Automatic Resource management
* Efficient thread utilization (non-blocking)
* Explicit timeouts
* Automatic propagation of cancellation and errors

<!--
 * Cancelling a parent task automatically cancels all its children - Tree pruning
 * Resource management: Resources can be allocated at a parent level and automatically shared with child tasks
 * Resource Management - tying resource lifetimes to the scope of concurrent operations
 * Efficient thread utilization - structured concurrency, especially when implemented with coroutines or similar constructs, allows for efficient use of threads. it enables writing seemingly sequential code that is actually non-blocking and reactive.
 * Semantic Errors - more meaningful error scenarios. errors in child tasks can be propagated up the hierarchy, maintaining context
 * Error Propigation: Errors in child tasks can be efficiently communicated up the hierarchy

Hierarchical Concurrency (diagram)  - hierarchical concurrency organizes concurrent tasks into a tree-like structure
-->
---

## Approaches to Structured Concurrency

* Scoped Driven
  - Java Loom
* Direct Style (Imperative / Monad free!)
  - Scala Ox
    - Built on Loom, JDK21+ only
  - Rust (Future based syntax)
* Effect Oriented
  - Scala ZIO
    - Monadic Effect
  - Scala Kyo
    - Algebraic Effects / single monad

---



## Project Loom Building Blocks - Continuations and Schedulers

* Continuations
  * Program object representing a computation that may be suspended and resumed
  * Provide the mechanism for capturing and restoring execution state
  * Low level - not used by developer
* New Schedulers designed for virtual threads - auto adapts to the number of available CPU cores
* Building blocks to allow JVM scheduler to manage start, suspend, resume, etc. of operations

---

## Project Loom Building Blocks - Virtual Threads

* Extremely lightweight (can create millions)
* Efficiently scheduled by the JVM and not the OS
* Transparent to the developer
* Multiple lightweight threads scheduled on the same OS thread.

---

## Structured Concurrency w/ Project Loom

* Transparent Blocking - Virtual threads can block without blocking the underlying OS thread
* Automatic Scheduling and Resource Management
* Scope-Based Lifetime Management
* Predictable Error Propagation
* Cancellation Hierarchies

---

## Direct Style Concurrency w/ Project Loom

* Sequential Appearance, Concurrent Execution
* Sequential code structure that uses virtual threads for concurrent execution 
* Readable control flow 
* Simplified error handling
* Async runtime inside the JVM

---


## Overview of Direct Style Concurrency with Scala Ox

* Built on Project Loom's virtual threads
* Leverages the JVM's async runtime for efficient, lightweight and safe concurrency. 
* What used to require specialized libraries can now be built direclty JVM.
* The structure of your code defines the structure of concurrent execution
* Scope-based lifetime management ensures that a scope completes only when all its threads finish (successfully or with errors)
* Prevents accidental thread leakage and simplifies resource management
* Threads are implementation detail and not an effect
* Still Experimental

---

## Easy Racer

[github.com/jamesward/easyracer](https://github.com/jamesward/easyracer)

> Ten Structured Concurrency "obstacle courses"

|                                                                                      |                                                                                           |                                                                                           |
|--------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| [Scala 3 + ZIO](https://github.com/jamesward/easyracer/tree/main/scala-zio)          | [Kotlin + Coroutines](https://github.com/jamesward/easyracer/tree/main/kotlin-coroutines) | [OCaml + Lwt + Cohttp](https://github.com/jamesward/easyracer/tree/main/ocaml-cohttp-lwt) |
| [Scala 3 + Ox](https://github.com/jamesward/easyracer/tree/main/scala-ox)            | [Kotlin + Splitties](https://github.com/jamesward/easyracer/tree/main/kotlin-splitties)   | [OCaml + Eio + Cohttp](https://github.com/jamesward/easyracer/tree/main/ocaml-cohttp-eio) |
| [Scala 3 + Kyo](https://github.com/jamesward/easyracer/tree/main/scala-kyo)          | [Kotlin + Arrow](https://github.com/jamesward/easyracer/tree/main/kotlin-arrow)           | Python (Various)                                                                          |
| [Scala + Cats Effects 3](https://github.com/jamesward/easyracer/tree/main/scala-ce3) | [Rust + Tokio](https://github.com/jamesward/easyracer/tree/main/rust-tokio)               | [C#](https://github.com/jamesward/easyracer/tree/main/dotnet)                             |
| [Java + Loom](https://github.com/jamesward/easyracer/tree/main/java-loom)            | [Go](https://github.com/jamesward/easyracer/tree/main/go-stdlib)                          | [Elm](https://github.com/jamesward/easyracer/tree/main/elm-worker)                        |

---





## Scenario 1

### Race 2 concurrent requests


* First request to complete is thhe one that wins
* What is a race?
    * Do multiple things at the same time, get the first result
* Loser cancellation (but not validated in this scenario)
    * Cancellation means stopping and cleaning up

---

## Scenario 1 - go standard lib

@[code lang=scala transclude={43-63}](@/../go-stdlib/main.go)

<!--

-->

---


## Scenario 1 - Java Loom

@[code lang=java transclude={35-43}](@/../java-loom/src/main/java/Main.java)

<!--

-->

---



## Scenario 1 - Scala Ox

@[code lang=scala transclude={19-22}](@/../scala-ox/src/main/scala/EasyRacerClient.scala)

<!--

-->

---


## Scenario 1 - Kotlin

@[code lang=scala transclude={24-35}](@/../kotlin-coroutines/src/main/kotlin/Main.kt)

<!--

-->

---


## Scenario 1 - Comparisson

* Ox
    * non effect oriented
    * race isn’t on a datatype
    * def instead of val
    * Loom
* Java
    * Scopes to define SC
        * ShutdownOnSuccess is the race
    * Direct Loom usage
        * client.send is blocking but not really
* Kotlin
    * Also Scope Based
    * But explicit cancellation of loser
---

## Scoped Values

* JEP 429: Scoped Values
* Thread Local done right
* Efficient sharing of immutable data within and across threads in the same scope
* Automatic Cleanup

---

## Introducing Java Scoped Values

@[code lang=java transclude={35-52}](@/../java-loom-expanded/src/main/java/ScopedValuesScenarios.java)

---

## Scenario 1 - Java Loom w/ Scoped Values

@[code lang=java transclude={63-81}](@/../java-loom-expanded/src/main/java/ScopedValuesScenarios.java)

---

## Scala Ox ForkLocal

@[code lang=java transclude={18-30}](@/../scala-ox-expanded/src/main/scala/EasyRacerClient.scala)

---

## Scenario 1 - Scala Ox w/ ForkLocal

@[code lang=java transclude={39-54}](@/../scala-ox-expanded/src/main/scala/EasyRacerClient.scala)

---

## Scenario 2

### Race 2 concurrent requests, where one produces a connection error

<!--
* An error loser does not win or cancel the race
-->

---

## Scenario 2 - Java Loom

@[code lang=java transclude={46-54}](@/../java-loom/src/main/java/Main.java)

<!--

-->

---

## Scenario 2 - Scala Ox

@[code lang=scala transclude={24-27}](@/../scala-ox/src/main/scala/EasyRacerClient.scala)

<!--

-->

---

## Scenario 2 - Kotlin

@[code lang=scala transclude={38-59}](@/../kotlin-coroutines/src/main/kotlin/Main.kt)

<!--

-->

---


## Scenario 4

### Race 2 concurrent requests but 1 of them should have a 1 second timeout

<!--
* Talking points
    * Validating that a connection is open for 1 second, then closed
    * Timeout’d racer doesn’t fail the race
    * Timeout shouldn’t block the main thread
    * Timeout with SC is generally implemented with a race
* Java
    * The timeout is a race within the request race
-->

---

## Scenario 4 - Java Loom

@[code lang=java transclude={73-90}](@/../java-loom/src/main/java/Main.java)

<!--

-->

---

## Scenario 4 - Scala Ox

@[code lang=scala transclude={35-38}](@/../scala-ox/src/main/scala/EasyRacerClient.scala)

<!--

-->

---

## Scenario 5

### Race 2 concurrent requests where a non-200 response is a loser

<!--
* Modifying the task based on the value it produces
* Different HTTP clients handle response codes differently and some mapping of non-2xx to fail the request is sometimes necessary
-->

---

## Scenario 5 - Java Loom

@[code lang=java transclude={93-113}](@/../java-loom/src/main/java/Main.java)

<!--

-->

---

## Scenario 5 - Scala Ox

@[code lang=scala transclude={40-43}](@/../scala-ox/src/main/scala/EasyRacerClient.scala)

<!--

-->

---

## Scenario 5 - Kotlin

@[code lang=scala transclude={38-59}](@/../kotlin-coroutines/src/main/kotlin/Main.kt)

<!--

-->

---

## Scenario 7

### Start a request, wait at least 3 seconds then start a second request (hedging)

<!--
* Hedging is a common use case for race
* why & example of hedging. P99
* Different approaches to a “delay” and like timeout, it shouldn’t block the main thread
-->

---

## Scenario 7 - Java Loom

@[code lang=java transclude={140-151}](@/../java-loom/src/main/java/Main.java)

<!--

-->

---

## Scenario 7 - Java Loom Expanded

@[code lang=java transclude={230-267}](@/../java-loom-expanded/src/main/java/ScopedValuesScenarios.java)

<!--

-->

---

## Scenario 7 - Scala Ox

@[code lang=scala transclude={50-56}](@/../scala-ox/src/main/scala/EasyRacerClient.scala)

<!--

-->

---

## Scenario 7 - Kotlin

@[code lang=scala transclude={132-144}](@/../kotlin-coroutines/src/main/kotlin/Main.kt)

<!--

-->

---

## Scenario 8

### Race 2 concurrent requests that "use" a resource which is obtained and released through other requests. The "use" request can return a non-20x request, in which case it is not a winner.

<!--
* Resource management - how hard is it to be sure open resources get closed with success & failures
* Effect systems make resources management + concurrency easy
* Java
    * ???
* Ox
    * unsupervised & forkPlain
-->

---
## Scenario 8 - Scala Ox

@[code lang=scala transclude={58-70}](@/../scala-ox/src/main/scala/EasyRacerClient.scala)

<!--

-->

---

## Scenario 8 - Java Loom

@[code lang=java transclude={154-200}](@/../java-loom/src/main/java/Main.java)

<!--

-->

---

## Scenario 8 - Kotlin

@[code lang=scala transclude={146-165}](@/../kotlin-coroutines/src/main/kotlin/Main.kt)

<!--

-->

---

## ScopedValue


<!--
TODO
-->

---
