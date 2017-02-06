/*
 * Part of NDLA auth.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.auth

import no.ndla.auth.controller.{AuthController, HealthController, TokenController}
import org.postgresql.ds.PGPoolingDataSource
import no.ndla.auth.integration.{DataSourceComponent, KongServiceComponent}
import no.ndla.auth.repository.StateRepositoryComponent
import no.ndla.auth.repository.UsersRepositoryComponent
import no.ndla.auth.integration.providers.{FacebookAuthServiceComponent, GoogleAuthServiceComponent, TwitterAuthServiceComponent}
import no.ndla.auth.service.{Clock, TokenService}

object ComponentRegistry
  extends DataSourceComponent
  with UsersRepositoryComponent
  with StateRepositoryComponent
  with FacebookAuthServiceComponent
  with GoogleAuthServiceComponent
  with TwitterAuthServiceComponent
  with KongServiceComponent
  with AuthController
  with HealthController
  with TokenController
  with TokenService
  with Clock
{
  implicit val swagger = new AuthSwagger

  val dataSource = new PGPoolingDataSource()
  dataSource.setUser(AuthProperties.MetaUserName)
  dataSource.setPassword(AuthProperties.MetaPassword)
  dataSource.setDatabaseName(AuthProperties.MetaResource)
  dataSource.setServerName(AuthProperties.MetaServer)
  dataSource.setPortNumber(AuthProperties.MetaPort)
  dataSource.setInitialConnections(AuthProperties.MetaInitialConnections)
  dataSource.setMaxConnections(AuthProperties.MetaMaxConnections)
  dataSource.setCurrentSchema(AuthProperties.MetaSchema)

  lazy val usersRepository = new UsersRepository
  lazy val stateRepository = new StateRepository
  lazy val facebookAuthService = new FacebookAuthService
  lazy val googleAuthService = new GoogleAuthService
  lazy val twitterAuthService = new TwitterAuthService
  lazy val kongService = new KongService
  lazy val resourcesApp = new ResourcesApp
  lazy val authController = new AuthController
  lazy val healthController = new HealthController
  lazy val tokenController = new TokenController
  lazy val tokenService = new TokenService
  lazy val clock = new SystemClock
}
