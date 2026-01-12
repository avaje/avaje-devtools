package io.avaje.tools.devtool.service;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;

public class ClipB {

  public static String getClipboardText() {
    String result = "";
    // Get the system clipboard
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    // Get the contents as a Transferable object
    Transferable contents = clipboard.getContents(null);

    // Check if the contents are non-null and support the string flavor
    if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      try {
        // Retrieve the data as a String
        result = (String) contents.getTransferData(DataFlavor.stringFlavor);
      } catch (UnsupportedFlavorException | IOException e) {
        // Handle exceptions related to unsupported data flavors or I/O errors
        System.err.println("Error reading clipboard: " + e.getMessage());
        e.printStackTrace();
      }
    }
    return result;
  }

  public static void setClipboardText(String clip) {
    System.out.println("setClipboardText: " + clip);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    var t = new StringSelection(clip);
    clipboard.setContents(t, null);
  }
}
