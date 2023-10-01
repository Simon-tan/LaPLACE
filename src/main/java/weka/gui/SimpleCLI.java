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
 *    SimpleCLI.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import weka.gui.scripting.ScriptingPanel;

/**
 * Creates a very simple command line for invoking the main method of
 * classes. System.out and System.err are redirected to an output area.
 * Features a simple command history -- use up and down arrows to move
 * through previous commmands. This gui uses only AWT (i.e. no Swing).
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 15293 $
 */
public class SimpleCLI
  extends JFrame {
  
  /** for serialization. */
  static final long serialVersionUID = -50661410800566036L;
  
  /**
   * Constructor.
   *
   * @throws Exception if an error occurs
   */
  public SimpleCLI() throws Exception {
    SimpleCLIPanel	panel;

    panel = new SimpleCLIPanel();

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent w) {
        panel.terminate();
        dispose();
      }
    });

    setLayout(new BorderLayout());
    setTitle(panel.getTitle());
    setIconImage(panel.getIcon().getImage());
    add(panel);
    pack();
    setSize(600, 500);
    setLocationRelativeTo(null);
    setVisible(true);
  }

  /**
   * Method to start up the simple cli.
   *
   * @param args 	Not used.
   */
  public static void main(String[] args) {

    weka.core.logging.Logger.log(weka.core.logging.Logger.Level.INFO,
            "Logging started");

    LookAndFeel.setLookAndFeel();
    // make sure that packages are loaded and the GenericPropertiesCreator
    // executes to populate the lists correctly
    weka.gui.GenericObjectEditor.determineClasses();

    ScriptingPanel.showPanel(new SimpleCLIPanel(), args, 600, 500);
  }
}
