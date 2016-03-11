package no.ndla.auth.integration.providers

import com.typesafe.scalalogging.StrictLogging
import no.ndla.auth.AuthProperties
import no.ndla.auth.model.{KongKey, KongKeys}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._

import scalaj.http.{Http, HttpResponse}

trait KongServiceComponent {
  val kongService: KongService

  class KongService extends StrictLogging {

    val KONG_HOSTNAME = "kong"
    // In etc hosts when linking containers.
    val KONG_ADMIN_PORT = "8001"

    implicit val formats = DefaultFormats // Brings in default date formats etc.

    def deleteKeyForConsumer(appkey: String, consumerId: String): Unit = {
      getKeys(consumerId).find(_.key == appkey) match {
        case Some(key) => {
          val deleteKey: HttpResponse[String] = Http(s"http://$KONG_HOSTNAME:$KONG_ADMIN_PORT/consumers/$consumerId/key-auth/${key.id}").method("DELETE").asString
          if (deleteKey.isError) throw new RuntimeException(s"Could not log out consumer. Got error ${deleteKey.code}")
        }
      }
    }

    def getOrCreateKeyAndConsumer(username: String): KongKey = {
      // We can not use a valid uuid as username because of kong api /consumers/{username or id} where is uuid. So we prefix it
      val usernameWithPrefix = AuthProperties.KONG_USERNAME_PREFIX + username

      createConsumerIfNotExists(usernameWithPrefix)
      val keys: List[KongKey] = getKeys(usernameWithPrefix)

      keys.size match {
        case 0 => createKey(usernameWithPrefix)
        case _ => keys.head // Always return the first key.
      }
    }

    private def createConsumerIfNotExists(username: String): Unit = {
      val getConsumer: HttpResponse[String] = Http(s"http://$KONG_HOSTNAME:$KONG_ADMIN_PORT/consumers/$username").asString
      if (getConsumer.is2xx) return
      if (getConsumer.isCodeInRange(404, 404)) {
        createConsumer(username)
        return
      }
      if (getConsumer.isError) throw new RuntimeException(s"Checking consumer returned: ${getConsumer.code}")
    }

    private def createConsumer(id: String): Unit = {
      val response: HttpResponse[String] = Http(s"http://$KONG_HOSTNAME:$KONG_ADMIN_PORT/consumers/").postForm(Seq(
        "username" -> id
      )).asString

      if (response.isError) {
        throw new RuntimeException("Unable to create consumer: " + response.body)
      }
    }

    private def createKey(id: String): KongKey = {
      val response: HttpResponse[String] = Http(s"http://$KONG_HOSTNAME:$KONG_ADMIN_PORT/consumers/$id/key-auth").method("POST").asString

      if (response.isError) {
        throw new RuntimeException("Unable to create key: " + response.body)
      }
      parse(response.body).extract[KongKey]
    }

    private def getKeys(username: String): List[KongKey] = {
      val response: HttpResponse[String] = Http(s"http://$KONG_HOSTNAME:$KONG_ADMIN_PORT/consumers/$username/key-auth/").asString
      parse(response.body).extract[KongKeys].data
    }
  }

}