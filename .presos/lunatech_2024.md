## Structured Concurrency: Managing the Hierarchical Cancelation and Error Handling

#### Ryan Knight
*Developer @ <a href="https://www.grandcloud.com//" class="twitter-follow-button" data-size="large">Grand Cloud</a>*
*Cloud Architect @ <a href="https://audioenhancement.com/" class="twitter-follow-button" data-size="large">Audio Enhancement</a>*
<a href="https://twitter.com/knight_cloud" class="twitter-follow-button" data-size="large">@knight_cloud</a><script async src="https://platform.twitter.com/widgets.js" charset="utf-8"></script>


---

## Why do we need Structured Concurrency?

<!--
Why concurrency is / has been hard (shared mutable state, mutexes, etc)
 * Shared Mutable State
 * Mutexes and Locks - Synchronization primitives to protect shared resources
 * Non-determinism - Race conditions may only occur under specific timing conditions
 * Scalability Concerns - Contention: Threads competing for shared resources
                        - Lock granularity trade-offs: Fine-grained vs. coarse-grained locking    
 *  Complexity in Design and Implementation


Hierarchical Concurrency (diagram)  - hierarchical concurrency organizes concurrent tasks into a tree-like structure
-->

---

## Generally Supports:

* Cancellation e.g. Races (loser cancellation)
* Resource management
* Efficient thread utilization (i.e. reactive, non-blocking)
* Explicit timeouts
* Semantic Errors and Error Propigation

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

## Approaches to Structured Concurrency

* Effect Oriented
  - Scala ZIO
    - Monadic Effect
  - Scala Kyo
    - Algebraic Effects / single monad
* Direct Style (Imperative / Monad free!)
  - Scala Ox
    - Built on Loom, JDK21+ only
  - Rust (Future based syntax)
* Scoped Driven
  - Java Loom

---

## Scenario 1

### Race 2 concurrent requests

<!--
* First one wins
* What is a race?
    * Do multiple things at the same time, get the first result
* Loser cancellation (but not validated in this scenario)
    * Cancellation means stopping and cleaning up

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
-->

---

## Scenario 1 - Java Fork/Join

@[code lang=scala transclude={6-52}](@/../java-fork-join/EasyRacerClient.java)

<!--

-->

---

## Scenario 1 - Problems with Java Fork/Join

* Lack of proper cancellation - no mechanismto cancel the loosing task
* Inefficent thread utilizaiton
* Potential errors include the possibility of both tasks failing silently, returning null, and the main thread interpreting this as a successful race with no winner

---


## Scenario 1 - Scala Ox

@[code lang=scala transclude={19-22}](@/../scala-ox/src/main/scala/EasyRacerClient.scala)

<!--

-->

---

## Scenario 1 - Java Loom

@[code lang=java transclude={35-43}](@/../java-loom/src/main/java/Main.java)

<!--

-->

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

## Scenario 3

### Race 10,000 concurrent requests

<!--
* 10000 concurrent requires efficient resource utilization
-->

---

## Scenario 3 - Java Loom

@[code lang=java transclude={57-70}](@/../java-loom/src/main/java/Main.java)

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

## ScopedValue


<!--
TODO
-->

---
