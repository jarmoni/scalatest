/*
 * Copyright 2001-2013 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalatest.fixture

import org.scalatest._
import words.{CanVerb, ResultOfAfterWordApplication, ShouldVerb, BehaveWord, MustVerb,
StringVerbBlockRegistration}
import scala.collection.immutable.ListSet
import org.scalatest.exceptions.StackDepthExceptionHelper.getStackDepth
import java.util.concurrent.atomic.AtomicReference
import java.util.ConcurrentModificationException
import org.scalatest.events._
import org.scalatest.Suite.anExceptionThatShouldCauseAnAbort
import org.scalatest.Suite.autoTagClassAnnotations
import scala.concurrent.Future

/**
 * Implementation trait for class <code>fixture.AsyncFreeSpec</code>, which is
 * a sister class to <code>org.scalatest.AsyncFreeSpec</code> that can pass a
 * fixture object into its tests.
 *
 * <p>
 * <a href="AsyncFreeSpec.html"><code>fixture.AsyncFreeSpec</code></a> is a class,
 * not a trait, to minimize compile time given there is a slight compiler
 * overhead to mixing in traits compared to extending classes. If you need
 * to mix the behavior of <code>fixture.AsyncFreeSpec</code> into some other
 * class, you can use this trait instead, because class
 * <code>fixture.AsyncFreeSpec</code> does nothing more than extend this trait and add a nice <code>toString</code> implementation.
 * </p>
 *
 * <p>
 * See the documentation of the class for a <a href="AsyncFreeSpec.html">detailed
 * overview of <code>fixture.AsyncFreeSpec</code></a>.
 * </p>
 *
 * @author Bill Venners
 */
//SCALATESTJS-ONLY @scala.scalajs.js.annotation.JSExportDescendentClasses(ignoreInvalidDescendants = true)
@Finders(Array("org.scalatest.finders.FreeSpecFinder"))
trait AsyncFreeSpecLike extends AsyncSuite with AsyncTestRegistration { thisSuite =>

  override private[scalatest] def transformToOutcome(testFun: FixtureParam => Future[Assertion]): FixtureParam => AsyncOutcome =
    (fixture: FixtureParam) => {
      val futureUnit = testFun(fixture)
      FutureOutcome(
        futureUnit.map(u => Succeeded).recover {
          case ex: exceptions.TestCanceledException => Canceled(ex)
          case _: exceptions.TestPendingException => Pending
          case tfe: exceptions.TestFailedException => Failed(tfe)
          case ex: Throwable if !Suite.anExceptionThatShouldCauseAnAbort(ex) => Failed(ex)
        }
      )
    }

  private final val engine = new AsyncFixtureEngine[FixtureParam](Resources.concurrentFixtureFreeSpecMod, "FixtureFreeSpec")

  import engine._

  private[scalatest] val sourceFileName = "FreeSpecRegistering.scala"

  final def registerAsyncTest(testText: String, testTags: Tag*)(testFun: FixtureParam => Future[Assertion]) {
    // SKIP-SCALATESTJS-START
    val stackDepthAdjustment = -2
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepthAdjustment = -5
    engine.registerAsyncTest(testText, transformToOutcome(testFun), Resources.testCannotBeNestedInsideAnotherTest, "FreeSpecRegistering.scala", "registerAsyncTest", 5, stackDepthAdjustment, None, None, testTags: _*)
  }

  final def registerIgnoredAsyncTest(testText: String, testTags: Tag*)(testFun: FixtureParam => Future[Assertion]) {
    // SKIP-SCALATESTJS-START
    val stackDepthAdjustment = -3
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepthAdjustment = -5
    engine.registerIgnoredAsyncTest(testText, transformToOutcome(testFun), Resources.testCannotBeNestedInsideAnotherTest, "FreeSpecRegistering.scala", "registerIgnoredAsyncTest", 4, stackDepthAdjustment, None, testTags: _*)
  }

