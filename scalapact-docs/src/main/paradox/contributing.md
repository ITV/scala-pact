# How to contribute

All efforts to contribute to this project are welcome and encouraged. Scala-Pact can only be better with more eyes looking for problems and more minds trying to solve them.

If you would like to contribute, there are principally two ways you can help.

### Raising issues
If we don't know there is a problem, we can't help fix it.

Please feel free to raise issues if you can't get something to work, find a bug, or you have any ideas for improvements.

Try and be as specific as possible, ideally you should include steps to re-produce your issue if it's a bug, or some sort of rationale for new features.

### Code
We will always try to accept code submissions. If you'd like to discuss a change before you make it, please raise an issue and describe the work you're planning to do.

If you would like to improve the project by contributing code, please fork the repo, make your change and submit a pull request.

#### When your PR is accepted, please rebase rather than merge
Special note to people who have direct access to the main project repo. Please do not use github's magic merge button on your pull requests. Please rebase your work, for example (assuming you're on your branch):
```
git fetch -p
git pull --rebase origin master
git push origin HEAD:master
```

This should help us keep a linear history.

#### Testing Scala-Pact
Before submitting your pull request, you should do your best to check it all works!

The process has been codified into the `local-build-test.sh` shell script in the project root. This script builds, tests, and publishes everything to your local .ivy2 repo. Run from the project root directory using (takes about 5 minutes to complete):
`bash scripts/local-build-test.sh`

Here is the current testing procedure (in order):

##### All Projects
1. Make sure all the projects have a new SNAPSHOT version, that it is the same everywhere, and that they all use each other's new version i.e. Bump the core, bump the plugin and update the plugin to use the new core, bump the test project and update it to use the new core and the new plugin version.
2. Make sure you don't have that version in your `~/.ivy2/local` or `~/.ivy2/cache` folders.

##### Core Project
1. Run `sbt clean update compile`
1. Run `sbt test`
1. Run `sbt "+ publish-local"` to cross compile and publish to your local cache.

##### Plugin Project
1. Run `sbt clean update compile`
1. Run `sbt test`
1. Run `sbt publish-local` to publish to your local cache.

##### Test Project
1. Run `sbt clean update compile`
1. Run `sbt test`
1. Run `sbt pact-test`
1. Run `sbt pact-stubber`
1. In a new terminal (again from the test project directory), run `sbt pact-verify --source target/pacts`

##### Standalone stubber
No testing required at present.

**If everything is green, you're ready to submit your pull request!**
