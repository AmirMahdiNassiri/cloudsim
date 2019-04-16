import javax.swing.*;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        mainForm form = new mainForm();
        form.setVisible(true);
    }
}
