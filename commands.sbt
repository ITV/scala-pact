addCommandAlias(
  "prepareScalaPactPublish",
  List(
    "+clean",
    "+update",
    "+compile",
    "+test",
    "+publishSigned"
  ).mkString(";", ";", "")
)

addCommandAlias(
  "localPublishScalaPact",
  List(
    "scalafmtCheckAll",
    "+clean",
    "+update",
    "+compile",
    "+test",
    "+publishLocal"
  ).mkString(";", ";", "")
)
