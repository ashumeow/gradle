/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.play.tasks

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.test.fixtures.archive.JarTestFixture

class CoffeeScriptCompileIntegrationTest extends AbstractIntegrationSpec {
    def setup() {
        buildFile << """
            plugins {
                id 'play-coffeescript'
            }

            repositories{
                jcenter()
                maven{
                    name = "typesafe-maven-release"
                    url = "http://repo.typesafe.com/typesafe/maven-releases"
                }
                maven {
                    name = "gradle-js"
                    url = "https://repo.gradle.org/gradle/javascript-public"
                }
            }
        """
    }

    def "compile default coffeescript source set as part of play application build" () {
        given:
        withCoffeeScriptSource("app/test.coffee")

        when:
        succeeds "assemble"

        then:
        executed(":compilePlayBinaryPlayCoffeeScriptSources", ":processPlayBinaryPlayCoffeeScriptGenerated")
        file("build/playBinary/coffeescript/test.js").exists()
        compareWithoutWhiteSpace file("build/playBinary/coffeescript/test.js").text, expectedJavaScript()
        jar("build/jars/play/playBinary.jar").containsDescendants(
                "test.js"
        )
    }

    def "compiles multiple coffeescript source sets as part of play application build" () {
        given:
        withCoffeeScriptSource("app/test1.coffee")
        withCoffeeScriptSource("src/play/extraCoffeeScript/xxx/test2.coffee")
        withCoffeeScriptSource("extra/a/b/c/test3.coffee")
        file('src/play/extraJavaScript/test/test4.js') << expectedJavaScript()
        file('/app/test5.js') << expectedJavaScript()

        when:
        buildFile << """
            model {
                components {
                    play {
                        sources {
                            extraCoffeeScript(CoffeeScriptSourceSet)
                            anotherCoffeeScript(CoffeeScriptSourceSet) {
                                source.srcDir "extra"
                            }
                            extraJavaScript(JavaScriptSourceSet)
                        }
                    }
                }
            }
        """
        succeeds "assemble"

        then:
        executed(":compilePlayBinaryPlayCoffeeScriptSources",
                 ":compilePlayBinaryPlayExtraCoffeeScript",
                 ":compilePlayBinaryPlayAnotherCoffeeScript",
                 ":processPlayBinaryPlayExtraJavaScript",
                 ":processPlayBinaryPlayJavaScriptSources",
                 ":processPlayBinaryPlayCoffeeScriptGenerated")
        file("build/playBinary/coffeescript/test1.js").exists()
        file("build/playBinary/coffeescript/xxx/test2.js").exists()
        file("build/playBinary/coffeescript/a/b/c/test3.js").exists()
        compareWithoutWhiteSpace file("build/playBinary/coffeescript/test1.js").text, expectedJavaScript()
        compareWithoutWhiteSpace file("build/playBinary/coffeescript/xxx/test2.js").text, expectedJavaScript()
        compareWithoutWhiteSpace file("build/playBinary/coffeescript/a/b/c/test3.js").text, expectedJavaScript()
        jar("build/jars/play/playBinary.jar").containsDescendants(
                "test1.js",
                "xxx/test2.js",
                "a/b/c/test3.js",
                "test/test4.js",
                "test5.js"
        )
    }

    JarTestFixture jar(String fileName) {
        new JarTestFixture(file(fileName))
    }

    boolean compareWithoutWhiteSpace(String string1, String string2) {
        return withoutWhiteSpace(string1) == withoutWhiteSpace(string2)
    }

    def withoutWhiteSpace(String string) {
        return string.replaceAll("\\s+", " ");
    }

    def withCoffeeScriptSource(String path) {
        return file(path) << """
# Assignment:
number   = 42
opposite = true

# Conditions:
number = -42 if opposite

# Functions:
square = (x) -> x * x

# Arrays:
list = [1, 2, 3, 4, 5]

# Objects:
math =
  root:   Math.sqrt
  square: square
  cube:   (x) -> x * square x

# Splats:
race = (winner, runners...) ->
  print winner, runners

# Existence:
alert "I knew it!" if elvis?

# Array comprehensions:
cubes = (math.cube num for num in list)"""
    }

    def expectedJavaScript() {
        return """(function() {
var cubes, list, math, num, number, opposite, race, square,
  __slice = [].slice;

number = 42;

opposite = true;

if (opposite) {
  number = -42;
}

square = function(x) {
  return x * x;
};

list = [1, 2, 3, 4, 5];

math = {
  root: Math.sqrt,
  square: square,
  cube: function(x) {
    return x * square(x);
  }
};

race = function() {
  var runners, winner;
  winner = arguments[0], runners = 2 <= arguments.length ? __slice.call(arguments, 1) : [];
  return print(winner, runners);
};

if (typeof elvis !== "undefined" && elvis !== null) {
  alert("I knew it!");
}

cubes = (function() {
  var _i, _len, _results;
  _results = [];
  for (_i = 0, _len = list.length; _i < _len; _i++) {
    num = list[_i];
    _results.push(math.cube(num));
  }
  return _results;
})();
}).call(this);
"""
    }
}
