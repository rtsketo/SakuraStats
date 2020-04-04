package eu.rtsketo.sakurastats.control

object APIDevKey {
    /* Optionally a developer key from RoyaleAPI (link below)
     https://docs.royaleapi.com/#/authenticationid=generating-new-keys
     and a GitHub access token for users to be able to anonymously report an issue */
    val devKey = "Developer Key"
    val gitKey = System.getenv("GITHUB") ?: "GitHub Key"
}