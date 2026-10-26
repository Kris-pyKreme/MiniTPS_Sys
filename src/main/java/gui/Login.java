package gui;

import security.*;
import service.AccountService;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

public class Login extends JFrame {
    private final JTextField txtUser=new JTextField();
    private final JPasswordField txtPass=new JPasswordField();
    private final JComboBox<Role> cmbRole=new JComboBox<>(Role.values());
    private final JButton btnLogin=Theme.primary("Sign in to your workspace");
    private final JLabel message=Theme.label(" ",12,new Color(0xB34747),false);
    private final AccountService accounts=new AccountService();
    private int failures;
    private long retryAfter;

    public Login() {
        super("KlinikAccess · Staff sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Rectangle available=availableScreenBounds();
        setMinimumSize(new Dimension(Math.min(860,available.width),Math.min(680,available.height)));
        setSize(Math.min(1040,available.width),Math.min(780,available.height));setLocationRelativeTo(null);
        JPanel root=new JPanel(new BorderLayout());root.setBackground(Theme.BG);root.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        JPanel backdrop=Theme.surface(new BorderLayout(),Theme.LOGIN_BG,24,10);
        JPanel center=new JPanel(new GridBagLayout());center.setOpaque(false);
        JPanel stack=Theme.column();
        JPanel brandRow=new JPanel(new FlowLayout(FlowLayout.CENTER,0,0));brandRow.setOpaque(false);
        brandRow.add(Theme.brand(44,26));brandRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        stack.add(brandRow);stack.add(Box.createVerticalStrut(20));
        JPanel card=Theme.surface(new BorderLayout(),Theme.WHITE,22,28);card.setName("signInCard");
        JPanel form=Theme.column();
        JLabel portal=Theme.label("STAFF PORTAL",13,Theme.TEAL,true);
        java.util.Map<java.awt.font.TextAttribute,Object> tracking=new java.util.HashMap<>();tracking.put(java.awt.font.TextAttribute.TRACKING,.08f);
        portal.setFont(portal.getFont().deriveFont(tracking));form.add(portal);form.add(Box.createVerticalStrut(12));
        form.add(Theme.label("Welcome back",30,Theme.INK,true));form.add(Box.createVerticalStrut(6));
        form.add(Theme.label("Sign in to continue caring for your community.",16,Theme.MUTED,false));form.add(Box.createVerticalStrut(26));
        field(form,"Your role",cmbRole);field(form,"Username",txtUser);field(form,"Password",txtPass);
        txtUser.putClientProperty("JTextField.placeholderText","Enter your username");txtPass.putClientProperty("JTextField.placeholderText","Enter your password");
        JPanel options=new JPanel(new BorderLayout());options.setOpaque(false);
        JCheckBox show=new JCheckBox("Show password");show.setName("showPassword");show.setOpaque(false);show.setFont(Theme.font(14,false));show.setForeground(Theme.MUTED);
        char echo=txtPass.getEchoChar();show.addActionListener(e->txtPass.setEchoChar(show.isSelected()?(char)0:echo));options.add(show,BorderLayout.WEST);
        JButton forgot=new JButton("Forgot password?");forgot.setName("forgotPassword");forgot.setBorderPainted(false);forgot.setContentAreaFilled(false);
        forgot.setForeground(Theme.TEAL);forgot.setFont(Theme.font(14,false));forgot.setMargin(new Insets(0,0,0,0));forgot.addActionListener(e->requestReset());options.add(forgot,BorderLayout.EAST);
        int optionsHeight=Math.max(26,Math.max(show.getPreferredSize().height,forgot.getPreferredSize().height));
        options.setPreferredSize(new Dimension(520,optionsHeight));options.setMinimumSize(new Dimension(0,optionsHeight));options.setMaximumSize(new Dimension(Integer.MAX_VALUE,optionsHeight));form.add(options);
        message.setPreferredSize(new Dimension(520,23));message.setMaximumSize(new Dimension(Integer.MAX_VALUE,23));form.add(message);
        btnLogin.setText("Sign in to your workspace");btnLogin.setIcon(new LineIcon("arrow",18,Theme.WHITE));btnLogin.setHorizontalTextPosition(SwingConstants.LEFT);btnLogin.setIconTextGap(12);
        btnLogin.setFont(Theme.font(17,true));int buttonHeight=Theme.controlHeight(btnLogin,48);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE,buttonHeight));btnLogin.setMinimumSize(new Dimension(0,buttonHeight));btnLogin.setPreferredSize(new Dimension(520,buttonHeight));
        btnLogin.addActionListener(e->doLogin());form.add(btnLogin);form.add(Box.createVerticalStrut(20));
        JLabel accountNote=Theme.label("Staff accounts are managed by your clinic administrator.",13,new Color(0x8B8D88),false);
        accountNote.setHorizontalAlignment(SwingConstants.CENTER);accountNote.setMaximumSize(new Dimension(Integer.MAX_VALUE,22));form.add(accountNote);
        for(Component c:form.getComponents())if(c instanceof JComponent)((JComponent)c).setAlignmentX(0);
        card.add(form,BorderLayout.CENTER);stack.add(card);stack.add(Box.createVerticalStrut(16));
        JPanel bottom=new JPanel(new BorderLayout());bottom.setOpaque(false);bottom.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
        JLabel tagline=Theme.label("A LITTLE MORE CARE. EVERY DAY.",12,Theme.TEAL,true);tagline.setHorizontalAlignment(SwingConstants.CENTER);tagline.setFont(tagline.getFont().deriveFont(tracking));bottom.add(tagline);
        stack.add(bottom);
        JPanel constrained=new JPanel(new BorderLayout());constrained.setOpaque(false);constrained.add(stack);constrained.setPreferredSize(new Dimension(600,stack.getPreferredSize().height));
        center.add(constrained);JScrollPane scroll=new JScrollPane(center);scroll.setBorder(BorderFactory.createEmptyBorder());scroll.setOpaque(false);scroll.getViewport().setOpaque(false);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);scroll.getVerticalScrollBar().setUnitIncrement(16);
        backdrop.add(scroll);root.add(backdrop);setContentPane(root);getRootPane().setDefaultButton(btnLogin);
        txtUser.setName("username");txtPass.setName("password");cmbRole.setName("role");btnLogin.setName("signIn");
    }
    private Rectangle availableScreenBounds() {
        GraphicsConfiguration monitor=getGraphicsConfiguration();
        if(monitor==null)monitor=GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle bounds=new Rectangle(monitor.getBounds());Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(monitor);
        bounds.x+=insets.left;bounds.y+=insets.top;
        bounds.width=Math.max(1,bounds.width-insets.left-insets.right);bounds.height=Math.max(1,bounds.height-insets.top-insets.bottom);
        return bounds;
    }
    private void field(JPanel panel,String label,JComponent field) {
        panel.add(Theme.label(label,15,Theme.INK,true));panel.add(Box.createVerticalStrut(6));
        field.setFont(Theme.font(18,false));int height=Theme.controlHeight(field,44);
        field.setPreferredSize(new Dimension(520,height));field.setMinimumSize(new Dimension(80,height));field.setMaximumSize(new Dimension(Integer.MAX_VALUE,height));
        panel.add(field);panel.add(Box.createVerticalStrut(16));
    }
    private void doLogin() {
        if(System.currentTimeMillis()<retryAfter){message.setText("Please wait a moment before trying again.");return;}
        char[] password=txtPass.getPassword();
        if(txtUser.getText().trim().isEmpty()||password.length==0){message.setText("Enter your username and password.");Arrays.fill(password,'\0');return;}
        String username=txtUser.getText().trim();Role role=(Role)cmbRole.getSelectedItem();btnLogin.setEnabled(false);message.setText("Signing in…");
        new SwingWorker<User,Void>() {
            @Override protected User doInBackground() throws Exception {try{return accounts.authenticate(username,password,role);}finally{Arrays.fill(password,'\0');}}
            @Override protected void done(){
                btnLogin.setEnabled(true);txtPass.setText("");
                try {
                    User user=get();
                    if(user==null){failures++;if(failures>=3){retryAfter=System.currentTimeMillis()+30000;failures=0;}message.setText("Check your username, password, and selected role.");return;}
                    Session.login(user);MainFrame main=new MainFrame();main.setVisible(true);dispose();
                }catch(Exception ex){message.setText("Sign in failed. Check your database connection.");GuiUtil.error(Login.this,ex.getCause()==null?ex.getMessage():ex.getCause().getMessage());}
            }
        }.execute();
    }
    private void requestReset() {
        String username=txtUser.getText().trim();
        if(username.isEmpty()){message.setText("Enter your username and select your role first.");return;}
        Role role=(Role)cmbRole.getSelectedItem();
        new SwingWorker<Void,Void>() {
            @Override protected Void doInBackground() throws Exception {accounts.requestReset(username,role);return null;}
            @Override protected void done(){try{get();GuiUtil.info(Login.this,"If the account matches, a request is now in the administrator’s dashboard.\nContact your clinic administrator to receive your new password.");}catch(Exception ex){GuiUtil.error(Login.this,"Could not send your request. Check the database connection.");}}
        }.execute();
    }
}
