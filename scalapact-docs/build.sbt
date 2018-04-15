name := "scalapact-docs"

enablePlugins(ParadoxSitePlugin)
enablePlugins(GhpagesPlugin)

git.remoteRepo := "git@github.com:ITV/scala-pact.git"

sourceDirectory in Paradox := sourceDirectory.value / "main" / "paradox"
