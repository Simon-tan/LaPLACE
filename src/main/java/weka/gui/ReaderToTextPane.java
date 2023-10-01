/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * ReaderToTextArea.java
 * Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.gui;

import java.awt.Color;
import java.io.InterruptedIOException;
import java.io.LineNumberReader;
import java.io.Reader;

import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

/**
 * A class that sends all lines from a reader to a JTextPane component.
 * 
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 15289 $
 */
public class ReaderToTextPane
  extends Thread {

  /** The reader being monitored. */
  protected LineNumberReader m_Input;

  /** The output text component. */
  protected JTextPane m_Output;
  
  /** the color to use. */
  protected Color m_Color;

  /** string buffer to use for content */
  protected StringBuffer m_Buffer;

  /**
   * Sets up the thread. Using black as color for displaying the text.
   *
   * @param input 	the Reader to monitor
   * @param output 	the TextArea to send output to
   */
  public ReaderToTextPane(Reader input, JTextPane output) {
    this(input, output, Color.BLACK);
  }

  /**
   * Sets up the thread.
   *
   * @param input 	the Reader to monitor
   * @param output 	the TextArea to send output to
   * @param color	the color to use
   */
  public ReaderToTextPane(Reader input, JTextPane output, Color color) {
    StyledDocument      doc;
    Style               style;

    setDaemon(true);
    
    m_Color  = color;
    m_Input  = new LineNumberReader(input);
    m_Output = output;
    m_Buffer = new StringBuffer();
    
    doc   = m_Output.getStyledDocument();
    style = StyleContext.getDefaultStyleContext()
                        .getStyle(StyleContext.DEFAULT_STYLE);
    style = doc.addStyle(getStyleName(), style);
    StyleConstants.setFontFamily(style, "monospaced");
    StyleConstants.setForeground(style, m_Color);
  }
  
  /**
   * Returns the color in use.
   * 
   * @return		the color
   */
  public Color getColor() {
    return m_Color;
  }
  
  /**
   * Returns the style name.
   * 
   * @return		the style name
   */
  protected String getStyleName() {
    return "" + m_Color.hashCode();
  }

  /**
   * Sit here listening for lines of input and appending them straight
   * to the text component.
   */
  public void run() {

    Thread t = new Thread() {
      public void run() {
        long oldSize = 0;
        while (true) {
          try {
            long currentSize = m_Buffer.length();
            if ((currentSize > 0) && (currentSize == oldSize)) {
              StyledDocument doc = m_Output.getStyledDocument();
              doc.insertString(doc.getLength(), m_Buffer.toString(), doc.getStyle(getStyleName()));
              m_Output.setCaretPosition(doc.getLength());
              m_Buffer.delete(0, m_Buffer.length());
              oldSize = 0;
            } else {
              oldSize = currentSize;
            }
            sleep(100);
          } catch (Exception e) {
            if (e instanceof InterruptedException || e instanceof InterruptedIOException) {
              break;
            }
          }
        }
      }
    };
    t.start();

    while (true) {
      try {
        String s = m_Input.readLine();
        m_Buffer.append(s).append('\n');
      } catch (Exception ex) {
        if (ex instanceof InterruptedException || ex instanceof InterruptedIOException) {
          t.interrupt();
          break;
        }
        try {
          sleep(100);
        } catch (Exception e) {
          if (e instanceof InterruptedException) {
            t.interrupt();
            break;
          }
        }
      }
    }
  }
}
