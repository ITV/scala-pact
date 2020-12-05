# How to contribute

All efforts to contribute to this project are welcome and encouraged. Scala-Pact can only be better with more eyes looking for problems and more minds trying to solve them.

A full list of our wonderful contributors can be found in the @ref:[change log](change-log.md).

If you would like to contribute, there are principally two ways you can help.

### Raising issues

If we don't know there is a problem, we can't help fix it.

Please feel free to raise issues if you can't get something to work, find a bug, or you have any ideas for improvements.

Try and be as specific as possible, ideally you should include steps to re-produce your issue if it's a bug, or some sort of rationale for new features.

### Code

We will always try to accept code submissions. If you'd like to discuss a change before you make it, please raise an issue and describe the work you're planning to do.

If you would like to improve the project by contributing code, please fork the repo, make your change and submit a pull request.

### Code, Again!

We really want your help, but accepting PR's is challenging.
For maximum success:
- Talk to us first!
- Keep it small!
- Do the minimum!

The most important point of the fix is to convey what change is really needed, so aim to solve the problem with the least changes possible. The code can always be improved / cleaned up as a secondary step.

#### When your PR is accepted, please rebase rather than merge

Special note to people who have direct access to the main project repo. Please do not use github's magic merge button on your pull requests without first making sure it's in rebase mode.

Please rebase your work, for example (assuming you are on your branch):
```
git fetch -p
git pull --rebase origin master
git push origin HEAD:master
```

This should help us keep a clean linear history.

#### Code formatting
We now use Scalafmt to format the code base, the main reason is practical, in that it reduces the diffs in code submissions.

Please install Scalafmt and run it before submitting your code review. Your build will fail if you have not done this!

#### Testing Scala-Pact

Before submitting your pull request, you should do your best to check it all works!

There is a special sbt project specific task called `sbt quicktest` (there is also `quickcompile`). This command only tests one thread of dependencies and only at scala version 2.12.x, but that's usually enough to discover anything serious.

Once you think you're getting close, you need a full build and test...

The process has been codified into the `local-build-test.sh` shell script in the project root. This script builds, tests, and publishes everything to your local .ivy2 repo. Run from the project root directory using `bash scripts/local-build-test.sh`. Takes about **10 minutes** to complete.

**If everything is green, you're ready to submit your pull request!**
