package com.itv.scalapact.shared

import scala.language.implicitConversions

object ColourOuput {

  implicit def toColouredString(s: String): ColouredString = new ColouredString(s)

  class ColouredString(s: String) {
    import Console._

    def black: String      = BLACK + s + RESET
    def white: String      = WHITE + s + RESET
    def red: String        = RED + s + RESET
    def green: String      = GREEN + s + RESET
    def yellow: String     = YELLOW + s + RESET
    def blue: String       = BLUE + s + RESET
    def magenta: String    = MAGENTA + s + RESET
    def cyan: String       = CYAN + s + RESET
    def bold: String       = BOLD + s + RESET
    def underlined: String = UNDERLINED + s + RESET
  }

}
