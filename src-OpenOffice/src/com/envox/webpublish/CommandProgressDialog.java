/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

import com.envox.webpublish.GUIHelper.Dialog;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XProgressBar;
import com.sun.star.awt.XWindow;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author martin
 */
public class CommandProgressDialog extends Dialog {
    XWindow m_xDialogWindow;
    XFixedText m_lblDescription;
    XWindow m_btnClose;
    XProgressBar m_progressBar;
    XWindow m_progressBarWnd;

    Command m_command;
    Thread m_thread;
    CommandErrorException m_errorException;

    public static void start(Command command) throws CommandErrorException {
        try {
            CommandProgressDialog dlgProgress = new CommandProgressDialog(command);
            dlgProgress.start();
        } catch (Exception ex) {
            Logger.getLogger(CommandProgressDialog.class.getName()).log(Level.SEVERE, null, ex);
            throw new CommandErrorException(CommandError.ERROR_PROGRESS_DIALOG_CREATION);
        }
    }

    private CommandProgressDialog(Command command) throws Exception {
        m_command = command;
        createDialog("PublishProgress");

        m_xDialogWindow = (XWindow) UnoRuntime.queryInterface(XWindow.class, m_xDialog);

        m_lblDescription = m_controlContainer.getFixedText("lblStatus");
        m_btnClose = m_controlContainer.getWindow("btnClose");

        m_progressBarWnd = m_controlContainer.getWindow("progressBar");
        m_progressBar = m_controlContainer.getProgressBar("progressBar");
    }

    private Thread createThread() {
        return new Thread(new RunnableWithProgress() {
            public void run() {
                try {
                    m_command.run(this);
                } catch (CommandErrorException ex) {
                    Logger.getLogger(CommandProgressDialog.class.getName()).log(Level.SEVERE, null, ex);
                    m_errorException = ex;
                }
                Util.mainThreadNotify(this, null);
            }

            public void status(String description) {
                Util.mainThreadNotify(this, description);
            }

            public void progress(int value, int min, int max) {
                Util.mainThreadNotify(this, new int[] { value, min, max } );
            }

            public void notify(Object arg0) {
                if (arg0 instanceof String) {
                    setDescription((String)arg0);
                } else if (arg0 instanceof int[]) {
                    int[] arr = (int[]) arg0;
                    setProgress(arr[0], arr[1], arr[2]);
                } else {
                    if (m_errorException != null) {
                        error();
                    } else {
                        success();
                    }
                }
            }
        });
    }

    private void setDescription(String description) {
        m_lblDescription.setText(description);
        setThrobberActive(true);
        m_progressBarWnd.setVisible(false);
    }

    private void setProgress(int value, int min, int max) {
        setThrobberActive(false);
        m_progressBarWnd.setVisible(true);
        m_progressBar.setRange(min, max);
        m_progressBar.setValue(value);
    }

    private void start() throws CommandErrorException {
        m_btnClose.setVisible(false);

        insertThrobber(101, 29, 10, 10);

        m_thread = createThread();
        m_thread.start();

        m_xDialog.execute();
    }

    private void error() {
        setThrobberActive(false);
        m_progressBarWnd.setVisible(false);
        m_btnClose.setVisible(true);

        Util.showMessageBox("WebPublish", m_errorException.getMessage());
    }

    private void success() {
        setThrobberActive(false);
        m_btnClose.setVisible(true);
    }
}
