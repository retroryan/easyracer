import com.sun.management.OperatingSystemMXBean

import scala.concurrent.duration.*
import sttp.client3.*
import sttp.model.Uri
import ox.*
import sttp.model.Uri.QuerySegment

import java.lang.management.ManagementFactory
import java.security.MessageDigest
import scala.annotation.tailrec
import scala.util.Random

object EasyRacerClient:
  private val backend = HttpClientSyncBackend()
  private def scenarioRequest(uri: Uri): Request[String, Any] = basicRequest.get(uri).response(asStringAlways)

  /** A number of computations can be raced against each other using the race method.
   *  The losing computation is interrupted. race waits until both branches finish; 
   *  this also applies to the losing one, which might take a while to clean up after interruption.
   *  
   *  race returns the first result, or re-throws the last exception
   * 
   */
  def scenario1(scenarioUrl: Int => Uri): String =
    val url = scenarioUrl(1)
    println(s"Calling scenario 1 with url: $url")
    def req = scenarioRequest(url).send(backend).body
    race(req, req)

  def scenario2(scenarioUrl: Int => Uri): String =
    val url = scenarioUrl(2)
    println(s"Calling scenario 2 with url: $url")
    def req = scenarioRequest(url).send(backend)
    race(req, req).body

  def scenario3(scenarioUrl: Int => Uri): String =
    val url = scenarioUrl(3)
    println(s"Calling scenario 3 with url: $url")
    val reqs = Seq.fill(10000): () =>
      scenarioRequest(url).send(backend)
    race(reqs).body

  def scenario4(scenarioUrl: Int => Uri): String =
    val url = scenarioUrl(4)
    println(s"Calling scenario 4 with url: $url")
    def req = scenarioRequest(url).send(backend).body
    race(timeout(1.second)(req), req)

  def scenario5(scenarioUrl: Int => Uri): String =
    val url = scenarioUrl(5)
    println(s"Calling scenario 5 with url: $url")
    def req = basicRequest.get(url).response(asString.getRight).send(backend).body
    race(req, req)

  def scenario6(scenarioUrl: Int => Uri): String =
    val url = scenarioUrl(6)
    println(s"Calling scenario 6 with url: $url")
    def req = basicRequest.get(url).response(asString.getRight).send(backend).body
    race(req, req, req)

  def scenario7(scenarioUrl: Int => Uri): String =
    val url = scenarioUrl(7)
    println(s"Calling scenario 7 with url: $url")
    def req = scenarioRequest(url).send(backend).body
    def delayedReq =
      Thread.sleep(4000)
      req
    race(req, delayedReq)

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
