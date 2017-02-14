/*
 * Part of NDLA auth.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.auth.model

import UserType._

case class FacebookUser(id: String,
                        ndla_id: Option[String],
                        name: Option[String],
                        first_name: Option[String],
                        middle_name: Option[String],
                        last_name: Option[String],
                        email: Option[String],
                        verified: Boolean,
                        userType: UserType.Value = UserType.FACEBOOK
                       ) extends ExternalUser