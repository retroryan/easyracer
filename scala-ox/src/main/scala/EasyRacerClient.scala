import com.sun.management.OperatingSystemMXBean

import scala.concurrent.duration.*
import sttp.client3.*
import sttp.model.Uri
import ox.*
import sttp.model.Uri.QuerySegment

import java.lang.management.ManagementFactory
import java.security.MessageDigest
import java.util.UUID
import scala.annotation.tailrec
import scala.util.{Failure, Random, Success, Try}

object EasyRacerClient:
  private val backend = HttpClientSyncBackend()

  // Create a ForkLocal to store a request ID
  private val requestId = ForkLocal(UUID.randomUUID().toString)

  private def scenarioRequest(uri: Uri): Request[String, Any] =
    val id = requestId.get()
    println(s"[Request ID: $id] Sending request to: $uri")
    basicRequest
      .get(uri)
      .response(asStringAlways)
      .mapResponse { response =>
        println(s"[Request ID: $id] Received response from: $uri")
        response
      }


  /** Race 2 concurrent requests. and the winner is the first request to return a 200 response with a body containing right
   *
   *  A number of computations can be raced against each other using the race method.
   *  The losing computation is interrupted. race waits until both branches finish;
   *  this also applies to the losing one, which might take a while to clean up after interruption.
   *
   *  race returns the first result, or re-throws the last exception
   *
   */
  def scenario1(scenarioUrl: Int => Uri): String =
    val url = scenarioUrl(1)
    println(s"Calling scenario 1 with url: $url")

    def req(id: String) = requestId.supervisedWhere(id) {
      println(s"Starting request with ID: ${requestId.get()}")
      scenarioRequest(url).send(backend).body
    }

    supervised {
      val req1 = fork { req(s"scenario1-req1-${UUID.randomUUID()}") }
      val req2 = fork { req(s"scenario1-req2-${UUID.randomUUID()}") }
      race(req1.join(), req2.join())
    }

  /**
   *   Race 2 concurrent requests, where one produces a connection error
   *   The winner returns a 200 response with a body containing right
   *
   * @param scenarioUrl
   * @return
   */
  def scenario2(scenarioUrl: Int => Uri): String =
    val url = scenarioUrl(2)
    println(s"Calling scenario 2 with url: $url")

    def req = supervised {
      val id = s"scenario2-req-${UUID.randomUUID()}"
      requestId.supervisedWhere(id) {
        println(s"Starting request with ID: ${requestId.get()}")
        Try(scenarioRequest(url).send(backend)) match
          case Success(response) => response.body
          case Failure(error) =>
            println(s"[Request ID: ${requestId.get()}] Error: ${error.getMessage}")
            throw error
      }
    }

    supervised {
      race(req, req)
    }

  /**
   *  Race 10,000 concurrent requests
   *  The winner returns a 200 response with a body containing right
   *
   *
   * @param scenarioUrl
   * @return
   */
  def scenario3(scenarioUrl: Int => Uri): String =
    val url = scenarioUrl(3)
    println(s"Calling scenario 3 with url: $url")
    val reqs = Seq.fill(10000): () =>
      scenarioRequest(url).send(backend)
    race(reqs).body

  /**
   * Race 2 concurrent requests but 1 of them should have a 1 second timeout
   * The winner returns a 200 response with a body containing right
   *
   * @param scenarioUrl
   * @return
   */
  def scenario4(scenarioUrl: Int => Uri): String =
    val url = scenarioUrl(4)
    println(s"Calling scenario 4 with url: $url")
    def req = scenarioRequest(url).send(backend).body
    race(timeout(1.second)(req), req)

  /**
   * Race 2 concurrent requests where a non-200 response is a loser
   * The winner returns a 200 response with a body containing right
   *
   * @param scenarioUrl
   * @return
   */
  def scenario5(scenarioUrl: Int => Uri): String =
    val url = scenarioUrl(5)
    println(s"Calling scenario 5 with url: $url")
    def req = basicRequest.get(url).response(asString.getRight).send(backend).body
    race(req, req)

  /**
   * Race 3 concurrent requests where a non-200 response is a loser
   * The winner returns a 200 response with a body containing right
   *
   * @param scenarioUrl
   * @return
   */
  def scenario6(scenarioUrl: Int => Uri): String =
    val url = scenarioUrl(6)
    println(s"Calling scenario 6 with url: $url")
    def req = basicRequest.get(url).response(asString.getRight).send(backend).body
    race(req, req, req)

  /**
  * Start a request, wait at least 3 seconds then start a second request (hedging)
  * The winner returns a 200 response with a body containing right
  */
  def scenario7(scenarioUrl: Int => Uri): String =
    val url = scenarioUrl(7)
    println(s"Calling scenario 7 with url: $url")
    def req = scenarioRequest(url).send(backend).body
    def delayedReq =
      Thread.sleep(4000)
      req
    race(req, delayedReq)

  /**
   * Race 2 concurrent requests that "use" a resource which is obtained and released through other requests. 
   * The "use" request can return a non-20x request, in which case it is not a winner.
   *
   * GET /8?open
   * GET /8?use=<id obtained from open request>
   * GET /8?close=<id obtained from open request>
   * The winner returns a 200 response with a body containing right
   *
   * @param scenarioUrl
   * @return
   */
  def scenario8(scenarioUrl: Int => Uri): String =
    def req(url: Uri) = basicRequest.get(url).response(asString.getRight).send(backend).body

    val urlForScenario8 = scenarioUrl(8)
    println(s"Calling scenario 8 with url: $urlForScenario8")

    def open = req(uri"$urlForScenario8?open")
    def use(id: String) = req(uri"$urlForScenario8?use=$id")
    def close(id: String) = req(uri"$urlForScenario8?close=$id")

    def reqRes =
      val id = open
      try use(id)
      finally close(id)

    race(reqRes, reqRes)

