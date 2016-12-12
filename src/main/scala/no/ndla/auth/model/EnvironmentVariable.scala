/*
 * Part of NDLA auth.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.auth.model

case class EnvironmentVariable(environmentVariables: Map[String, String], key: String) {
    def value = environmentVariables.get(key).get
}