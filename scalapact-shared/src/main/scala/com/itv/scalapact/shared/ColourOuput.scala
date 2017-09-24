package com.itv.scalapact.shared

import scala.language.implicitConversions

object ColourOuput {

  implicit def toColouredString(s: String): ColouredString = new ColouredString(s)

  class ColouredString(s: String) {
    import Console._

    def black = BLACK + s + RESET
    def white = WHITE + s + RESET
    def red = RED + s + RESET
    def green = GREEN + s + RESET
    def yellow = YELLOW + s + RESET
    def blue = BLUE + s + RESET
    def magenta = MAGENTA + s + RESET
    def cyan = CYAN + s + RESET
    def bold = BOLD + s + RESET
    def underlined = UNDERLINED + s + RESET
  }

}
