package no.ndla.auth.service

import com.typesafe.scalalogging.StrictLogging
import no.ndla.auth._
import no.ndla.auth.exception._
import no.ndla.auth.repository.{UsersRepositoryComponent, StateRepositoryComponent}

import no.ndla.auth.model.{NdlaUser, GoogleAccessToken, GoogleUser}
import org.json4s._
import org.json4s.native.JsonMethods._

import scalaj.http.HttpResponse

trait GoogleAuthServiceComponent {
  this: UsersRepositoryComponent with StateRepositoryComponent =>

  class GoogleAuthService extends StrictLogging {
    implicit val formats = DefaultFormats // Brings in default date formats etc for Json

    // Required parameters for this provider.
    val requiredParameters = Array("code", "state")

    val configuration: Map[String, String] = AuthProperties.getWithPrefix("GOOGLE_").collect {
      case (key, Some(value)) => key -> value
    }

    // Parameter values below are used as query or post parameter names to Google.
    val CLIENT_ID = EnvironmentVariable("client_id", "Your ID used to identify your app when calling the OAuth2 provider.")
    val RESPONSE_TYPE = EnvironmentVariable("response_type", "The response type used in the oAuth protocol.")
    val SCOPE = EnvironmentVariable("scope", "A space separated list of scopes to request. For example 'email gender'")
    val REDIRECT_URI = EnvironmentVariable("redirect_uri", "The absolute address to redirect to verify the login")
    val GRANT_TYPE = EnvironmentVariable("grant_type", "The id of the app calling Google")
    val CODE = "code"
    val STATE = "state"

    // Parameters below are not used as query/post parameters.
    val ACCESS_TOKEN_VERIFICATION_URL = EnvironmentVariable("access_token_verification_url", "The absolute address to verify the oauth code")
    val LOGIN_URL = EnvironmentVariable("login_url", "The login url of the Oauth provider")
    val CLIENT_SECRET = EnvironmentVariable("client_secret", "The client secret shared with the oauth provider. Must be kept secret and not be used in client code.")
    val USER_INFO_URL = EnvironmentVariable("user_info_url", "The url used to get info about the authorized user.")

    def getRedirectUri(successUrl: String, failureUrl: String): String = {
      val requiredParameters = List(CLIENT_ID, RESPONSE_TYPE, SCOPE, REDIRECT_URI).map(_.key)
      val state = stateRepository.createState(successUrl, failureUrl).toString
      val parameters = configuration.filterKeys(requiredParameters.contains(_)) + (STATE -> state)
      configuration(LOGIN_URL.key) + "?" + toQueryStringFormat(parameters)
    }

    def getOrCreateNdlaUser(code: String, state: String): NdlaUser = {
      getUser(getAccessToken(code, state))
    }

    private def getAccessToken(code: String, state: String): GoogleAccessToken = {
      stateRepository.isStateValid(state) match {
        case false => throw new RuntimeException("Illegal State")
        case true => // Ok
      }

      val url: String = configuration.get(ACCESS_TOKEN_VERIFICATION_URL.key).get
      val requiredPostParameters = List(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, GRANT_TYPE).map(_.key)
      val postParameters: Seq[(String, String)] = (configuration.filterKeys(requiredPostParameters.contains(_)) + (CODE -> code)).toSeq
      val response: HttpResponse[String] = scalaj.http.Http(url).postForm(postParameters).asString

      if (response.isNotError)
        parse(response.body).extract[GoogleAccessToken]
      else
        throw AccessTokenVerificationException(s"An error occurred while verifying the access token. ${response.statusLine}: ${response.body}")
    }

    private def getUser(accessToken: GoogleAccessToken): NdlaUser = {
      val url: String = configuration.get(USER_INFO_URL.key).get
      val authorization = ("Authorization", s"Bearer ${accessToken.access_token}")
      val response = scalaj.http.Http(url).headers(authorization).asString

      if (response.isNotError) {
        val googleUser = parse(response.body).extract[GoogleUser]
        usersRepository.getOrCreateNdlaUser(googleUser)
      }
      else {
        throw AccessTokenVerificationException(s"An error occurred while getting user info. ${response.statusLine}: ${response.body}")
      }
    }
  }


}