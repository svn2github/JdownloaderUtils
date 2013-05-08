package org.appwork.uio;

import javax.swing.ImageIcon;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.UserIODefinition;

public interface UserIOHandlerInterface {

    boolean showConfirmDialog(int flags, String title, String message, ImageIcon icon, String ok, String cancel);

    boolean showConfirmDialog(int flag, String title, String message);

    void showMessageDialog(String message);

    <T extends UserIODefinition> T show(Class<T> class1, AbstractDialog<?> defImpl);

    void showErrorMessage(String message);

}