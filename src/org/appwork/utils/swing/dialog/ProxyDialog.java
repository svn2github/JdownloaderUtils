package org.appwork.utils.swing.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import net.miginfocom.swing.MigLayout;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.StringUtils;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
import org.appwork.utils.swing.EDTRunner;

public class ProxyDialog extends AbstractDialog<HTTPProxy> implements CaretListener {

    private JComboBox        cmbType;
    private ExtTextField     txtHost;
    private ExtTextField     txtPort;
    private ExtTextField     txtUser;
    private ExtPasswordField txtPass;

    private final String[]   types = new String[] { _AWU.T.ProxyDialog_http(), _AWU.T.ProxyDialog_socks5(), _AWU.T.ProxyDialog_socks4(), _AWU.T.ProxyDialog_direct() };
    private JLabel           lblUser;
    private JLabel           lblPass;
    private JLabel           lblPort;
    private JLabel           lblHost;
    private DelayedRunnable  delayer;
    private TYPE             type;
    private HTTPProxy        proxy;
    private ExtTextArea      desc;
    private String           message;

    public ProxyDialog(final HTTPProxy usedProxy, final String message) {
        super(Dialog.STYLE_HIDE_ICON, _AWU.T.proxydialog_title(), null, _AWU.T.lit_save(), _AWU.T.ABSTRACTDIALOG_BUTTON_CANCEL());
        proxy = usedProxy;
        this.message = message;
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel panel = new JPanel(new MigLayout("ins 0, wrap 4", "[][grow 10,fill][][grow 3,fill]"));
        desc = new ExtTextArea();
        desc.setText(message);
        desc.setLabelMode(true);
        panel.add(desc, "spanx,pushx,growx");
        panel.add(new JLabel(_AWU.T.ProxyDialog_type()));
        panel.add(cmbType = new JComboBox(types), "spanx");
        cmbType.addActionListener(this);
        panel.add(lblHost = new JLabel(_AWU.T.ProxyDialog_hostport()));
        panel.add(txtHost = new ExtTextField() {
            @Override
            public void onChanged() {

                delayer.resetAndStart();

            }

        });

        delayer = new DelayedRunnable(ToolTipController.EXECUTER, 2000) {

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        set(txtHost.getText());

                    }
                };

            }

        };
        txtHost.addCaretListener(this);
        panel.add(lblPort = new JLabel(":"));
        panel.add(txtPort = new ExtTextField(), "shrinkx");
        // txtHost.setHelpText(_AWU.T.ProxyDialog_hostport());
        txtPort.setText("8080");
        txtPort.addCaretListener(this);
   
        panel.add(lblUser = new JLabel(_AWU.T.ProxyDialog_username()));
        panel.add(txtUser = new ExtTextField(), "spanx");
        // txtUser.setHelpText(_AWU.T.ProxyDialog_username());

        panel.add(lblPass = new JLabel(_AWU.T.ProxyDialog_password()));
        panel.add(txtPass = new ExtPasswordField(), "spanx");
        // txtPass.setHelpText(_AWU.T.ProxyDialog_password());
        okButton.setEnabled(true);
        registerFocus(txtPort);
        registerFocus(txtUser);
        registerFocus(txtHost);
        // set(ClipboardMonitoring.getINSTANCE().getCurrentContent());
        set(proxy);
        return panel;
    }

    /**
     * @param txtPort2
     */
    private void registerFocus(final JTextField field) {
        field.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(final FocusEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void focusGained(final FocusEvent e) {
                field.selectAll();
            }
        });

    }

    public static void main(final String[] args) throws UnsupportedEncodingException, DialogClosedException, DialogCanceledException {
        // SyntheticaHelper.init();
        Dialog.getInstance().showDialog(new ProxyDialog(HTTPProxy.NONE, "No Connection to the Internet. Please check your Connection settings!"));
    }

    /**
     * @param proxy2
     */
    private void set(final HTTPProxy p) {
        txtUser.setText(p.getUser());
        switch (p.getType()) {
        case DIRECT:
        case NONE:
            cmbType.setSelectedIndex(3);
            txtHost.setText(p.getLocalIP() == null ? "" : p.getLocalIP().getHostAddress());

            break;
        case HTTP:
            cmbType.setSelectedIndex(0);
            txtHost.setText(p.getHost());
            txtPort.setText(p.getPort() + "");
            txtUser.setText(p.getUser());
            break;

        case SOCKS4:
            cmbType.setSelectedIndex(2);
            txtHost.setText(p.getHost());
            txtPort.setText(p.getPort() + "");
            break;
        case SOCKS5:
            cmbType.setSelectedIndex(1);
            txtHost.setText(p.getHost());
            txtPort.setText(p.getPort() + "");
            txtUser.setText(p.getUser());
            break;

        }

    }

    public void dispose() {
        super.dispose();
        delayer.stop();
    }

    protected void set(final String text) {

        final int carPos = txtHost.getCaretPosition();
        String myText = text;
        if (myText.endsWith(":")) { return; }
        for (int i = 0; i < 2; i++) {
            try {
                final URL url = new URL(myText);
                txtHost.setText(url.getHost());
                if (url.getPort() > 0) {
                    txtPort.setText(url.getPort() + "");
                }
                final String userInfo = url.getUserInfo();
                if (userInfo != null) {
                    final int in = userInfo.indexOf(":");
                    if (in >= 0) {
                        txtUser.setText(userInfo.substring(0, in));
                        txtPass.setText(userInfo.substring(in + 1));
                    } else {
                        txtUser.setText(userInfo);
                    }
                }
                return;
            } catch (final MalformedURLException e) {
                if (text.contains(":")) {
                    myText = "http://" + myText;
                }
            }
        }

        txtHost.setCaretPosition(carPos);

    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cmbType) {

            switch (cmbType.getSelectedIndex()) {
            case 0:
                // http
                txtPass.setEnabled(true);
                lblPass.setEnabled(true);
                txtPort.setEnabled(true);
                lblUser.setEnabled(true);
                txtUser.setEnabled(true);
                lblPort.setEnabled(true);

                if (StringUtils.isEmpty(txtPort.getText())) {
                    txtPort.setText("8080");
                }
                break;
            case 1:
                // socks5
                txtPass.setEnabled(true);
                lblPass.setEnabled(true);
                txtPort.setEnabled(true);
                lblUser.setEnabled(true);
                txtUser.setEnabled(true);
                lblPort.setEnabled(true);
                if (StringUtils.isEmpty(txtPort.getText())) {
                    txtPort.setText("1080");
                }
                break;
            case 2:
                // socks4
                txtPass.setEnabled(false);
                lblPass.setEnabled(false);
                txtPort.setEnabled(true);
                lblUser.setEnabled(true);
                txtUser.setEnabled(true);
                lblPort.setEnabled(true);
                if (StringUtils.isEmpty(txtPort.getText())) {
                    txtPort.setText("1080");
                }
                break;
            case 3:
                // direct
                txtPass.setEnabled(false);
                lblPass.setEnabled(false);
                txtPort.setEnabled(false);
                lblUser.setEnabled(false);
                txtUser.setEnabled(false);
                lblPort.setEnabled(false);
                break;
            default:
                txtPass.setEnabled(false);
                lblPass.setEnabled(false);
                lblUser.setEnabled(true);
                txtUser.setEnabled(true);
                lblPort.setEnabled(true);
                if (StringUtils.isEmpty(txtPort.getText())) {
                    txtPort.setText("1080");
                }
            }

        } else {
            super.actionPerformed(e);
        }
    }

    /**
     * returns HTTPProxy for given settings
     */
    @Override
    protected HTTPProxy createReturnValue() {
        final int mask = getReturnmask();
        if (BinaryLogic.containsSome(mask, Dialog.RETURN_CLOSED)) { return null; }
        if (BinaryLogic.containsSome(mask, Dialog.RETURN_CANCEL)) { return null; }
        try {

            HTTPProxy.TYPE type = null;
            if (cmbType.getSelectedIndex() == 0) {
                type = HTTPProxy.TYPE.HTTP;
            } else if (cmbType.getSelectedIndex() == 1) {
                type = HTTPProxy.TYPE.SOCKS5;
            } else if (cmbType.getSelectedIndex() == 2) {
                type = HTTPProxy.TYPE.SOCKS4;
            } else if (cmbType.getSelectedIndex() == 3) {
                type = HTTPProxy.TYPE.DIRECT;
                return HTTPProxy.parseHTTPProxy("direct://" + txtHost.getText());
            } else {
                return null;
            }
            final HTTPProxy ret = new HTTPProxy(type, txtHost.getText(), Integer.parseInt(txtPort.getText().trim()));

            ret.setPass(txtPass.getText());
            ret.setUser(txtUser.getText());

            return ret;
        } catch (final Throwable e) {
            Log.exception(e);
            return null;
        }
    }

    /**
     * update okayButton enabled status, check if host/port(valid number) or
     * host is given
     */
    public void caretUpdate(final CaretEvent e) {
        boolean enable = false;
        try {
            if (cmbType.getSelectedIndex() != 2) {
                if (txtHost.getDocument().getLength() > 0 && txtPort.getDocument().getLength() > 0) {
                    try {
                        final int port = Integer.parseInt(txtPort.getText());
                        if (port > 0 && port < 65535) {
                            enable = true;
                        }
                    } catch (final Throwable ee) {
                    }
                }
            } else {
                if (txtHost.getDocument().getLength() > 0) {
                    enable = true;
                }
            }
        } finally {
            okButton.setEnabled(enable);
        }
    }

    /**
     * @return
     */
    public int getPort() {
        try {
            return Integer.parseInt(txtPort.getText());
        } catch (final Exception e) {
            return -1;
        }
    }

    /**
     * @return
     */
    public String getHost() {
        // TODO Auto-generated method stub
        return txtHost.getText();
    }

    /**
     * @return
     */
    public String getUser() {
        // TODO Auto-generated method stub
        return txtUser.getText();
    }

    /**
     * @return
     */
    public String getPass() {
        // TODO txtP-generated method stub
        return txtPass.getText();
    }

    /**
     * @return
     */
    public TYPE getType() {
        switch (cmbType.getSelectedIndex()) {
        case 0:
            // http
            return TYPE.HTTP;

        case 1:
            // socks5
            return TYPE.SOCKS5;
        case 2:
            // socks4
            return TYPE.SOCKS4;
        case 3:
            if (StringUtils.isEmpty(txtHost.getText())) {
                return TYPE.NONE;
            } else {
                return TYPE.DIRECT;
            }
        }
        return TYPE.NONE;

    }

    /**
     * @return
     */
    public HTTPProxy getProxy() {
        final HTTPProxy ret = new HTTPProxy(getType());
        ret.setHost(getHost());
        ret.setUser(getUser());
        ret.setPort(getPort());

        ret.setPass(getPass());
        return ret;
    }

}