  /**
   * Register a test with the given spec text, optional tags, and test function value that takes no arguments.
   * An invocation of this method is called an &ldquo;example.&rdquo;
   *
   * This method will register the test for later execution via an invocation of one of the <code>execute</code>
   * methods. The name of the test will be a concatenation of the text of all surrounding describers,
   * from outside in, and the passed spec text, with one space placed between each item. (See the documenation
   * for <code>testNames</code> for an example.) The resulting test name must not have been registered previously on
   * this <code>FreeSpec</code> instance.
   *
   * @param specText the specification text, which will be combined with the descText of any surrounding describers
   * to form the test name
   * @param testTags the optional list of tags for this test
   * @param methodName caller's method name
   * @param testFun the test function
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws TestRegistrationClosedException if invoked after <code>run</code> has been invoked on this suite
   * @throws NullArgumentException if <code>specText</code> or any passed test tag is <code>null</code>
   */
  private def registerAsyncTestToRun(specText: String, testTags: List[Tag], methodName: String, testFun: FixtureParam => Future[Assertion]) {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -3
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -6
    engine.registerAsyncTest(specText, transformToOutcome(testFun), Resources.inCannotAppearInsideAnotherIn, sourceFileName, methodName, stackDepth, stackDepthAdjustment, None, None, testTags: _*)
  }

  private def registerPendingTestToRun(specText: String, testTags: List[Tag], methodName: String, testFun: FixtureParam => PendingStatement) {
    engine.registerAsyncTest(specText, OldTransformer(testFun), Resources.inCannotAppearInsideAnotherIn, sourceFileName, methodName, 4, -3, None, None, testTags: _*)
  }

  /**
   * Register a test to ignore, which has the given spec text, optional tags, and test function value that takes no arguments.
   * This method will register the test for later ignoring via an invocation of one of the <code>execute</code>
   * methods. This method exists to make it easy to ignore an existing test by changing the call to <code>it</code>
   * to <code>ignore</code> without deleting or commenting out the actual test code. The test will not be executed, but a
   * report will be sent that indicates the test was ignored. The name of the test will be a concatenation of the text of all surrounding describers,
   * from outside in, and the passed spec text, with one space placed between each item. (See the documenation
   * for <code>testNames</code> for an example.) The resulting test name must not have been registered previously on
   * this <code>FreeSpec</code> instance.
   *
   * @param specText the specification text, which will be combined with the descText of any surrounding describers
   * to form the test name
   * @param testTags the optional list of tags for this test
   * @param methodName caller's method name
   * @param testFun the test function
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws TestRegistrationClosedException if invoked after <code>run</code> has been invoked on this suite
   * @throws NullArgumentException if <code>specText</code> or any passed test tag is <code>null</code>
   */
  private def registerAsyncTestToIgnore(specText: String, testTags: List[Tag], methodName: String, testFun: FixtureParam => Future[Assertion]) {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -4
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -7
    engine.registerIgnoredAsyncTest(specText, transformToOutcome(testFun), Resources.ignoreCannotAppearInsideAnIn, sourceFileName, methodName, stackDepth, stackDepthAdjustment, None, testTags: _*)
  }

  private def registerPendingTestToIgnore(specText: String, testTags: List[Tag], methodName: String, testFun: FixtureParam => PendingStatement) {
    engine.registerIgnoredAsyncTest(specText, OldTransformer(testFun), Resources.ignoreCannotAppearInsideAnIn, sourceFileName, methodName, 4, -4, None, testTags: _*)
  }
  /*
 private def registerBranch(description: String, childPrefix: Option[String], fun: () => Unit) {

   // TODO: Fix the resource name and method name
   registerNestedBranch(description, childPrefix, fun(), "describeCannotAppearInsideAnIt", sourceFileName, "describe")
 }  */

  /**
   * Class that supports the registration of tagged tests.
   *
   * <p>
   * Instances of this class are returned by the <code>taggedAs</code> method of
   * class <code>FreeSpecStringWrapper</code>.
   * </p>
   *
   * @param specText the specification text
   * @param tags the list of tags
   *
   * @author Bill Venners
   * @author Chee Seng
   */
  protected final class ResultOfTaggedAsInvocationOnString(specText: String, tags: List[Tag]) {

    /**
     * Supports tagged test registration.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" taggedAs(SlowTest) in { fixture => ... }
     *                                       ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     *
     * @param testFun the test function
     */
    def in(testFun: FixtureParam => Future[Assertion]) {
      registerAsyncTestToRun(specText, tags, "in", testFun)
    }

    /**
     * Supports tagged test registration, for tests that don't take a fixture.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" taggedAs(SlowTest) in { () => ... }
     *                                       ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     *
     * @param testFun the test function
     */
    def in(testFun: () => Future[Assertion]) {
      registerAsyncTestToRun(specText, tags, "in", new NoArgTestWrapper(testFun))
    }

