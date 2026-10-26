package gui;

import security.*;
import service.AccountService;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import javax.swing.*;

public class Login extends JFrame {
    private final JTextField txtUser=new JTextField();
    private final JPasswordField txtPass=new JPasswordField();
    private final JComboBox<Role> cmbRole=new JComboBox<>(Role.values());
    private final JButton btnLogin=Theme.primary("Sign in to your workspace   →");
    private final JLabel message=Theme.label(" ",12,new Color(0xB34747),false);
    private final JButton restoreWindow=Theme.quiet("Restore window (Esc)");
    private final AccountService accounts=new AccountService();
    private Timer boundsAnimation;
    private Rectangle windowedBounds;
    private boolean autoExpanded;
    private int failures;
    private long retryAfter;

    public Login() {
        super("ClinicFlow · Staff sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Rectangle available=availableScreenBounds();
        setMinimumSize(new Dimension(Math.min(930,available.width),Math.min(620,available.height)));
        setSize(Math.min(1040,available.width),Math.min(650,available.height));setLocationRelativeTo(null);
        JPanel root=new JPanel(new GridLayout(1,2)); root.setBackground(Color.WHITE);
        JPanel brand=new JPanel(new BorderLayout(0,18)); brand.setBackground(new Color(0xEDF7F4)); brand.setBorder(BorderFactory.createEmptyBorder(38,38,30,38));
        JPanel logo=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); logo.setOpaque(false);
        JLabel cross=Theme.label("✚",30,Theme.TEAL,true);logo.add(cross);logo.add(Theme.label("ClinicFlow",23,Theme.INK,true));brand.add(logo,BorderLayout.NORTH);
        JPanel story=Theme.column();
        story.add(Box.createVerticalGlue());
        JLabel kicker=Theme.label("A LITTLE MORE CARE. EVERY DAY.",10,Theme.TEAL,true);kicker.setAlignmentX(0);story.add(kicker);story.add(Box.createVerticalStrut(16));
        JLabel title=Theme.label("<html>Better care starts<br>with a better flow.</html>",33,Theme.INK,true);title.setAlignmentX(0);story.add(title);story.add(Box.createVerticalStrut(14));
        JLabel copy=Theme.label("<html>One connected space for your patients,<br>your team, and every step of their care.</html>",14,Theme.MUTED,false);copy.setAlignmentX(0);story.add(copy);
        ClinicArt art=new ClinicArt();art.setAlignmentX(0);story.add(art);story.add(Box.createVerticalGlue());brand.add(story,BorderLayout.CENTER);
        brand.add(Theme.label("PATIENTS   /   APPOINTMENTS   /   CARE",10,Theme.TEAL,true),BorderLayout.SOUTH);
        root.add(brand);
        JPanel right=new JPanel(new GridBagLayout());right.setBackground(Color.WHITE);right.setBorder(BorderFactory.createEmptyBorder(24,42,24,42));
        JPanel form=Theme.column();form.setMaximumSize(new Dimension(350,600));
        form.add(Theme.label("STAFF PORTAL",10,Theme.TEAL,true));form.add(Box.createVerticalStrut(13));
        form.add(Theme.label("Welcome back",29,Theme.INK,true));form.add(Box.createVerticalStrut(8));
        form.add(Theme.label("Sign in to continue caring for your community.",12,Theme.MUTED,false));form.add(Box.createVerticalStrut(26));
        field(form,"Your role",cmbRole);field(form,"Username",txtUser);field(form,"Password",txtPass);
        txtUser.putClientProperty("JTextField.placeholderText","Enter your username");txtPass.putClientProperty("JTextField.placeholderText","Enter your password");
        JCheckBox show=new JCheckBox("Show password");show.setOpaque(false);show.setForeground(Theme.MUTED);show.addActionListener(e->txtPass.setEchoChar(show.isSelected()?(char)0:'•'));form.add(show);
        JButton forgot=new JButton("Forgot password? Contact your administrator");forgot.setBorderPainted(false);forgot.setContentAreaFilled(false);forgot.setForeground(Theme.TEAL);forgot.setFont(forgot.getFont().deriveFont(11f));forgot.setMargin(new Insets(6,0,6,0));forgot.addActionListener(e->requestReset());form.add(forgot);
        message.setPreferredSize(new Dimension(350,36));message.setMaximumSize(new Dimension(400,36));form.add(message);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));btnLogin.setAlignmentX(0);btnLogin.addActionListener(e->doLogin());form.add(btnLogin);form.add(Box.createVerticalStrut(21));
        form.add(Theme.label("Staff accounts are managed by your clinic administrator.",10,Theme.MUTED,false));
        JPanel windowControls=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));windowControls.setOpaque(false);
        windowControls.setPreferredSize(new Dimension(350,30));windowControls.setMaximumSize(new Dimension(Integer.MAX_VALUE,30));
        restoreWindow.setName("restoreLoginWindow");restoreWindow.setVisible(false);
        restoreWindow.setFont(restoreWindow.getFont().deriveFont(11f));restoreWindow.setMargin(new Insets(5,0,0,0));
        restoreWindow.setBorderPainted(false);restoreWindow.setContentAreaFilled(false);restoreWindow.addActionListener(e->restoreWindow());
        windowControls.add(restoreWindow);form.add(windowControls);
        for(Component c:form.getComponents()) if(c instanceof JComponent)((JComponent)c).setAlignmentX(0);
        right.add(form);root.add(right);setContentPane(root);getRootPane().setDefaultButton(btnLogin);
        txtUser.setName("username");txtPass.setName("password");cmbRole.setName("role");btnLogin.setName("signIn");
        GuiUtil.onChange(txtUser,this::expandOnInput);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"restoreLoginWindow");
        getRootPane().getActionMap().put("restoreLoginWindow",new AbstractAction(){
            @Override public void actionPerformed(ActionEvent e){restoreWindow();}
        });
    }
    /** Keep the existing fields and keyboard focus while the window grows around them. */
    private void expandOnInput() {
        if(autoExpanded || !isShowing() || txtUser.getDocument().getLength()==0)return;
        autoExpanded=true;
        if((getExtendedState() & MAXIMIZED_BOTH)!=0)return;
        Rectangle target=availableScreenBounds();
        if(getBounds().equals(target))return;
        windowedBounds=getBounds();restoreWindow.setVisible(true);
        // The user may have moved the login onto a smaller secondary monitor.
        Dimension minimum=getMinimumSize();
        setMinimumSize(new Dimension(Math.min(minimum.width,target.width),Math.min(minimum.height,target.height)));
        animateBounds(target);
    }
    private Rectangle availableScreenBounds() {
        GraphicsConfiguration monitor=getGraphicsConfiguration();
        if(monitor==null)monitor=GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle bounds=new Rectangle(monitor.getBounds());Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(monitor);
        bounds.x+=insets.left;bounds.y+=insets.top;
        bounds.width=Math.max(1,bounds.width-insets.left-insets.right);bounds.height=Math.max(1,bounds.height-insets.top-insets.bottom);
        return bounds;
    }
    private void restoreWindow() {
        if(windowedBounds==null)return;
        // The one-shot flag stays set: Escape must not cause expansion on the next keystroke.
        Rectangle target=windowedBounds;windowedBounds=null;restoreWindow.setVisible(false);
        if((getExtendedState() & MAXIMIZED_BOTH)!=0)setExtendedState(NORMAL);
        animateBounds(target);
    }
    private void animateBounds(Rectangle target) {
        if(boundsAnimation!=null)boundsAnimation.stop();
        Rectangle start=getBounds();long started=System.nanoTime();
        boundsAnimation=new Timer(16,e->{
            if(e.getSource()!=boundsAnimation)return;
            if(!isShowing()){boundsAnimation.stop();boundsAnimation=null;return;}
            double progress=Math.min(1,(System.nanoTime()-started)/460_000_000.0);
            double eased=progress*progress*(3-2*progress);
            setBounds(interpolate(start.x,target.x,eased),interpolate(start.y,target.y,eased),
                    interpolate(start.width,target.width,eased),interpolate(start.height,target.height,eased));
            if(progress>=1){boundsAnimation.stop();boundsAnimation=null;}
        });
        boundsAnimation.setCoalesce(true);boundsAnimation.start();
    }
    private int interpolate(int from,int to,double progress){return (int)Math.round(from+(to-from)*progress);}
    @Override public void dispose(){
        if(boundsAnimation!=null){boundsAnimation.stop();boundsAnimation=null;}
        super.dispose();
    }
    private void field(JPanel p,String label,JComponent field) {
        p.add(Theme.label(label,12,Theme.INK,true));p.add(Box.createVerticalStrut(7));
        field.setPreferredSize(new Dimension(350,42));field.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));p.add(field);p.add(Box.createVerticalStrut(16));
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
