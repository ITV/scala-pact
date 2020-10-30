package com.itv.scalapact.shared

object ColourOutput {
  implicit class ColouredString(val s: String) extends AnyVal {
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
