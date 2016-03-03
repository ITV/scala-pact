package com.itv.scalapact.plugin.common

import scala.language.implicitConversions

/**
  * Stolen from here:
  * https://github.com/ktoso/scala-rainbow
  *
  * Have submitted an issue to the author re:cross compiling for scala 2.10,
  * if he fixes it we should use the library properly.
  */
object Rainbow {

  implicit def hasRainbow(s: String): RainbowString = new RainbowString(s)

  class RainbowString(s: String) {
    import Console._

    /** Colorize the given string foreground to ANSI black */
    def black = BLACK + s + RESET
    /** Colorize the given string foreground to ANSI red */
    def red = RED + s + RESET
    /** Colorize the given string foreground to ANSI red */
    def green = GREEN + s + RESET
    /** Colorize the given string foreground to ANSI red */
    def yellow = YELLOW + s + RESET
    /** Colorize the given string foreground to ANSI red */
    def blue = BLUE + s + RESET
    /** Colorize the given string foreground to ANSI red */
    def magenta = MAGENTA + s + RESET
    /** Colorize the given string foreground to ANSI red */
    def cyan = CYAN + s + RESET
    /** Colorize the given string foreground to ANSI red */
    def white = WHITE + s + RESET

    /** Colorize the given string background to ANSI red */
    def onBlack = BLACK_B + s + RESET
    /** Colorize the given string background to ANSI red */
    def onRed = RED_B+ s + RESET
    /** Colorize the given string background to ANSI red */
    def onGreen = GREEN_B+ s + RESET
    /** Colorize the given string background to ANSI red */
    def onYellow = YELLOW_B + s + RESET
    /** Colorize the given string background to ANSI red */
    def onBlue = BLUE_B+ s + RESET
    /** Colorize the given string background to ANSI red */
    def onMagenta = MAGENTA_B + s + RESET
    /** Colorize the given string background to ANSI red */
    def onCyan = CYAN_B+ s + RESET
    /** Colorize the given string background to ANSI red */
    def onWhite = WHITE_B+ s + RESET

    /** Make the given string bold */
    def bold = BOLD + s + RESET
    /** Underline the given string */
    def underlined = UNDERLINED + s + RESET
    /** Make the given string blink (some terminals may turn this off) */
    def blink = BLINK + s + RESET
    /** Reverse the ANSI colors of the given string */
    def reversed = REVERSED + s + RESET
    /** Make the given string invisible using ANSI color codes */
    def invisible = INVISIBLE + s + RESET
  }

}
