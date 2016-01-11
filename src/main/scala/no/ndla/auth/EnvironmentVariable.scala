package no.ndla.auth

case class EnvironmentVariable(key: String, description: String) {
    def value(implicit environmentVariables: Map[String, String]) = environmentVariables.get(key).get
}