    /**
     * Supports registration of tagged, pending tests.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" taggedAs(SlowTest) is (pending)
     *                                       ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     *
     * @param testFun the test function
     */
    def is(testFun: => PendingStatement) {
      registerPendingTestToRun(specText, tags, "is", unusedFixtureParam => testFun)
    }

    /**
     * Supports registration of tagged, ignored tests.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" taggedAs(SlowTest) ignore { fixture => ... }
     *                                       ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     *
     * @param testFun the test function
     */
    def ignore(testFun: FixtureParam => Future[Assertion]) {
      registerAsyncTestToIgnore(specText, tags, "ignore", testFun)
    }

    /**
     * Supports registration of tagged, ignored tests that take no fixture parameter.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" taggedAs(SlowTest) ignore { () => ... }
     *                                       ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     *
     * @param testFun the test function
     */
    def ignore(testFun: () => Future[Assertion]) {
      registerAsyncTestToIgnore(specText, tags, "ignore", new NoArgTestWrapper(testFun))
    }
  }

  /**
   * A class that via an implicit conversion (named <code>convertToFreeSpecStringWrapper</code>) enables
   * methods <code>when</code>, <code>that</code>, <code>in</code>, <code>is</code>, <code>taggedAs</code>
   * and <code>ignore</code> to be invoked on <code>String</code>s.
   *
   * <p>
   * This class provides much of the syntax for <code>fixture.FreeSpec</code>, however, it does not add
   * the verb methods (<code>should</code>, <code>must</code>, and <code>can</code>) to <code>String</code>.
   * Instead, these are added via the <code>ShouldVerb</code>, <code>MustVerb</code>, and <code>CanVerb</code>
   * traits, which <code>fixture.FreeSpec</code> mixes in, to avoid a conflict with implicit conversions provided
   * in <code>ShouldMatchers</code> and <code>MustMatchers</code>.
   * </p>
   *
   * @param string the string that is wrapped
   *
   * @author Bill Venners
   */
  protected final class FreeSpecStringWrapper(string: String) {

    /**
     * Register some text that may surround one or more tests. Thepassed function value may contain surrounding text
     * registrations (defined with dash (<code>-</code>)) and/or tests (defined with <code>in</code>). This trait's
     * implementation of this method will register the text (passed to the contructor of <code>FreeSpecStringWrapper</code>
     * and immediately invoke the passed function.
     */
    def - (fun: => Unit) {

      // SKIP-SCALATESTJS-START
      val stackDepth = 3
      val errorStackDepth = 3
      // SKIP-SCALATESTJS-END
      //SCALATESTJS-ONLY val stackDepth = 5
      //SCALATESTJS-ONLY val errorStackDepth = 10

      try {
        registerNestedBranch(string, None, fun, Resources.dashCannotAppearInsideAnIn, sourceFileName, "-", stackDepth, -2, None)
      }
      catch {
        case e: exceptions.TestFailedException => throw new exceptions.NotAllowedException(FailureMessages.assertionShouldBePutInsideInClauseNotDashClause, Some(e), e => errorStackDepth)
        case e: exceptions.TestCanceledException => throw new exceptions.NotAllowedException(FailureMessages.assertionShouldBePutInsideInClauseNotDashClause, Some(e), e => errorStackDepth)
        case tgce: exceptions.TestRegistrationClosedException => throw tgce
        case other: Throwable if (!Suite.anExceptionThatShouldCauseAnAbort(other)) => throw new exceptions.NotAllowedException(FailureMessages.exceptionWasThrownInDashClause(UnquotedString(other.getClass.getName), string), Some(other), e => errorStackDepth)
        case other: Throwable => throw other
      }
    }

    /**
     * Supports test registration.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" in { fixture => ... }
     *                    ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     *
     * @param testFun the test function
     */
    def in(testFun: FixtureParam => Future[Assertion]) {
      registerAsyncTestToRun(string, List(), "in", testFun)
    }

    /**
     * Supports registration of tests that take no fixture.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" in { () => ... }
     *                    ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     *
     * @param testFun the test function
     */
    def in(testFun: () => Future[Assertion]) {
      registerAsyncTestToRun(string, List(), "in", new NoArgTestWrapper(testFun))
    }

