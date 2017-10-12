package validate.applications;

import boofcv.gui.BoofSwingUtil;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * @author Peter Abeles
 */
public class InfoHandSelectPanel extends JPanel implements ChangeListener, MouseWheelListener,
		ActionListener
{
	private static double MAX = 20;
	private static double MIN = 0.1;
	private static double INC = 0.1;

	HandSelectBase owner;

	protected JSpinner zoomSpinner;
	protected JButton resetZoomButton;
	protected JButton saveButton;
	protected JButton nextButton;
	protected JButton openButton;
	protected JButton reloadButton;
	protected JButton detectShapes;
	protected JTextField fieldPrefix;


	protected JButton clearButton;

	protected JTextArea labelWidth = new JTextArea();
	protected JTextArea labelHeight = new JTextArea();

	protected JCheckBox cSkipLabeled = new JCheckBox("Skip Labeled");

	Runnable handleSelectShape;

	boolean skipLabeled = false;
	String prefix="";

	public InfoHandSelectPanel(HandSelectBase owner) {
		this.owner = owner;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

		SpinnerModel model = new SpinnerNumberModel(1.0, MIN,MAX,0.1);
		zoomSpinner = new JSpinner(model);
		zoomSpinner.addChangeListener(this);
		zoomSpinner.setFocusable(false);
		zoomSpinner.setMaximumSize(new Dimension(250,80));

		resetZoomButton = new JButton("Home");
		resetZoomButton.addActionListener(this);
		saveButton = new JButton("Save");
		saveButton.addActionListener(this);
		nextButton = new JButton("Next Image");
		nextButton.addActionListener(actionEvent -> {this.prefix = fieldPrefix.getText();owner.openNextImage();});
		openButton = new JButton("Open Image");
		openButton.addActionListener(actionEvent -> owner.openImageDialog());
		reloadButton = new JButton("Reload");
		reloadButton.addActionListener(e -> {
			this.prefix = fieldPrefix.getText();
			owner.reloadImage();
		});


		fieldPrefix = new JTextField(prefix);
		fieldPrefix.addActionListener(e-> this.prefix = fieldPrefix.getText());
		fieldPrefix.setMaximumSize(new Dimension(250,40));

		clearButton = new JButton("Clear");
		clearButton.addActionListener(this);
		cSkipLabeled.setSelected(skipLabeled);
		cSkipLabeled.addChangeListener(e->skipLabeled = cSkipLabeled.isSelected());

		detectShapes = new JButton("Detect");
		detectShapes.addActionListener(e->{if(handleSelectShape != null)handleSelectShape.run();});

		labelWidth.setEditable(false);
		labelWidth.setMaximumSize(new Dimension(250,40));
		labelHeight.setEditable(false);
		labelHeight.setMaximumSize(new Dimension(250,40));

		add( new JLabel("Width"));
		add( labelWidth);
		add(Box.createRigidArea(new Dimension(10,10)));
		add( new JLabel("Height"));
		add( labelHeight);
		add(Box.createRigidArea(new Dimension(10,10)));
		add( new JLabel("Scale"));
		add(zoomSpinner);
		add(Box.createRigidArea(new Dimension(10,50)));
		add(cSkipLabeled);
		add(resetZoomButton);
		add(saveButton);
		add(openButton);
		add(nextButton);
		add(new JLabel("Prefix Labeled"));
		add(fieldPrefix);
		add(reloadButton);
		add(Box.createVerticalGlue());
		add(detectShapes);
		add(clearButton);
	}

	public void setHandleSelectShape(Runnable handleSelectShape) {
		this.handleSelectShape = handleSelectShape;
	}

	public void setImageShape(int width , int height ) {
		BoofSwingUtil.invokeNowOrLater(()->{
			labelWidth.setText(""+width);
			labelHeight.setText(""+height);
		});
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		double value = ((Number)zoomSpinner.getValue()).doubleValue();
		owner.setScale(value);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {

		double curr = ((Number)zoomSpinner.getValue()).doubleValue();

		if( e.getWheelRotation() > 0 )
			curr *= 1.1;
		else
			curr /= 1.1;

		setScale(curr);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == resetZoomButton ) {
			zoomSpinner.setValue(1.0);
			owner.setScale(1.0);
		} else if( e.getSource() == saveButton ) {
			owner.save();
		} else if( e.getSource() == clearButton ) {
			owner.clearPoints();
		}
	}

	public void setScale(double scale) {
		double curr;

		if( scale >= 1 ) {
			curr = INC * ((int) (scale / INC + 0.5));
		} else {
			curr = scale;
		}
		if( curr < MIN ) curr = MIN;
		if( curr > MAX ) curr = MAX;

		zoomSpinner.setValue(curr);
	}
}
