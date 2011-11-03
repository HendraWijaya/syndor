package syndor.xml

import java.io.FilterReader
import java.io.Reader

/**
 * This is a filter reader that removes all invalid XML chars
 */
class XmlFilterReader(in: Reader) extends FilterReader(in) {
  override def read(): Int = {
    var character = -1

    do {
      character = super.read()
    } while (!isValid(character) && character != -1)

    return character
  }

  def isValid(c: Int) = {
    if (
      (c == 0x9) ||
      (c == 0xA) ||
      (c == 0xD) ||
      ((c >= 0x20) && (c <= 0xD7FF)) ||
      ((c >= 0xE000) && (c <= 0xFFFD)) ||
      ((c >= 0x10000) && (c <= 0x10FFFF))
    )
      true
    else
      false
  }
}