    /**
     * Supports pending test registration.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" is (pending)
     *                    ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     *
     * @param testFun the test function
     */
    def is(testFun: => PendingStatement) {
      registerPendingTestToRun(string, List(), "is", unusedFixtureParam => testFun)
    }

    /**
     * Supports ignored test registration.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" ignore { fixture => ... }
     *                    ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     *
     * @param testFun the test function
     */
    def ignore(testFun: FixtureParam => Future[Assertion]) {
      registerAsyncTestToIgnore(string, List(), "ignore", testFun)
    }

    /**
     * Supports registration of ignored tests that take no fixture.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" ignore { () => ... }
     *                    ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     *
     * @param testFun the test function
     */
    def ignore(testFun: () => Future[Assertion]) {
      registerAsyncTestToIgnore(string, List(), "ignore", new NoArgTestWrapper(testFun))
    }

    /**
     * Supports tagged test registration.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" taggedAs(SlowTest) in { fixture => ... }
     *                    ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     *
     * @param firstTestTag the first mandatory test tag
     * @param otherTestTags the others additional test tags
     */
    def taggedAs(firstTestTag: Tag, otherTestTags: Tag*) = {
      val tagList = firstTestTag :: otherTestTags.toList
      new ResultOfTaggedAsInvocationOnString(string, tagList)
    }
  }

  import scala.language.implicitConversions

  /**
   * Implicitly converts <code>String</code>s to <code>FreeSpecStringWrapper</code>, which enables
   * methods <code>when</code>, <code>that</code>, <code>in</code>, <code>is</code>, <code>taggedAs</code>
   * and <code>ignore</code> to be invoked on <code>String</code>s.
   */
  protected implicit def convertToFreeSpecStringWrapper(s: String) = new FreeSpecStringWrapper(s)

  /**
   * A <code>Map</code> whose keys are <code>String</code> tag names to which tests in this <code>FreeSpec</code> belong, and values
   * the <code>Set</code> of test names that belong to each tag. If this <code>FreeSpec</code> contains no tags, this method returns an empty <code>Map</code>.
   *
   * <p>
   * This trait's implementation returns tags that were passed as strings contained in <code>Tag</code> objects passed to
   * methods <code>test</code> and <code>ignore</code>.
   * </p>
   *
   * <p>
   * In addition, this trait's implementation will also auto-tag tests with class level annotations.
   * For example, if you annotate @Ignore at the class level, all test methods in the class will be auto-annotated with @Ignore.
   * </p>
   */
  override def tags: Map[String, Set[String]] = autoTagClassAnnotations(atomic.get.tagsMap, this)

  /**
   * Run a test. This trait's implementation runs the test registered with the name specified by
   * <code>testName</code>. Each test's name is a concatenation of the text of all describers surrounding a test,
   * from outside in, and the test's  spec text, with one space placed between each item. (See the documenation
   * for <code>testNames</code> for an example.)
   *
   * @param testName the name of one test to execute.
   * @param args the <code>Args</code> for this run
   * @return a <code>Status</code> object that indicates when the test started by this method has completed, and whether or not it failed .
   * @throws NullArgumentException if <code>testName</code> or <code><args/code> is <code>null</code>.
   */
  protected override def runTest(testName: String, args: Args): Status = {
    def invokeWithAsyncFixture(theTest: TestLeaf): AsyncOutcome = {
      val theConfigMap = args.configMap
      val testData = testDataFor(testName, theConfigMap)
      FutureOutcome(
        withAsyncFixture(
          new OneArgAsyncTest {
            val name = testData.name

            def apply(fixture: FixtureParam): Future[Outcome] =
              theTest.testFun(fixture).toFutureOutcome

            val configMap = testData.configMap
            val scopes = testData.scopes
            val text = testData.text
            val tags = testData.tags
          }
        )
      )
    }

    runTestImpl(thisSuite, testName, args, true, invokeWithAsyncFixture)
  }