//  def scenario8a(scenarioUrl: Int => Uri): String = supervised {
//    def req(url: Uri) = basicRequest.get(url).response(asString.getRight).send(backend).body
//
//    def open = req(uri"${scenarioUrl(8)}?open")
//
//    def use(id: String) = req(uri"${scenarioUrl(8)}?use=$id")
//
//    def close(id: String) = req(uri"${scenarioUrl(8)}?close=$id")
//
//    def reqRes = fork {
//      useInScope(open)(close) { id =>
//        use(id)
//      }
//    }
//
//    race(reqRes, reqRes)
//  }

  /**
   * Make 10 concurrent requests where 5 return a 200 response with a letter
   * When assembled in order of when they responded, form the "right" answer
   *
   * @param scenarioUrl
   * @return
   */
  def scenario9(scenarioUrl: Int => Uri): String =
    def req =
      val body = basicRequest.get(scenarioUrl(9)).response(asString.getRight).send(backend).body
      val now = System.nanoTime
      now -> body

    unsupervised:
      val forks = Seq.fill(10)(forkUnsupervised(req))
      forks
        .map(_.joinEither())
        .collect:
          case Right(v) => v
        .sortBy(_._1)
        .map(_._2)
        .mkString

  def scenario10(scenarioUrl: Int => Uri): String =
    val id = Random.nextString(8)

    def req(url: Uri) =
      basicRequest.get(url).response(asStringAlways).send(backend)

    val messageDigest = MessageDigest.getInstance("SHA-512")

    def blocking(): Unit =
      var result = Random.nextBytes(512)
      while (!Thread.interrupted())
        result = messageDigest.digest(result)

    def blocker =
      val url = scenarioUrl(10).addQuerySegment(QuerySegment.Plain(id))
      race(req(url), blocking())

    @tailrec
    def reporter: String =
      val osBean = ManagementFactory.getPlatformMXBean(classOf[OperatingSystemMXBean])
      val load = osBean.getProcessCpuLoad * osBean.getAvailableProcessors
      val resp = req(scenarioUrl(10).addQuerySegment(QuerySegment.KeyValue(id, load.toString)))
      if resp.code.isRedirect then
        Thread.sleep(1000)
        reporter
      else if resp.code.isSuccess then resp.body
      else throw Error(resp.body)

    val (_, result) = par(blocker, reporter)
    result

@main def run(): Unit =
  import EasyRacerClient.*
  def scenarioUrl(scenario: Int) = uri"http://localhost:8080/$scenario"
//  def scenarios = Seq(scenario1, scenario2, scenario3, scenario4, scenario5, scenario6, scenario7, scenario8, scenario9, scenario10)
  def scenarios = Seq(scenario1, scenario2, scenario4, scenario5, scenario6, scenario7, scenario8)

//  def scenarios: Seq[(Int => Uri) => String] = Seq(scenario8)
//def scenarios: Seq[(Int => Uri) => String] = Seq(scenario10)
  scenarios.foreach: s =>
    println(s(scenarioUrl))