  /**
   * <p>
   * Run zero to many of this <code>FreeSpec</code>'s tests.
   * </p>
   *
   * <p>
   * This method takes a <code>testName</code> parameter that optionally specifies a test to invoke.
   * If <code>testName</code> is <code>Some</code>, this trait's implementation of this method
   * invokes <code>runTest</code> on this object with passed <code>args</code>.
   * </p>
   *
   * <p>
   * This method takes an <code>args</code> that contains a <code>Set</code> of tag names that should be included (<code>tagsToInclude</code>), and a <code>Set</code>
   * that should be excluded (<code>tagsToExclude</code>), when deciding which of this <code>Suite</code>'s tests to execute.
   * If <code>tagsToInclude</code> is empty, all tests will be executed
   * except those those belonging to tags listed in the <code>tagsToExclude</code> <code>Set</code>. If <code>tagsToInclude</code> is non-empty, only tests
   * belonging to tags mentioned in <code>tagsToInclude</code>, and not mentioned in <code>tagsToExclude</code>
   * will be executed. However, if <code>testName</code> is <code>Some</code>, <code>tagsToInclude</code> and <code>tagsToExclude</code> are essentially ignored.
   * Only if <code>testName</code> is <code>None</code> will <code>tagsToInclude</code> and <code>tagsToExclude</code> be consulted to
   * determine which of the tests named in the <code>testNames</code> <code>Set</code> should be run. For more information on trait tags, see the main documentation for this trait.
   * </p>
   *
   * <p>
   * If <code>testName</code> is <code>None</code>, this trait's implementation of this method
   * invokes <code>testNames</code> on this <code>Suite</code> to get a <code>Set</code> of names of tests to potentially execute.
   * (A <code>testNames</code> value of <code>None</code> essentially acts as a wildcard that means all tests in
   * this <code>Suite</code> that are selected by <code>tagsToInclude</code> and <code>tagsToExclude</code> should be executed.)
   * For each test in the <code>testName</code> <code>Set</code>, in the order
   * they appear in the iterator obtained by invoking the <code>elements</code> method on the <code>Set</code>, this trait's implementation
   * of this method checks whether the test should be run based on the <code>tagsToInclude</code> and <code>tagsToExclude</code> <code>Set</code>s.
   * If so, this implementation invokes <code>runTest</code> via passed in <code>args</code>.
   * </p>
   *
   * @param testName an optional name of one test to execute. If <code>None</code>, all relevant tests should be executed.
   *                 I.e., <code>None</code> acts like a wildcard that means execute all relevant tests in this <code>fixture.FreeSpec</code>.
   * @param args the <code>Args</code> for this run
   * @return a <code>Status</code> object that indicates when all tests started by this method have completed, and whether or not a failure occurred.
   * @throws NullArgumentException if <code>testName</code> or <code>args</code> is <code>null</code>.
   */
  protected override def runTests(testName: Option[String], args: Args): Status = {
    runTestsImpl(thisSuite, testName, args, true, parallelAsyncTestExecution, runTest)
  }

  /**
   * An immutable <code>Set</code> of test names. If this <code>fixture.FreeSpec</code> contains no tests, this method returns an
   * empty <code>Set</code>.
   *
   * <p>
   * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's
   * iterator will return those names in the order in which the tests were registered. Each test's name is composed
   * of the concatenation of the text of each surrounding describer, in order from outside in, and the text of the
   * example itself, with all components separated by a space.
   * </p>
   *
   * @return the <code>Set</code> of test names
   */
  override def testNames: Set[String] = {
    // I'm returning a ListSet here so that they tests will be run in registration order
    ListSet(atomic.get.testNamesList.toArray: _*)
  }

  override def run(testName: Option[String], args: Args): Status = {
    runImpl(thisSuite, testName, args, super.run)
  }

  /**
   * Supports shared test registration in <code>fixture.FreeSpec</code>s.
   *
   * <p>
   * This field enables syntax such as the following:
   * </p>
   *
   * <pre class="stHighlight">
   * behave like nonFullStack(stackWithOneItem)
   * ^
   * </pre>
   *
   * <p>
   * For more information and examples of the use of <cod>behave</code>, see the <a href="../FreeSpec.html#SharedTests">Shared tests section</a>
   * in the main documentation for trait <code>FreeSpec</code>.
   * </p>
   */
  protected val behave = new BehaveWord

  /**
   * Suite style name.
   *
   * @return <code>org.scalatest.fixture.FreeSpec</code>
   */
  final override val styleName: String = "org.scalatest.fixture.FreeSpec"

  override def testDataFor(testName: String, theConfigMap: ConfigMap = ConfigMap.empty): TestData = createTestDataFor(testName, theConfigMap, this)
}